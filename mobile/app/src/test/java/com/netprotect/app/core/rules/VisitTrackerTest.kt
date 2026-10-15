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
