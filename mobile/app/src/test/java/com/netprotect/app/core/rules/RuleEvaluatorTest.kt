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

class RuleEvaluatorTest {

    @Test
    fun `no rule for the package means no block`() {
        val result = RuleEvaluator.evaluate(emptyList(), PKG, emptyMap(), mondayAt(12))
        assertNull(result)
    }

    @Test
    fun `a rule for a different package does not block this one`() {
        val result = RuleEvaluator.evaluate(
            listOf(blockRule().copy(packageName = "com.other")), PKG, emptyMap(), mondayAt(12)
        )
        assertNull(result)
    }

    @Test
    fun `ALLOW never blocks`() {
        val result = RuleEvaluator.evaluate(listOf(allowRule()), PKG, emptyMap(), mondayAt(12))
        assertNull(result)
    }

    @Test
    fun `BLOCK always blocks`() {
        val result = RuleEvaluator.evaluate(listOf(blockRule()), PKG, emptyMap(), mondayAt(3))
        assertEquals(RuleType.BLOCK, result)
    }

    @Test
    fun `DAILY_LIMIT does not block while under the limit`() {
        val result = RuleEvaluator.evaluate(
            listOf(dailyLimitRule(60)), PKG, mapOf(PKG to 59 * 60), mondayAt(12)
        )
        assertNull(result)
    }

    @Test
    fun `DAILY_LIMIT blocks once usage reaches the limit exactly`() {
        val result = RuleEvaluator.evaluate(
            listOf(dailyLimitRule(60)), PKG, mapOf(PKG to 60 * 60), mondayAt(12)
        )
        assertEquals(RuleType.DAILY_LIMIT, result)
    }

    @Test
    fun `DAILY_LIMIT with no usage reported yet does not block`() {
        val result = RuleEvaluator.evaluate(listOf(dailyLimitRule(60)), PKG, emptyMap(), mondayAt(12))
        assertNull(result)
    }

    @Test
    fun `SCHEDULE blocks inside a same-day window`() {
        val rule = scheduleRule(start = 9 * 60, end = 17 * 60)
        assertEquals(RuleType.SCHEDULE, RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(12)))
        assertNull(RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(8)))
        assertNull(RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(17)))
    }

    @Test
    fun `SCHEDULE handles an overnight window that wraps past midnight`() {
        val rule = scheduleRule(start = 22 * 60, end = 6 * 60)
        assertEquals(RuleType.SCHEDULE, RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(23)))
        assertEquals(RuleType.SCHEDULE, RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(3)))
        assertNull(RuleEvaluator.evaluate(listOf(rule), PKG, emptyMap(), mondayAt(12)))
    }

    @Test
    fun `SCHEDULE only blocks on days included in the mask`() {
        val mondayOnly = 0b000_0001
        val blockedRule = scheduleRule(start = 0, end = 23 * 60 + 59, daysMask = mondayOnly)
        assertEquals(
            RuleType.SCHEDULE,
            RuleEvaluator.evaluate(listOf(blockedRule), PKG, emptyMap(), mondayAt(12)),
        )
        // 2026-09-08 is the Tuesday right after the fixed Monday reference above.
        val tuesday = LocalDateTime.of(2026, 9, 8, 12, 0)
        assertNull(RuleEvaluator.evaluate(listOf(blockedRule), PKG, emptyMap(), tuesday))
    }
}
