package com.netprotect.app.core.rules

import com.netprotect.app.core.rules.LimitRecheck.UsageEventKind.OTHER_RESUMED
import com.netprotect.app.core.rules.LimitRecheck.UsageEventKind.PAUSED
import com.netprotect.app.core.rules.LimitRecheck.UsageEventKind.RESUMED
import com.netprotect.app.core.rules.LimitRecheck.UsageEventSample
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val APP = "com.instagram.android"

private fun appRule(type: RuleType, daily: Int? = null, weekly: Int? = null) =
    AppRule(APP, type, daily, weekly, null, null, null)

private fun categoryRule(type: RuleType, daily: Int? = null, weekly: Int? = null) =
    CategoryRule(Category.SOCIAL_MEDIA, type, daily, weekly, null, null, null)

private val SOCIAL = listOf(CategoryAssignment(APP, Category.SOCIAL_MEDIA))

private fun remaining(
    rules: List<AppRule> = emptyList(),
    categoryRules: List<CategoryRule> = emptyList(),
    today: Int = 0,
    week: Int = 0,
) = LimitRecheck.limitRemainingSeconds(
    rules, SOCIAL, categoryRules, APP, mapOf(APP to today), mapOf(APP to week),
)

private fun recheck(
    lastHandled: String? = APP,
    changed: String? = null,
    blockedThisVisit: Boolean = false,
    governedByLimit: Boolean = true,
    nextCheckAt: Long? = 1_000L,
    now: Long = 1_000L,
) = LimitRecheck.shouldRecheck(lastHandled, changed, blockedThisVisit, governedByLimit, nextCheckAt, now)

class LimitRecheckTest {

    // ------------------------------------------------------------ when to re-evaluate the same app

    @Test
    fun `same app still in front, limit rule, check time reached - re-evaluates`() {
        assertTrue(recheck())
    }

    @Test
    fun `a repeated foreground event of the same app still counts as staying in it`() {
        assertTrue(recheck(changed = APP))
    }

    @Test
    fun `before the check time - waits`() {
        assertFalse(recheck(nextCheckAt = 1_001L, now = 1_000L))
    }

    @Test
    fun `a limit that appeared after the visit started has no check time yet - re-evaluates now`() {
        assertTrue(recheck(nextCheckAt = null))
    }

    @Test
    fun `an app without a daily or weekly limit is never re-evaluated mid-visit`() {
        assertFalse(recheck(governedByLimit = false))
    }

    @Test
    fun `already blocked during this visit - never re-evaluated, so the event is reported once`() {
        assertFalse(recheck(blockedThisVisit = true))
    }

    @Test
    fun `nothing handled yet, e g right after the block screen - no re-evaluation`() {
        assertFalse(recheck(lastHandled = null))
    }

    @Test
    fun `another app came to the front - that is a new visit, not a re-evaluation`() {
        assertFalse(recheck(changed = "com.other"))
    }

    // ------------------------------------------------------------ is the app governed by a limit

    @Test
    fun `own daily limit governs the app`() {
        assertTrue(LimitRecheck.isGovernedByLimit(listOf(appRule(RuleType.DAILY_LIMIT, daily = 30)), SOCIAL, emptyList(), APP))
    }

    @Test
    fun `category weekly limit governs an app with no rule of its own`() {
        assertTrue(
            LimitRecheck.isGovernedByLimit(emptyList(), SOCIAL, listOf(categoryRule(RuleType.WEEKLY_LIMIT, weekly = 60)), APP)
        )
    }

    @Test
    fun `an app rule wins over its category limit, as in RuleEvaluator`() {
        assertFalse(
            LimitRecheck.isGovernedByLimit(
                listOf(appRule(RuleType.ALLOW)), SOCIAL, listOf(categoryRule(RuleType.DAILY_LIMIT, daily = 30)), APP,
            )
        )
    }

    @Test
    fun `schedule or block rules are not limits`() {
        assertFalse(LimitRecheck.isGovernedByLimit(listOf(appRule(RuleType.SCHEDULE)), SOCIAL, emptyList(), APP))
        assertFalse(LimitRecheck.isGovernedByLimit(emptyList(), SOCIAL, emptyList(), APP))
    }

    // ------------------------------------------------------------ remaining time and next check

    @Test
    fun `daily limit remaining is limit minus today's use`() {
        assertEquals(100, remaining(rules = listOf(appRule(RuleType.DAILY_LIMIT, daily = 30)), today = 1_700))
    }

    @Test
    fun `weekly limit remaining uses the week's use`() {
        assertEquals(
            600,
            remaining(rules = listOf(appRule(RuleType.WEEKLY_LIMIT, weekly = 60)), today = 3_500, week = 3_000),
        )
    }

    @Test
    fun `category daily limit remaining`() {
        assertEquals(60, remaining(categoryRules = listOf(categoryRule(RuleType.DAILY_LIMIT, daily = 1)), today = 0))
    }

    @Test
    fun `exhausted limit never goes negative`() {
        assertEquals(0, remaining(rules = listOf(appRule(RuleType.DAILY_LIMIT, daily = 1)), today = 999))
    }

    @Test
    fun `no limit means no remaining time`() {
        assertNull(remaining(rules = listOf(appRule(RuleType.ALLOW))))
    }

    @Test
    fun `next check is at the moment the time runs out when that is soon`() {
        assertEquals(1_000L + 10_000L, LimitRecheck.nextCheckAt(1_000L, 10))
    }

    @Test
    fun `next check is capped so a stale usage reading is corrected quickly`() {
        assertEquals(1_000L + LimitRecheck.MAX_RECHECK_MS, LimitRecheck.nextCheckAt(1_000L, 3_600))
    }

    @Test
    fun `next check never comes sooner than the polling tick`() {
        assertEquals(1_000L + LimitRecheck.MIN_RECHECK_MS, LimitRecheck.nextCheckAt(1_000L, 0))
    }

    @Test
    fun `no limit means no scheduled check`() {
        assertNull(LimitRecheck.nextCheckAt(1_000L, null))
    }

    // ------------------------------------------------------------ open foreground session from events

    @Test
    fun `a closed session counts resume to pause`() {
        val events = listOf(UsageEventSample(100, RESUMED), UsageEventSample(400, PAUSED))
        assertEquals(300L, LimitRecheck.foregroundMillisFromEvents(events, 0, 1_000))
    }

    @Test
    fun `the session still open counts up to now`() {
        val events = listOf(UsageEventSample(100, RESUMED))
        assertEquals(900L, LimitRecheck.foregroundMillisFromEvents(events, 0, 1_000))
    }

    @Test
    fun `a session that began before the window counts from the window start`() {
        val events = listOf(UsageEventSample(50, PAUSED), UsageEventSample(200, RESUMED))
        assertEquals(50L + 800L, LimitRecheck.foregroundMillisFromEvents(events, 0, 1_000))
    }

    @Test
    fun `switching activities inside the app does not double count`() {
        val events = listOf(
            UsageEventSample(100, RESUMED),
            UsageEventSample(200, RESUMED),
            UsageEventSample(300, PAUSED),
        )
        assertEquals(200L, LimitRecheck.foregroundMillisFromEvents(events, 0, 1_000))
    }

    @Test
    fun `another app coming to the front closes a session whose pause was never logged`() {
        val events = listOf(UsageEventSample(100, RESUMED), UsageEventSample(300, OTHER_RESUMED))
        assertEquals(200L, LimitRecheck.foregroundMillisFromEvents(events, 0, 1_000))
    }

    @Test
    fun `no events means no time`() {
        assertEquals(0L, LimitRecheck.foregroundMillisFromEvents(emptyList(), 0, 1_000))
    }

    // ------------------------------------------------------------ adding the open session without duplicating

    @Test
    fun `usage stats missing the open session - the difference is added`() {
        assertEquals(300, LimitRecheck.missingSeconds(statsTodaySeconds = 600, eventsTodaySeconds = 900))
    }

    @Test
    fun `usage stats already counting the open session - nothing is added`() {
        assertEquals(0, LimitRecheck.missingSeconds(statsTodaySeconds = 900, eventsTodaySeconds = 900))
    }

    @Test
    fun `events undercounting, e g a missed resume - stats win, nothing is added`() {
        assertEquals(0, LimitRecheck.missingSeconds(statsTodaySeconds = 1_000, eventsTodaySeconds = 900))
    }

    @Test
    fun `events unavailable - nothing is added`() {
        assertEquals(0, LimitRecheck.missingSeconds(statsTodaySeconds = 600, eventsTodaySeconds = null))
    }
}
