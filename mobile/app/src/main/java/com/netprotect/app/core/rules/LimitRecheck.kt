package com.netprotect.app.core.rules

/** Pure decisions for re-evaluating a daily/weekly limit while the supervised user stays inside
 * the same app (B-15). The poll loop in RuleEnforcementService only evaluated an app when a NEW
 * foreground event arrived, so a limit crossed mid-visit never blocked until the next app switch.
 *
 * No I/O here: the service feeds in the cached rules, the usage maps and the raw usage events.
 */
object LimitRecheck {
    /** Never re-check faster than the poll loop's own tick (RuleEnforcementService.POLL_INTERVAL_MS). */
    const val MIN_RECHECK_MS = 3_000L

    // Why a cap instead of trusting the computed instant alone: the remaining time is only as good
    // as the usage reading it came from, and a tutor can lower a limit at any moment (rules refresh
    // every 60 s). 30 s bounds how late a block can be in either case, for the cost of one
    // queryUsageStats + queryEvents every 30 s and only while an app with a limit is in front.
    const val MAX_RECHECK_MS = 30_000L

    enum class UsageEventKind {
        /** This package's activity resumed (MOVE_TO_FOREGROUND / ACTIVITY_RESUMED). */
        RESUMED,
        /** This package's activity paused (MOVE_TO_BACKGROUND / ACTIVITY_PAUSED). */
        PAUSED,
        /** Another package's activity resumed: this package can no longer be the one in front. */
        OTHER_RESUMED,
    }

    data class UsageEventSample(val timeMillis: Long, val kind: UsageEventKind)

    /** Whether the app that is still in the foreground must be evaluated again now.
     *
     * Only when the same visit continues ([changedPackage] null or the same app again), the visit
     * has not been blocked already (a block reports its event once; the overlay then covers the
     * app until a new visit), the app is governed by a daily/weekly limit, and its check time has
     * come. A null [nextCheckAtMillis] means the visit started without a limit (the tutor added
     * one since): check right away.
     */
    fun shouldRecheck(
        lastHandledPackage: String?,
        changedPackage: String?,
        blockedThisVisit: Boolean,
        governedByLimit: Boolean,
        nextCheckAtMillis: Long?,
        nowMillis: Long,
    ): Boolean {
        if (lastHandledPackage == null || blockedThisVisit || !governedByLimit) return false
        if (changedPackage != null && changedPackage != lastHandledPackage) return false
        return nextCheckAtMillis == null || nowMillis >= nextCheckAtMillis
    }

    /** True when the rule that RuleEvaluator would apply to [packageName] is a daily or weekly
     * limit — same precedence: the app's own rule wins over its category's.
     */
    fun isGovernedByLimit(
        rules: List<AppRule>,
        categoryAssignments: List<CategoryAssignment>,
        categoryRules: List<CategoryRule>,
        packageName: String,
    ): Boolean = effectiveLimit(rules, categoryAssignments, categoryRules, packageName) != null

    /** Seconds left before the limit governing [packageName] is reached (never negative), or null
     * when no daily/weekly limit governs it.
     */
    fun limitRemainingSeconds(
        rules: List<AppRule>,
        categoryAssignments: List<CategoryAssignment>,
        categoryRules: List<CategoryRule>,
        packageName: String,
        todayUsageSeconds: Map<String, Int>,
        weekUsageSeconds: Map<String, Int>,
    ): Int? {
        val limit = effectiveLimit(rules, categoryAssignments, categoryRules, packageName) ?: return null
        val used = when (limit.type) {
            RuleType.WEEKLY_LIMIT -> weekUsageSeconds[packageName] ?: 0
            else -> todayUsageSeconds[packageName] ?: 0
        }
        return (limit.minutes * 60 - used).coerceAtLeast(0)
    }

    /** When to look again: the instant the time runs out, clamped to [MIN_RECHECK_MS, MAX_RECHECK_MS]. */
    fun nextCheckAt(nowMillis: Long, remainingSeconds: Int?): Long? {
        if (remainingSeconds == null) return null
        val wait = (remainingSeconds.toLong() * 1_000L).coerceIn(MIN_RECHECK_MS, MAX_RECHECK_MS)
        return nowMillis + wait
    }

    /** Foreground time of one package within [windowStartMillis, nowMillis], from its usage
     * events in chronological order, with a still-open session counted up to [nowMillis].
     *
     * Deliberately biased to undercount rather than overcount (an overcount would block a
     * child's app early): repeated resumes keep the first start, a pause without an open session
     * only counts when it is the first event (the session began before the window), and another
     * app resuming closes the session even if its pause was never logged.
     */
    fun foregroundMillisFromEvents(
        events: List<UsageEventSample>,
        windowStartMillis: Long,
        nowMillis: Long,
    ): Long {
        var total = 0L
        var openSince: Long? = null
        var seenOwnEvent = false
        for (event in events) {
            when (event.kind) {
                UsageEventKind.RESUMED -> {
                    if (openSince == null) openSince = event.timeMillis
                    seenOwnEvent = true
                }
                UsageEventKind.PAUSED -> {
                    val start = openSince ?: if (!seenOwnEvent) windowStartMillis else null
                    if (start != null) total += (event.timeMillis - start).coerceAtLeast(0)
                    openSince = null
                    seenOwnEvent = true
                }
                UsageEventKind.OTHER_RESUMED -> {
                    openSince?.let { total += (event.timeMillis - it).coerceAtLeast(0) }
                    openSince = null
                }
            }
        }
        openSince?.let { total += (nowMillis - it).coerceAtLeast(0) }
        return total
    }

    /** How many seconds of today's use queryUsageStats is missing for the app in front — in
     * practice the session still open, which Android may not fold into totalTimeInForeground until
     * the app pauses. Both numbers measure the same thing (today's foreground time of one app), so
     * the larger one is the better estimate and only the difference is added: nothing is counted
     * twice when the stats already include the open session, and events that undercount fall back
     * to the stats. The same difference applies to the week, which contains today.
     */
    fun missingSeconds(statsTodaySeconds: Int, eventsTodaySeconds: Int?): Int =
        if (eventsTodaySeconds == null) 0 else (eventsTodaySeconds - statsTodaySeconds).coerceAtLeast(0)

    private data class Limit(val type: RuleType, val minutes: Int)

    private fun effectiveLimit(
        rules: List<AppRule>,
        categoryAssignments: List<CategoryAssignment>,
        categoryRules: List<CategoryRule>,
        packageName: String,
    ): Limit? {
        val appRule = rules.find { it.packageName == packageName }
        if (appRule != null) {
            return limitOf(appRule.ruleType, appRule.dailyLimitMinutes, appRule.weeklyLimitMinutes)
        }
        val category = categoryAssignments.find { it.packageName == packageName }?.category ?: return null
        val categoryRule = categoryRules.find { it.category == category } ?: return null
        return limitOf(categoryRule.ruleType, categoryRule.dailyLimitMinutes, categoryRule.weeklyLimitMinutes)
    }

    private fun limitOf(type: RuleType, daily: Int?, weekly: Int?): Limit? = when (type) {
        RuleType.DAILY_LIMIT -> daily?.let { Limit(type, it) }
        RuleType.WEEKLY_LIMIT -> weekly?.let { Limit(type, it) }
        else -> null
    }
}
