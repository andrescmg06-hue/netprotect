package com.netprotect.app.core.rules

import java.time.LocalDateTime

/** Decides whether a foreground app should be blocked right now. Pure function, no I/O: the
 * caller is responsible for fetching rules, the device policy and today's usage first
 * (RuleEnforcementService).
 */
object RuleEvaluator {

    /** Returns why [packageName] is blocked, or null if it isn't.
     *
     * [todayUsageSeconds] is the same per-package total AppInventoryCollector already computes
     * for the Sprint 7 sync — DAILY_LIMIT is compared against it directly, not a new counter.
     *
     * [defaultPolicy] decides what happens to an app with no rule at all: under BLOCK
     * (allowlist mode) it is blocked, under ALLOW it runs. A DAILY_LIMIT or SCHEDULE rule counts
     * as approval while its condition isn't met — a tutor who set "one hour of this app a day"
     * has approved that app for that hour, even in allowlist mode.
     *
     * Protected packages (launcher, phone, settings — see ProtectedPackages) are handled by the
     * caller, and only against DEFAULT_POLICY: an explicit rule still applies to them. The
     * protected set includes user-settable defaults, so exempting them from every rule would
     * make "set this as my default phone app" a way to bypass any block.
     */
    fun evaluate(
        rules: List<AppRule>,
        packageName: String,
        todayUsageSeconds: Map<String, Int>,
        now: LocalDateTime,
        defaultPolicy: DefaultAppPolicy,
    ): BlockReason? {
        val rule = rules.find { it.packageName == packageName }
            ?: return defaultPolicyOutcome(defaultPolicy)

        return when (rule.ruleType) {
            RuleType.ALLOW -> null
            RuleType.BLOCK -> BlockReason.BLOCK
            RuleType.DAILY_LIMIT -> {
                val limitMinutes = rule.dailyLimitMinutes ?: return defaultPolicyOutcome(defaultPolicy)
                val usedSeconds = todayUsageSeconds[packageName] ?: 0
                if (usedSeconds >= limitMinutes * 60) BlockReason.DAILY_LIMIT else null
            }
            RuleType.SCHEDULE ->
                if (isWithinSchedule(rule, now)) BlockReason.SCHEDULE else null
        }
    }

    private fun defaultPolicyOutcome(defaultPolicy: DefaultAppPolicy): BlockReason? =
        if (defaultPolicy == DefaultAppPolicy.BLOCK) BlockReason.DEFAULT_POLICY else null

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
