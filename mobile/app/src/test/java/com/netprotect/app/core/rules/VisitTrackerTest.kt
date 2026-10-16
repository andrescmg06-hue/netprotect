package com.netprotect.app.core.rules

import com.netprotect.app.core.rules.ForegroundTransition.Kind.PAUSED
import com.netprotect.app.core.rules.ForegroundTransition.Kind.RESUMED
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FACEBOOK = "com.facebook.katana"
private const val HOME = "com.miui.home"
private const val OWN = "com.netprotect.app"

private fun resumed(pkg: String) = ForegroundTransition(pkg, RESUMED)
private fun paused(pkg: String) = ForegroundTransition(pkg, PAUSED)

/** A tracker whose current visit is [pkg], already handled in an earlier batch. */
private fun trackerIn(pkg: String) = VisitTracker().also { it.consume(listOf(resumed(pkg))) }

class VisitTrackerTest {

    @Test
    fun firstResumedStartsAVisit() {
        val tracker = VisitTracker()
        assertTrue(tracker.consume(listOf(resumed(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun emptyBatchChangesNothing() {
        val fresh = VisitTracker()
        assertFalse(fresh.consume(emptyList()))
        assertNull(fresh.currentPackage)
        assertFalse(fresh.inForeground)

        val tracker = trackerIn(FACEBOOK)
        assertFalse(tracker.consume(emptyList()))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    /** Sprint 73, Fallo 1: the real Redmi sequence. Facebook was blocked; «Ir al inicio» and back
     * to Facebook inside one poll window must be a NEW visit of Facebook, not the old one. */
    @Test
    fun homeAndBackToTheSameAppInOneWindowIsANewVisit() {
        val tracker = trackerIn(FACEBOOK)
        val newVisit = tracker.consume(listOf(resumed(HOME), resumed(FACEBOOK)))
        assertTrue(newVisit)
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun realSequenceWithPausesIsANewVisitToo() {
        val tracker = trackerIn(FACEBOOK)
        val newVisit = tracker.consume(
            listOf(paused(FACEBOOK), resumed(HOME), paused(HOME), resumed(FACEBOOK)),
        )
        assertTrue(newVisit)
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun theWholeSprintSequenceInOneWindowEndsInAFacebookVisit() {
        // facebook, home, facebook — all in the first window the tracker ever sees.
        val tracker = VisitTracker()
        assertTrue(tracker.consume(listOf(resumed(FACEBOOK), resumed(HOME), resumed(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun movingBetweenTheAppsOwnActivitiesIsTheSameVisit() {
        val tracker = trackerIn(FACEBOOK)
        // One activity pauses, another of the same app resumes: nothing else in between.
        assertFalse(tracker.consume(listOf(paused(FACEBOOK), resumed(FACEBOOK))))
        assertFalse(tracker.consume(listOf(resumed(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun pauseOfTheCurrentAppLeavesTheVisitButNotInFront() {
        val tracker = trackerIn(FACEBOOK)
        assertFalse(tracker.consume(listOf(paused(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertFalse(tracker.inForeground)
        // Coming back with nothing in between is still the same visit.
        assertFalse(tracker.consume(listOf(resumed(FACEBOOK))))
        assertTrue(tracker.inForeground)
    }

    @Test
    fun pauseOfAnotherAppIsIgnored() {
        val tracker = trackerIn(FACEBOOK)
        // The previous app's pause logged after the new one resumed.
        assertFalse(tracker.consume(listOf(paused(HOME))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun switchingToAnotherAppIsANewVisitOfThatApp() {
        val tracker = trackerIn(FACEBOOK)
        assertTrue(tracker.consume(listOf(paused(FACEBOOK), resumed(HOME))))
        assertEquals(HOME, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun ownPackageIsAVisitLikeAnyOtherAndLeavingItStartsANewOne() {
        // The tracker does not special-case this app: the service resets on an own-package visit
        // and never evaluates it. Going back to the blocked app afterwards is a new visit.
        val tracker = trackerIn(FACEBOOK)
        assertTrue(tracker.consume(listOf(resumed(OWN))))
        assertEquals(OWN, tracker.currentPackage)
        assertTrue(tracker.consume(listOf(paused(OWN), resumed(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
    }

    @Test
    fun ownPackageAndBackInOneWindowIsANewVisitOfTheBlockedApp() {
        val tracker = trackerIn(FACEBOOK)
        assertTrue(tracker.consume(listOf(resumed(OWN), resumed(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }
}

class VisitRecheckTest {
    private fun action(
        rulesBeat: Boolean = true,
        otherApp: Boolean = true,
        visitStarted: Boolean = false,
        inForeground: Boolean = true,
        blocked: Boolean = false,
    ) = VisitRecheck.onRulesBeat(rulesBeat, otherApp, visitStarted, inForeground, blocked)

    /** Sprint 73, Fallo 3: a rule created while the child is already inside the app. */
    @Test
    fun newRulesReEvaluateTheAppAlreadyInFront() {
        assertEquals(VisitRecheck.Action.EVALUATE, action())
    }

    @Test
    fun withoutARulesBeatNothingIsReEvaluated() {
        assertEquals(VisitRecheck.Action.NONE, action(rulesBeat = false))
        assertEquals(VisitRecheck.Action.NONE, action(rulesBeat = false, blocked = true))
    }

    @Test
    fun aBlockedVisitIsOnlyConfirmedNeverReportedAgain() {
        assertEquals(VisitRecheck.Action.CONFIRM_BLOCK, action(blocked = true))
        // Also once it left the front: a removed rule must not leave its block screen behind.
        assertEquals(VisitRecheck.Action.CONFIRM_BLOCK, action(blocked = true, inForeground = false))
    }

    @Test
    fun ownPackageOrNoVisitIsNeverEvaluated() {
        assertEquals(VisitRecheck.Action.NONE, action(otherApp = false))
        assertEquals(VisitRecheck.Action.NONE, action(otherApp = false, blocked = true))
    }

    @Test
    fun aVisitThatJustStartedIsAlreadyBeingEvaluated() {
        assertEquals(VisitRecheck.Action.NONE, action(visitStarted = true))
    }

    @Test
    fun anAppNoLongerInFrontIsNotBlockedLate() {
        assertEquals(VisitRecheck.Action.NONE, action(inForeground = false))
    }
}

/** Sprint 73 (T5): seeding the tracker when the loop (re)starts, and overlapping event reads. */
class VisitSeedAndEventWindowTest {

    // ------------------------------------------------ seeding at (re)start

    /** The loop restarted (reboot, update, system restart) with the child already in an app:
     * the look-back read puts it in front and it is evaluated as a new visit. */
    @Test
    fun seedingLearnsTheAppAlreadyInFront() {
        val tracker = VisitTracker()
        val evaluate = tracker.seed(listOf(resumed(HOME), paused(HOME), resumed(FACEBOOK)))
        assertTrue(evaluate)
        assertEquals(FACEBOOK, tracker.currentPackage)
        assertTrue(tracker.inForeground)
    }

    @Test
    fun seedingWithTheLastAppPausedEvaluatesNothing() {
        val tracker = VisitTracker()
        // Screen turned off inside Facebook: nothing is in front.
        assertFalse(tracker.seed(listOf(resumed(FACEBOOK), paused(FACEBOOK))))
        assertEquals(FACEBOOK, tracker.currentPackage)
        // Coming back to it later is a transition of the same package: no new visit by itself...
        assertFalse(tracker.consume(listOf(resumed(FACEBOOK))))
        assertTrue(tracker.inForeground)
    }

    @Test
    fun seedingWithNothingReadKnowsNothing() {
        val tracker = VisitTracker()
        assertFalse(tracker.seed(emptyList()))
        assertNull(tracker.currentPackage)
    }

    @Test
    fun seedingThenANewAppIsANewVisit() {
        val tracker = VisitTracker()
        tracker.seed(listOf(resumed(FACEBOOK)))
        assertTrue(tracker.consume(listOf(paused(FACEBOOK), resumed(HOME))))
        assertEquals(HOME, tracker.currentPackage)
    }

    // ------------------------------------------------ overlapping reads without duplicates

    private fun at(ms: Long, transition: ForegroundTransition) = TimedTransition(ms, transition)

    @Test
    fun eachReadStartsTwoSecondsBeforeThePreviousEnd() {
        val window = EventWindow(initialEnd = 10_000L)
        assertEquals(8_000L, window.nextStart())
        window.accept(emptyList(), queriedUntil = 13_000L)
        assertEquals(11_000L, window.nextStart())
    }

    /** An event logged late, stamped inside the previous window, is still handed over — once. */
    @Test
    fun aLateEventInTheOverlapIsDeliveredOnce() {
        val window = EventWindow(initialEnd = 0L)
        val home = at(9_000L, resumed(HOME))
        assertEquals(listOf(resumed(HOME)), window.accept(listOf(home), queriedUntil = 10_000L))

        // Read again from 8 000: the HOME event comes back (dropped), the late Facebook one is new.
        val lateFacebook = at(9_500L, resumed(FACEBOOK))
        assertEquals(listOf(resumed(FACEBOOK)), window.accept(listOf(home, lateFacebook), queriedUntil = 13_000L))

        // And the next overlapping read hands neither over again.
        assertEquals(emptyList<ForegroundTransition>(), window.accept(listOf(lateFacebook), queriedUntil = 16_000L))
    }

    /** The same RESUMED read twice must not open a second visit (and so a second rule event). */
    @Test
    fun theSameResumedReadTwiceIsOneVisit() {
        val window = EventWindow(initialEnd = 0L)
        val tracker = VisitTracker()
        val facebook = at(9_000L, resumed(FACEBOOK))
        val home = at(9_500L, resumed(HOME))
        assertTrue(tracker.consume(window.accept(listOf(facebook), queriedUntil = 9_200L)))
        // Overlap re-reads Facebook, plus the home screen and Facebook again with a later stamp.
        val back = at(10_500L, resumed(FACEBOOK))
        val second = window.accept(listOf(facebook, home, back), queriedUntil = 11_000L)
        assertEquals(listOf(resumed(HOME), resumed(FACEBOOK)), second)
        assertTrue(tracker.consume(second))
        assertEquals(FACEBOOK, tracker.currentPackage)
        // Re-read once more: nothing new, no new visit.
        assertFalse(tracker.consume(window.accept(listOf(back), queriedUntil = 12_000L)))
    }

    /** A late event older than one already handed over would put the wrong app back in front. */
    @Test
    fun aLateEventOlderThanWhatWasAlreadyHandedOverIsDropped() {
        val window = EventWindow(initialEnd = 0L)
        window.accept(listOf(at(9_800L, resumed(HOME))), queriedUntil = 10_000L)
        val olderLate = at(9_500L, resumed(FACEBOOK))
        assertEquals(emptyList<ForegroundTransition>(), window.accept(listOf(olderLate), queriedUntil = 12_000L))
    }
}
