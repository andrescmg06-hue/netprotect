package com.netprotect.app.core.status

import com.netprotect.app.core.network.ReconnectPolicy
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStatusTest {

    private val now = Instant.parse("2026-10-01T15:00:00Z")

    @After
    fun clean() = ServiceStatusRegistry.reset()

    // ---- reconexión (B-02) ----------------------------------------------------------------

    @Test
    fun reconnectWaitsGrowThenStayAtThirtySeconds() {
        assertEquals(listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (0..6).map { ReconnectPolicy.delayFor(it) })
        // A long outage never overflows or exceeds the cap.
        assertEquals(30_000L, ReconnectPolicy.delayFor(10_000))
        assertEquals(1_000L, ReconnectPolicy.delayFor(-1))
    }

    @Test
    fun onlyNotYourDeviceStopsRetrying() {
        assertTrue(ReconnectPolicy.isPermanent(4404))
        assertFalse(ReconnectPolicy.isPermanent(4401)) // token rejected: a fresh one may work
        assertFalse(ReconnectPolicy.isPermanent(1006)) // network drop
        assertFalse(ReconnectPolicy.isPermanent(1000))
    }

    // ---- "Estado de NetProtect" never claims more than it knows ------------------------------

    @Test
    fun reportIsActiveOnlyWithARecentHeartbeat() {
        assertEquals(ServiceState.Unconfirmed, servicesView(ServiceStatus(), null, now).report)
        val recent = ServiceStatus(lastHeartbeatOk = now.minusSeconds(90))
        assertEquals(ServiceState.Active, servicesView(recent, null, now).report)
        val stale = ServiceStatus(lastHeartbeatOk = now.minusSeconds(REPORT_STALE_AFTER_SECONDS + 1))
        assertEquals(ServiceState.Unconfirmed, servicesView(stale, null, now).report)
        assertEquals(stale.lastHeartbeatOk, servicesView(stale, null, now).lastReport)
    }

    @Test
    fun appControlNeedsBothTheServiceAndItsLivenessMark() {
        assertEquals(ServiceState.Inactive, servicesView(ServiceStatus(enforcementRunning = false), true, now).appControl)
        assertEquals(ServiceState.Active, servicesView(ServiceStatus(enforcementRunning = true), true, now).appControl)
        // Running but not stamping (killed without onDestroy, or not started yet): not "Activo".
        assertEquals(ServiceState.Unconfirmed, servicesView(ServiceStatus(enforcementRunning = true), false, now).appControl)
        assertEquals(ServiceState.Unconfirmed, servicesView(ServiceStatus(enforcementRunning = true), null, now).appControl)
    }

    @Test
    fun registryFollowsWhatTheServicesReport() {
        ServiceStatusRegistry.locationRunning(true)
        ServiceStatusRegistry.screenShareStarted(now)
        ServiceStatusRegistry.heartbeatSucceeded(now)
        var view = servicesView(ServiceStatusRegistry.status.value, null, now)
        assertEquals(ServiceState.Active, view.location)
        assertTrue(view.screenShareActive)
        assertEquals(now, view.screenShareSince)
        assertEquals(ServiceState.Active, view.report)

        ServiceStatusRegistry.screenShareStopped()
        ServiceStatusRegistry.locationRunning(false)
        view = servicesView(ServiceStatusRegistry.status.value, null, now)
        assertFalse(view.screenShareActive)
        assertNull(view.screenShareSince)
        assertEquals(ServiceState.Inactive, view.location)
    }
}
