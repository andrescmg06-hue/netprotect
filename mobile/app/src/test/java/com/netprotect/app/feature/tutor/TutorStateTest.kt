package com.netprotect.app.feature.tutor

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.PairingCode
import com.netprotect.app.feature.tutor.device.DEVICE_GONE_MESSAGE
import com.netprotect.app.feature.tutor.device.DeviceDetailController
import com.netprotect.app.feature.tutor.device.validDeviceName
import com.netprotect.app.feature.tutor.home.PairingUi
import com.netprotect.app.feature.tutor.home.TutorHomeController
import com.netprotect.app.feature.tutor.home.pairingSecondsLeft
import com.netprotect.app.ui.state.LoadState
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TutorStateTest {

    private fun device(name: String = "Tablet de Sofía") =
        DeviceSummary("d1", name, "ANDROID", "ONLINE", null, null, osVersion = "13")

    // --- pairing countdown ---------------------------------------------------------------

    @Test
    fun countdownUsesElapsedTimeAndRoundsUp() {
        assertEquals(180, pairingSecondsLeft(issuedAtMillis = 1_000, totalSeconds = 180, nowMillis = 1_000))
        assertEquals(180, pairingSecondsLeft(1_000, 180, 1_500))
        assertEquals(1, pairingSecondsLeft(1_000, 180, 180_999))
        assertEquals(0, pairingSecondsLeft(1_000, 180, 181_000))
        assertEquals(0, pairingSecondsLeft(1_000, 180, 999_999))
    }

    @Test
    fun anActiveCodeCountsDownAndExpires() = runBlocking {
        var now = 10_000L
        val home = TutorHomeController(
            loadDevices = { emptyList() },
            createCode = { PairingCode("482917", 180) },
            revokeCode = {},
            clock = { now },
        )
        home.generateCode()
        assertEquals(PairingUi.Active("482917", 180, 180), home.pairing)
        now += 61_000
        home.tick()
        assertEquals(119, (home.pairing as PairingUi.Active).remainingSeconds)
        now += 119_000
        home.tick()
        assertEquals(PairingUi.Expired, home.pairing)
        home.dismissPairing()
        assertEquals(PairingUi.Idle, home.pairing)
    }

    @Test
    fun aFailedRevokeSaysSoInsteadOfHidingTheCode() = runBlocking {
        val home = TutorHomeController(
            loadDevices = { emptyList() },
            createCode = { PairingCode("482917", 180) },
            revokeCode = { throw IOException("offline") },
            clock = { 0L },
        )
        home.generateCode()
        home.revoke()
        assertTrue(home.pairing is PairingUi.Failed)
    }

    @Test
    fun deviceListLoadsAndReportsErrorsInSpanish() = runBlocking {
        var fail = true
        val home = TutorHomeController(
            loadDevices = { if (fail) throw IOException("offline") else listOf(device()) },
            createCode = { PairingCode("1", 1) },
            revokeCode = {},
            clock = { 0L },
        )
        home.refreshDevices()
        val failed = home.devices as LoadState.Failed
        assertFalse(failed.message.contains("IOException"))
        fail = false
        home.refreshDevices()
        assertEquals(listOf(device()), (home.devices as LoadState.Loaded).value)
    }

    // --- device name ---------------------------------------------------------------------

    @Test
    fun deviceNameRules() {
        assertEquals("Tablet", validDeviceName("  Tablet  "))
        assertNull(validDeviceName("   "))
        assertNull(validDeviceName(""))
        assertEquals(255, validDeviceName("a".repeat(255))!!.length)
        assertNull(validDeviceName("a".repeat(256)))
    }

    // --- device detail ------------------------------------------------------------------

    @Test
    fun renameSendsTheTrimmedNameAndShowsErrors() = runBlocking {
        var sent: String? = null
        var fail = true
        val detail = DeviceDetailController(
            loadDevice = { device() },
            renameDevice = { name -> sent = name; if (fail) throw IOException("offline") else device(name) },
            unlinkDevice = {},
        )
        detail.refresh()
        detail.startRename()
        detail.editRename("  Celular de Juan ")
        detail.saveRename()
        assertEquals("Celular de Juan", sent)
        assertTrue(detail.rename!!.error != null)
        assertFalse(detail.rename!!.saving)
        fail = false
        detail.saveRename()
        assertNull(detail.rename)
        assertEquals("Celular de Juan", (detail.device as LoadState.Loaded).value.name)
    }

    @Test
    fun anInvalidNameIsNeverSent() = runBlocking {
        var calls = 0
        val detail = DeviceDetailController(
            loadDevice = { device() },
            renameDevice = { calls++; device() },
            unlinkDevice = {},
        )
        detail.refresh()
        detail.startRename()
        detail.editRename("   ")
        assertFalse(detail.rename!!.canSave)
        detail.saveRename()
        assertEquals(0, calls)
    }

    @Test
    fun notFoundMeansTheDeviceIsGone() = runBlocking {
        val detail = DeviceDetailController(
            loadDevice = { throw ApiException("device_not_found", 404) },
            renameDevice = { device() },
            unlinkDevice = {},
        )
        detail.refresh()
        assertEquals(LoadState.Failed(DEVICE_GONE_MESSAGE, notFound = true), detail.device)
    }

    @Test
    fun unlinkReportsSuccessOrShowsTheError() = runBlocking {
        var fail = true
        val detail = DeviceDetailController(
            loadDevice = { device() },
            renameDevice = { device() },
            unlinkDevice = { if (fail) throw IOException("offline") },
        )
        detail.refresh()
        assertFalse(detail.unlink())
        assertTrue(detail.unlinkError != null)
        assertFalse(detail.unlinking)
        fail = false
        assertTrue(detail.unlink())
        assertNull(detail.unlinkError)
    }
}
