package com.netprotect.app.core.rules

/** One foreground transition read from the usage events (MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND,
 * the same constants as ACTIVITY_RESUMED / ACTIVITY_PAUSED), in the order the system logged it.
 */
data class ForegroundTransition(val packageName: String, val kind: Kind) {
    enum class Kind { RESUMED, PAUSED }
}

/** Sprint 73: decides what a "visit" to an app is from the WHOLE sequence of transitions of each
 * poll window, not only from the last package that came to the front.
 *
 * Before, the poll loop kept just the latest MOVE_TO_FOREGROUND of each ~3 s window. Leaving a
 * blocked app for the home screen and coming back within the same window (facebook -> home ->
 * facebook, seen on a real phone) looked like "facebook again", the same visit already blocked,
 * so nothing was evaluated or drawn and the app stayed usable. Here every RESUMED of a package
 * other than the current one opens a new visit, wherever it falls in the window.
 *
 * Rules:
 * - RESUMED of a package different from the current visit's: new visit of that package.
 * - PAUSED of the current visit's package: it is no longer in front (the visit itself goes on).
 * - RESUMED of the current package with no other package in between: same visit (moving between
 *   the app's own activities never re-evaluates it).
 * - PAUSED of any other package: ignored (it is the previous app finishing its exit).
 *
 * Pure: no Android types, so the decision is unit-tested on the JVM (VisitTrackerTest). Knows
 * nothing about this app's own package: the caller decides what an own-package visit means.
 */
class VisitTracker {
    /** Package of the current visit, or null before the first RESUMED was ever seen. */
    var currentPackage: String? = null
        private set

    /** Whether [currentPackage] is still in front (its last transition was RESUMED). */
    var inForeground: Boolean = false
        private set

    /** Applies one poll window's transitions, oldest first. Returns true when at least one new
     * visit started in this batch; [currentPackage] is then the package of the LAST one. */
    fun consume(transitions: List<ForegroundTransition>): Boolean {
        var newVisitStarted = false
        for (transition in transitions) {
            when (transition.kind) {
                ForegroundTransition.Kind.RESUMED -> {
                    if (transition.packageName != currentPackage) {
                        currentPackage = transition.packageName
                        newVisitStarted = true
                    }
                    inForeground = true
                }
                ForegroundTransition.Kind.PAUSED -> {
                    if (transition.packageName == currentPackage) inForeground = false
                }
            }
        }
        return newVisitStarted
    }
}

/** Sprint 73 (Fallo 3): what to do with the visit already in front on a rules beat.
 *
 * A visit used to be evaluated only when it began (plus B-15's limit re-checks), so a block rule
 * the tutor created while the child was already inside that app did nothing until the app was
 * closed and opened again; the same for a SCHEDULE (or school mode) whose time began mid-visit.
 *
 * Why on the rules beat, and not every 3 s poll: the beat already comes at once on a realtime
 * "rules_changed" (new rules apply within one poll) and otherwise every 60 s, whether the fetch
 * worked or not — so a schedule that starts while offline still blocks from the cached rules.
 * Schedules are minute-granular, so a re-evaluation per minute bounds that delay to about a
 * minute; doing it every poll would read the usage stats (two queryUsageStats calls) twenty times
 * more often for no gain. Pure: unit-tested in VisitTrackerTest.
 */
object VisitRecheck {
    enum class Action {
        /** Nothing to do this cycle. */
        NONE,

        /** Evaluate the visit in front with the latest rules; a block reports its rule event. */
        EVALUATE,

        /** The visit is blocked: check, without reporting, that the latest rules still block it. */
        CONFIRM_BLOCK,
    }

    /**
     * @param rulesBeat the rules were just (re)fetched or due this cycle.
     * @param currentIsOtherApp a visit exists and it is not this app's own package (never evaluated).
     * @param visitStarted the visit began this cycle: it is being evaluated anyway.
     * @param inForeground the visit's app is still in front (last transition RESUMED).
     * @param blockedThisVisit the visit was already blocked (its event already reported).
     */
    fun onRulesBeat(
        rulesBeat: Boolean,
        currentIsOtherApp: Boolean,
        visitStarted: Boolean,
        inForeground: Boolean,
        blockedThisVisit: Boolean,
    ): Action = when {
        !rulesBeat || !currentIsOtherApp || visitStarted -> Action.NONE
        // Even if it is no longer in front: a removed rule should not leave its screen behind.
        blockedThisVisit -> Action.CONFIRM_BLOCK
        inForeground -> Action.EVALUATE
        else -> Action.NONE
    }
}
