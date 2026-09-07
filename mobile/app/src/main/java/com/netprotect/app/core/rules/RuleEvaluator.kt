package com.netprotect.app.core.rules

import java.time.LocalDateTime

/** Decides whether a foreground app should be blocked right now. Pure function, no I/O: the
 * caller is responsible for fetching rules, category assignments/rules, the device policy and
 * today's usage first (RuleEnforcementService).
 */
object RuleEvaluator {

    /** Returns why [packageName] is blocked, or null if it isn't.
     *
     * Priority, most to least specific (see docs/sprint-10.md): a per-app [AppRule] always wins
     * if one exists for [packageName]; otherwise, if the package was assigned a category
     * ([categoryAssignments]) and that category has a rule ([categoryRules]), that rule applies;
     * otherwise [defaultPolicy] decides.
     *
     * [todayUsageSeconds] is the same per-package total AppInventoryCollector already computes
     * for the Sprint 7 sync — DAILY_LIMIT is compared against it directly, not a new counter.
     *
     * An ALLOW rule, or a DAILY_LIMIT/SCHEDULE rule that isn't currently blocking, counts as
     * approval — at the app level (Sprint 9) and, identically, at the category level (Sprint 10):
     * a tutor who set "Games: one hour a day" has approved that category for that hour, even
     * under a BLOCK default policy (allowlist mode).
     *
     * Protected packages (launcher, phone, settings — see ProtectedPackages) are handled by the
     * caller, and only against DEFAULT_POLICY: an explicit app or category rule still applies to
     * them. The protected set includes user-settable defaults, so exempting them from every rule
     * would make "set this as my default phone app" a way to bypass any block.
     */
    fun evaluate(
        rules: List<AppRule>,
        categoryAssignments: List<CategoryAssignment>,
        categoryRules: List<CategoryRule>,
        packageName: String,
        todayUsageSeconds: Map<String, Int>,
        weekUsageSeconds: Map<String, Int>,
        now: LocalDateTime,
        defaultPolicy: DefaultAppPolicy,
        schoolMode: SchoolMode,
    ): BlockReason? {
        val usedSeconds = todayUsageSeconds[packageName] ?: 0
        val usedWeekSeconds = weekUsageSeconds[packageName] ?: 0

        val appRule = rules.find { it.packageName == packageName }
        if (appRule != null) {
            return evaluateRuleFields(
                appRule.ruleType,
                appRule.dailyLimitMinutes,
                appRule.weeklyLimitMinutes,
                appRule.scheduleStartMinute,
                appRule.scheduleEndMinute,
                appRule.scheduleDaysMask,
                usedSeconds,
                usedWeekSeconds,
                now,
            )
        }

        val category = categoryAssignments.find { it.packageName == packageName }?.category
        val categoryRule = category?.let { assigned -> categoryRules.find { it.category == assigned } }
        if (categoryRule != null) {
            val blocked = evaluateRuleFields(
                categoryRule.ruleType,
                categoryRule.dailyLimitMinutes,
                categoryRule.weeklyLimitMinutes,
                categoryRule.scheduleStartMinute,
                categoryRule.scheduleEndMinute,
                categoryRule.scheduleDaysMask,
                usedSeconds,
                usedWeekSeconds,
                now,
            )
            // The category rule only ever reports back BLOCK/DAILY_LIMIT/SCHEDULE or null — it's
            // re-tagged as CATEGORY here so the tutor's event history (and the block screen) can
            // tell "blocked by its category" apart from "blocked by a rule on this exact app".
            return if (blocked != null) BlockReason.CATEGORY else null
        }

        return defaultPolicyOutcome(defaultPolicy, schoolMode, now)
    }

    /** The part of evaluation shared by AppRule and CategoryRule — same four rule types, same
     * fields, just sourced from whichever of the two matched. Returning null means "not blocked
     * by this rule" for either caller: at the app level that's the final answer (Sprint 9); at
     * the category level the caller still re-tags a non-null result as CATEGORY.
     */
    private fun evaluateRuleFields(
        ruleType: RuleType,
        dailyLimitMinutes: Int?,
        weeklyLimitMinutes: Int?,
        scheduleStartMinute: Int?,
        scheduleEndMinute: Int?,
        scheduleDaysMask: Int?,
        usedSeconds: Int,
        usedWeekSeconds: Int,
        now: LocalDateTime,
    ): BlockReason? = when (ruleType) {
        RuleType.ALLOW -> null
        RuleType.BLOCK -> BlockReason.BLOCK
        RuleType.DAILY_LIMIT -> {
            // dailyLimitMinutes/weeklyLimitMinutes null here is unreachable in practice — the
            // backend's CHECK constraint requires them for their respective rule_type — but
            // null-safety still has to resolve to something: "not blocked" rather than crashing.
            val limitMinutes = dailyLimitMinutes
            if (limitMinutes != null && usedSeconds >= limitMinutes * 60) BlockReason.DAILY_LIMIT else null
        }
        RuleType.WEEKLY_LIMIT -> {
            val limitMinutes = weeklyLimitMinutes
            if (limitMinutes != null && usedWeekSeconds >= limitMinutes * 60) {
                BlockReason.WEEKLY_LIMIT
            } else {
                null
            }
        }
        RuleType.SCHEDULE ->
            if (isWithinSchedule(scheduleStartMinute, scheduleEndMinute, scheduleDaysMask, now)) {
                BlockReason.SCHEDULE
            } else {
                null
            }
    }

    /** BLOCK if either the device's own default policy is BLOCK, or school mode (Sprint 12) is
     * currently in its window — whichever fires reports its own distinct reason, since a tutor
     * reading the history needs to tell "this device is allowlist-only all day" apart from
     * "school mode just kicked in". School mode is checked first only because it's the more
     * specific, temporary condition; a device already in allowlist mode all the time would report
     * DEFAULT_POLICY outside school hours regardless of ordering.
     */
    private fun defaultPolicyOutcome(
        defaultPolicy: DefaultAppPolicy,
        schoolMode: SchoolMode,
        now: LocalDateTime,
    ): BlockReason? {
        if (schoolMode.enabled &&
            isWithinSchedule(schoolMode.startMinute, schoolMode.endMinute, schoolMode.daysMask, now)
        ) {
            return BlockReason.SCHOOL_MODE
        }
        return if (defaultPolicy == DefaultAppPolicy.BLOCK) BlockReason.DEFAULT_POLICY else null
    }

    private fun isWithinSchedule(
        startMinute: Int?,
        endMinute: Int?,
        daysMask: Int?,
        now: LocalDateTime,
    ): Boolean {
        val start = startMinute ?: return false
        val end = endMinute ?: return false
        val mask = daysMask ?: return false

        // java.time.DayOfWeek.value is MONDAY=1..SUNDAY=7, so value-1 already lines up with
        // the backend's bit 0 = Monday ... bit 6 = Sunday — no Sunday special-case needed.
        val dayBit = 1 shl (now.dayOfWeek.value - 1)
        if (mask and dayBit == 0) return false

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
