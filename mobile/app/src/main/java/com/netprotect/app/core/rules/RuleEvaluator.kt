package com.netprotect.app.core.rules

import java.time.LocalDateTime

/** Decides whether a foreground app should be blocked right now. Pure function, no I/O: the
 * caller is responsible for fetching rules and today's usage first (RuleEnforcementService).
 */
object RuleEvaluator {

    /** Returns the rule type that caused the block, or null if [packageName] isn't blocked.
     * ALLOW and "no rule for this package" both return null — see AppRule.kt on why ALLOW has
     * no distinct effect yet.
     *
     * [todayUsageSeconds] is the same per-package total AppInventoryCollector already computes
     * for the Sprint 7 sync — DAILY_LIMIT is compared against it directly, not a new counter.
     */
    fun evaluate(
        rules: List<AppRule>,
        packageName: String,
        todayUsageSeconds: Map<String, Int>,
        now: LocalDateTime,
    ): RuleType? {
        val rule = rules.find { it.packageName == packageName } ?: return null
        return when (rule.ruleType) {
            RuleType.ALLOW -> null
            RuleType.BLOCK -> RuleType.BLOCK
            RuleType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return null
                val usedSeconds = todayUsageSeconds[packageName] ?: 0
                if (usedSeconds >= limitMinutes * 60) RuleType.DAILY_LIMIT else null
            }
            RuleType.SCHEDULE -> if (isWithinSchedule(rule, now)) RuleType.SCHEDULE else null
        }
    }

    private fun isWithinSchedule(rule: AppRule, now: LocalDateTime): Boolean {
        val start = rule.scheduleStartMinute ?: return false
        val end = rule.scheduleEndMinute ?: return false
        val daysMask = rule.scheduleDaysMask ?: return false

        // java.time.DayOfWeek.value is MONDAY=1..SUNDAY=7, so value-1 already lines up with
        // the backend's bit 0 = Monday ... bit 6 = Sunday — no Sunday special-case needed.
        val dayBit = 1 shl (now.dayOfWeek.value - 1)
        if (daysMask and dayBit == 0) return false

        val minuteOfDay = now.hour * 60 + now.minute
        return if (start <= end) {
            minuteOfDay in start until end
        } else {
            // Overnight window, e.g. 22:00-06:00: blocked from start-to-midnight or
            // midnight-to-end.
            minuteOfDay >= start || minuteOfDay < end
        }
    }
}
