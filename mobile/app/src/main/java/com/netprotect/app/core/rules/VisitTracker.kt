package com.netprotect.app.core.rules

/** One foreground transition read from the usage events (MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND,
 * the same constants as ACTIVITY_RESUMED / ACTIVITY_PAUSED), in the order the system logged it.
 */
data class ForegroundTransition(val packageName: String, val kind: Kind) {
    enum class Kind { RESUMED, PAUSED }
}

/** One transition with the instant the system logged it (UsageEvents.Event.timeStamp). Two reads
 * that both return the same logged event return equal values, which is how [EventWindow] spots it. */
data class TimedTransition(val timestampMs: Long, val transition: ForegroundTransition)

/** Sprint 73 (T5): consecutive usage-event reads with a small overlap, without handing the same
 * event over twice.
 *
 * Each poll used to read exactly [previous end, now). An event the system logged a moment late —
 * stamped inside the previous window but not yet visible when that window was read — fell between
 * two reads and was lost; a lost RESUMED can be a whole visit nobody evaluates. Now each read
 * starts [OVERLAP_MS] before the previous end, and what is handed over drops:
 * - events already handed over (same instant, package and kind) — so the same RESUMED never opens
 *   a second visit and the same block is never reported twice;
 * - events older than the newest one already handed over: applied now, out of order, a late
 *   "RESUMED a" would wrongly put `a` back in front of the app that followed it.
 *
 * Pure (no Android types): unit-tested in VisitTrackerTest.
 */
class EventWindow(initialEnd: Long, private val overlapMs: Long = OVERLAP_MS) {
    private var end = initialEnd
    private var newestHandedOver = Long.MIN_VALUE
    private var recent: Set<TimedTransition> = emptySet()

    /** Where the next read starts: the previous end minus the overlap. */
    fun nextStart(): Long = end - overlapMs

    /** Takes one read of [from, queriedUntil), oldest first, and returns only what is new. */
    fun accept(events: List<TimedTransition>, queriedUntil: Long): List<ForegroundTransition> {
        val fresh = events.filter { it !in recent && it.timestampMs >= newestHandedOver }
        fresh.maxOfOrNull { it.timestampMs }?.let { newestHandedOver = maxOf(newestHandedOver, it) }
        // Only what a later read can return again, or what shares the newest instant handed over
        // (the only older-or-equal stamp still accepted), needs remembering.
        val horizon = minOf(queriedUntil - overlapMs, newestHandedOver)
        recent = (recent + events).filterTo(HashSet()) { it.timestampMs >= horizon }
        end = maxOf(end, queriedUntil)
        return fresh.map { it.transition }
    }

    companion object {
        const val OVERLAP_MS = 2_000L
    }
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

    /** Sprint 73 (T5): applies a look-back read taken when the poll loop (re)starts — after a
     * reboot, an app update or a restart by the system the loop used to know nothing about the
     * app already in front (no visit until the child left it and came back), so that app was
     * never evaluated. Returns true when the read leaves an app in front: the caller treats it as
     * a new visit and evaluates it. An app whose last transition was PAUSED (screen off, say) is
     * not in front, so nothing to evaluate; an empty read leaves nothing known.
     */
    fun seed(transitions: List<ForegroundTransition>): Boolean {
        consume(transitions)
        return currentPackage != null && inForeground
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
