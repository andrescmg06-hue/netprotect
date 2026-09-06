package com.netprotect.app.core.rules

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val PKG = "com.instagram.android"
private const val ALL_DAYS = 0b111_1111 // bit 0 = Monday ... bit 6 = Sunday

private fun blockRule() = AppRule(PKG, RuleType.BLOCK, null, null, null, null)
private fun allowRule() = AppRule(PKG, RuleType.ALLOW, null, null, null, null)
private fun dailyLimitRule(minutes: Int) = AppRule(PKG, RuleType.DAILY_LIMIT, minutes, null, null, null)
private fun scheduleRule(start: Int, end: Int, daysMask: Int = ALL_DAYS) =
    AppRule(PKG, RuleType.SCHEDULE, null, start, end, daysMask)

// 2026-09-07 is a Monday, used as the fixed reference day for every SCHEDULE test below.
private fun mondayAt(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 9, 7, hour, minute)

private fun evaluate(
    rules: List<AppRule>,
    now: LocalDateTime = mondayAt(12),
    usage: Map<String, Int> = emptyMap(),
    policy: DefaultAppPolicy = DefaultAppPolicy.ALLOW,
    packageName: String = PKG,
) = RuleEvaluator.evaluate(rules, packageName, usage, now, policy)

class RuleEvaluatorTest {

    // ------------------------------------------------------------------ blocklist mode (ALLOW)

    @Test
    fun `no rule for the package means no block`() {
        assertNull(evaluate(emptyList()))
    }

    @Test
    fun `a rule for a different package does not block this one`() {
        assertNull(evaluate(listOf(blockRule().copy(packageName = "com.other"))))
    }

    @Test
    fun `ALLOW never blocks`() {
        assertNull(evaluate(listOf(allowRule())))
    }

    @Test
    fun `BLOCK always blocks`() {
        assertEquals(BlockReason.BLOCK, evaluate(listOf(blockRule()), now = mondayAt(3)))
    }

    @Test
    fun `DAILY_LIMIT does not block while under the limit`() {
        assertNull(evaluate(listOf(dailyLimitRule(60)), usage = mapOf(PKG to 59 * 60)))
    }

    @Test
    fun `DAILY_LIMIT blocks once usage reaches the limit exactly`() {
        assertEquals(
            BlockReason.DAILY_LIMIT,
            evaluate(listOf(dailyLimitRule(60)), usage = mapOf(PKG to 60 * 60)),
        )
    }

    @Test
    fun `DAILY_LIMIT with no usage reported yet does not block`() {
        assertNull(evaluate(listOf(dailyLimitRule(60))))
    }

    @Test
    fun `SCHEDULE blocks inside a same-day window`() {
        val rule = scheduleRule(start = 9 * 60, end = 17 * 60)
        assertEquals(BlockReason.SCHEDULE, evaluate(listOf(rule), now = mondayAt(12)))
        assertNull(evaluate(listOf(rule), now = mondayAt(8)))
        assertNull(evaluate(listOf(rule), now = mondayAt(17)))
    }

    @Test
    fun `SCHEDULE handles an overnight window that wraps past midnight`() {
        val rule = scheduleRule(start = 22 * 60, end = 6 * 60)
        assertEquals(BlockReason.SCHEDULE, evaluate(listOf(rule), now = mondayAt(23)))
        assertEquals(BlockReason.SCHEDULE, evaluate(listOf(rule), now = mondayAt(3)))
        assertNull(evaluate(listOf(rule), now = mondayAt(12)))
    }

    @Test
    fun `SCHEDULE only blocks on days included in the mask`() {
        val mondayOnly = 0b000_0001
        val rule = scheduleRule(start = 0, end = 23 * 60 + 59, daysMask = mondayOnly)
        assertEquals(BlockReason.SCHEDULE, evaluate(listOf(rule), now = mondayAt(12)))
        // 2026-09-08 is the Tuesday right after the fixed Monday reference above.
        assertNull(evaluate(listOf(rule), now = LocalDateTime.of(2026, 9, 8, 12, 0)))
    }

    // ----------------------------------------------------------------- allowlist mode (BLOCK)

    @Test
    fun `an app with no rule is blocked by the default policy in allowlist mode`() {
        assertEquals(
            BlockReason.DEFAULT_POLICY,
            evaluate(emptyList(), policy = DefaultAppPolicy.BLOCK),
        )
    }

    @Test
    fun `an ALLOW rule approves an app in allowlist mode`() {
        assertNull(evaluate(listOf(allowRule()), policy = DefaultAppPolicy.BLOCK))
    }

    @Test
    fun `a BLOCK rule still blocks in allowlist mode, reported as BLOCK not DEFAULT_POLICY`() {
        assertEquals(
            BlockReason.BLOCK,
            evaluate(listOf(blockRule()), policy = DefaultAppPolicy.BLOCK),
        )
    }

    @Test
    fun `DAILY_LIMIT counts as approval in allowlist mode while under the limit`() {
        assertNull(
            evaluate(
                listOf(dailyLimitRule(60)),
                usage = mapOf(PKG to 30 * 60),
                policy = DefaultAppPolicy.BLOCK,
            )
        )
    }

    @Test
    fun `DAILY_LIMIT over the limit reports the limit, not the default policy`() {
        assertEquals(
            BlockReason.DAILY_LIMIT,
            evaluate(
                listOf(dailyLimitRule(60)),
                usage = mapOf(PKG to 90 * 60),
                policy = DefaultAppPolicy.BLOCK,
            ),
        )
    }

    @Test
    fun `SCHEDULE counts as approval in allowlist mode outside its blocked window`() {
        val rule = scheduleRule(start = 22 * 60, end = 6 * 60)
        assertNull(evaluate(listOf(rule), now = mondayAt(12), policy = DefaultAppPolicy.BLOCK))
    }

    @Test
    fun `a rule for another package does not approve this one in allowlist mode`() {
        val otherRule = allowRule().copy(packageName = "com.other")
        assertEquals(
            BlockReason.DEFAULT_POLICY,
            evaluate(listOf(otherRule), policy = DefaultAppPolicy.BLOCK),
        )
    }
}
