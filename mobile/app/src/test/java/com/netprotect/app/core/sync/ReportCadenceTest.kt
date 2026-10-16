package com.netprotect.app.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 73 (T7): when RuleEnforcementService sends the heartbeat and the app/usage sync with the app closed. Pure
 * functions of (now, last attempt, last success by anyone); times are monotonic milliseconds, 0 = never. */
class ReportCadenceTest {

    private val hb = ReportCadence.HEARTBEAT_INTERVAL_MS
    private val sync = ReportCadence.APP_SYNC_INTERVAL_MS
    private val margin = ReportCadence.BACKSTOP_MARGIN_MS

    @Test
    fun `the intervals are the ones the screen used before`() {
        assertEquals(60_000L, hb)
        assertEquals(5 * 60_000L, sync)
    }

    @Test
    fun `the backstop margin keeps the heartbeat well inside the 5 minutes the backend allows`() {
        assertTrue(hb + margin < 5 * 60_000L)
    }

    @Test
    fun `a heartbeat is due at once when none was ever attempted`() {
        assertTrue(ReportCadence.heartbeatDue(nowMs = 5_000L, lastAttemptMs = 0L, lastSuccessMs = 0L))
    }

    @Test
    fun `a heartbeat is not due before the interval plus the margin since the last attempt`() {
        val last = 1_000_000L
        assertFalse(ReportCadence.heartbeatDue(last + 1, lastAttemptMs = last, lastSuccessMs = 0L))
        assertFalse(ReportCadence.heartbeatDue(last + hb, lastAttemptMs = last, lastSuccessMs = 0L))
        assertFalse(ReportCadence.heartbeatDue(last + hb + margin - 1, lastAttemptMs = last, lastSuccessMs = 0L))
        assertTrue(ReportCadence.heartbeatDue(last + hb + margin, lastAttemptMs = last, lastSuccessMs = 0L))
    }

    @Test
    fun `a failed attempt is retried at the next due time, not at every tick`() {
        val attempt = 2_000_000L
        // Nothing succeeded (offline): the attempt itself holds the next one back.
        assertFalse(ReportCadence.heartbeatDue(attempt + 3_000L, lastAttemptMs = attempt, lastSuccessMs = 0L))
        assertTrue(ReportCadence.heartbeatDue(attempt + hb + margin, lastAttemptMs = attempt, lastSuccessMs = 0L))
    }

    @Test
    fun `a heartbeat sent by the screen with the app open keeps the service quiet`() {
        // The screen sends every 60 s: its last success is always within the interval, so the service never reaches
        // interval + margin and sends nothing while the loop runs.
        val lastSuccessByScreen = 3_000_000L
        assertFalse(ReportCadence.heartbeatDue(lastSuccessByScreen + hb + 1_000L, lastAttemptMs = 0L, lastSuccessMs = lastSuccessByScreen))
        assertFalse(ReportCadence.heartbeatDue(lastSuccessByScreen + hb + margin - 1, lastAttemptMs = 0L, lastSuccessMs = lastSuccessByScreen))
    }

    @Test
    fun `the service steps in once the screen has been silent for the interval plus the margin`() {
        val lastSuccessByScreen = 3_000_000L
        assertTrue(ReportCadence.heartbeatDue(lastSuccessByScreen + hb + margin, lastAttemptMs = 0L, lastSuccessMs = lastSuccessByScreen))
    }

    @Test
    fun `the newer of the last attempt and the last success counts`() {
        val old = 1_000_000L
        val recent = old + 10 * hb
        assertFalse(ReportCadence.heartbeatDue(recent + hb, lastAttemptMs = old, lastSuccessMs = recent))
        assertFalse(ReportCadence.heartbeatDue(recent + hb, lastAttemptMs = recent, lastSuccessMs = old))
        assertTrue(ReportCadence.heartbeatDue(recent + hb + margin, lastAttemptMs = recent, lastSuccessMs = old))
    }

    @Test
    fun `the app sync is never due without usage access`() {
        assertFalse(ReportCadence.appSyncDue(nowMs = 9_000_000L, lastAttemptMs = 0L, lastSuccessMs = 0L, usageAccessGranted = false))
        assertFalse(ReportCadence.appSyncDue(nowMs = 9_000_000L, lastAttemptMs = 1L, lastSuccessMs = 1L, usageAccessGranted = false))
    }

    @Test
    fun `the app sync is due at once with usage access and nothing sent yet`() {
        assertTrue(ReportCadence.appSyncDue(nowMs = 5_000L, lastAttemptMs = 0L, lastSuccessMs = 0L, usageAccessGranted = true))
    }

    @Test
    fun `the app sync waits its own interval, much longer than the heartbeat's`() {
        val last = 4_000_000L
        // A heartbeat would be due here; the sync (heavier) is not.
        assertTrue(ReportCadence.heartbeatDue(last + hb + margin, lastAttemptMs = last, lastSuccessMs = 0L))
        assertFalse(ReportCadence.appSyncDue(last + hb + margin, lastAttemptMs = last, lastSuccessMs = 0L, usageAccessGranted = true))
        assertFalse(ReportCadence.appSyncDue(last + sync + margin - 1, lastAttemptMs = last, lastSuccessMs = 0L, usageAccessGranted = true))
        assertTrue(ReportCadence.appSyncDue(last + sync + margin, lastAttemptMs = last, lastSuccessMs = 0L, usageAccessGranted = true))
    }

    @Test
    fun `a sync done by the screen or SyncWorker holds the service's sync back`() {
        val bySomeoneElse = 6_000_000L
        assertFalse(ReportCadence.appSyncDue(bySomeoneElse + sync, lastAttemptMs = 0L, lastSuccessMs = bySomeoneElse, usageAccessGranted = true))
        assertTrue(ReportCadence.appSyncDue(bySomeoneElse + sync + margin, lastAttemptMs = 0L, lastSuccessMs = bySomeoneElse, usageAccessGranted = true))
    }

    @Test
    fun `the report clock keeps the last success of each report apart`() {
        ReportClock.reset()
        assertEquals(0L, ReportClock.lastHeartbeatSuccessMs)
        ReportClock.heartbeatSucceeded(123L)
        assertEquals(123L, ReportClock.lastHeartbeatSuccessMs)
        assertEquals(0L, ReportClock.lastAppSyncSuccessMs)
        ReportClock.appSyncSucceeded(456L)
        assertEquals(456L, ReportClock.lastAppSyncSuccessMs)
        assertEquals(123L, ReportClock.lastHeartbeatSuccessMs)
        ReportClock.reset()
    }
}
