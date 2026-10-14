package com.netprotect.app.feature.supervised.linked

import com.netprotect.app.core.status.REPORT_STALE_AFTER_SECONDS
import com.netprotect.app.ui.components.SealArcState
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeStatusTest {

    private val now = Instant.parse("2026-10-01T15:00:00Z")
    private fun beatAgo(seconds: Long) = now.minusSeconds(seconds)

    // ---- rule §2.3 ---------------------------------------------------------------------------

    @Test
    fun allPermissionsAndARecentBeatIsAllWorking() {
        assertEquals(HomeStatus.AllWorking, homeStatus(0, beatAgo(30), now))
    }

    @Test
    fun thresholdIsInclusiveAtExactlyThreeHundredSeconds() {
        assertEquals(300L, REPORT_STALE_AFTER_SECONDS)
        assertEquals(HomeStatus.AllWorking, homeStatus(0, beatAgo(299), now))
        assertEquals(HomeStatus.AllWorking, homeStatus(0, beatAgo(300), now))
        assertEquals(HomeStatus.NoRecentReport, homeStatus(0, beatAgo(301), now))
    }

    @Test
    fun noBeatYetIsNoRecentReportNeverAllWorking() {
        // The old screen said "Conectado" before the first heartbeat; the rule must not.
        assertEquals(HomeStatus.NoRecentReport, homeStatus(0, null, now))
    }

    @Test
    fun pendingPermissionWinsOverAMissingReport() {
        assertEquals(HomeStatus.MissingStep(1), homeStatus(1, null, now))
        assertEquals(HomeStatus.MissingStep(2), homeStatus(2, beatAgo(3_600), now))
        assertEquals(HomeStatus.MissingStep(3), homeStatus(3, beatAgo(10), now))
    }

    @Test
    fun injectedNowIsTheOnlyClock() {
        val beat = Instant.parse("2020-01-01T00:00:00Z")
        assertEquals(HomeStatus.AllWorking, homeStatus(0, beat, beat.plusSeconds(300)))
        assertEquals(HomeStatus.NoRecentReport, homeStatus(0, beat, beat.plusSeconds(301)))
    }

    @Test
    fun customThresholdIsRespected() {
        assertEquals(HomeStatus.AllWorking, homeStatus(0, beatAgo(60), now, staleAfterSeconds = 60))
        assertEquals(HomeStatus.NoRecentReport, homeStatus(0, beatAgo(61), now, staleAfterSeconds = 60))
    }

    // ---- texts (02_PANTALLAS §03) ----------------------------------------------------------

    @Test
    fun titlesAreTheLaminaHeadlines() {
        assertEquals("Todo funciona", homeStatusTitle(HomeStatus.AllWorking))
        assertEquals("Falta un paso", homeStatusTitle(HomeStatus.MissingStep(2)))
        assertEquals("Sin reporte reciente", homeStatusTitle(HomeStatus.NoRecentReport))
    }

    @Test
    fun descriptionsAreLiteralWithCorrectSingular() {
        assertEquals(
            "Los 4 permisos están configurados y el dispositivo se comunicó hace un momento.",
            homeStatusDescription(HomeStatus.AllWorking),
        )
        assertEquals(
            "Falta 1 permiso para que NetProtect funcione bien en este teléfono.",
            homeStatusDescription(HomeStatus.MissingStep(1)),
        )
        assertEquals(
            "Faltan 2 permisos para que NetProtect funcione bien en este teléfono.",
            homeStatusDescription(HomeStatus.MissingStep(2)),
        )
        assertEquals(
            "No recibimos una actualización reciente del dispositivo. El vínculo sigue activo.",
            homeStatusDescription(HomeStatus.NoRecentReport),
        )
    }

    @Test
    fun onlyMissingStepOffersReviewPermissions() {
        assertEquals("Revisar permisos", homeStatusAction(HomeStatus.MissingStep(1)))
        assertNull(homeStatusAction(HomeStatus.AllWorking))
        assertNull(homeStatusAction(HomeStatus.NoRecentReport))
    }

    // ---- seal ----------------------------------------------------------------------------------

    @Test
    fun pendingWithoutReportKeepsPendingArcsVisible() {
        val status = homeStatus(2, null, now)
        assertEquals(
            listOf(SealArcState.Ready, SealArcState.Ready, SealArcState.Pending, SealArcState.Pending),
            homeSealStates(status, readyCount = 2),
        )
    }

    @Test
    fun noRecentReportGreysEveryArc() {
        assertEquals(List(4) { SealArcState.Inactive }, homeSealStates(HomeStatus.NoRecentReport, readyCount = 4))
        assertEquals(List(4) { SealArcState.Ready }, homeSealStates(HomeStatus.AllWorking, readyCount = 4))
    }
}
