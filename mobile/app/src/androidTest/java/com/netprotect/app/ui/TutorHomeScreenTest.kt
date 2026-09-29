package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.home.PairingUi
import com.netprotect.app.feature.tutor.home.TutorHomeScreen
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TutorHomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-29T15:00:00Z")

    private fun device(
        id: String,
        name: String,
        status: String = "ONLINE",
        lastSeenAt: String? = now.minusSeconds(720).toString(),
        osVersion: String? = "13",
    ) = DeviceSummary(
        id = id,
        name = name,
        platform = "ANDROID",
        status = status,
        lastSeenAt = lastSeenAt,
        timezone = "America/Bogota",
        osVersion = osVersion,
        appVersion = "0.1.0",
    )

    private fun set(
        pairing: PairingUi = PairingUi.Idle,
        devices: LoadState<List<DeviceSummary>> = LoadState.Loaded(emptyList()),
        userName: String? = "Andrés Mosquera",
        onGenerateCode: () -> Unit = {},
        onRevokeCode: () -> Unit = {},
        onDismissPairing: () -> Unit = {},
        onOpenDevice: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                TutorHomeScreen(
                    userName = userName,
                    userEmail = "tutor@example.com",
                    pairing = pairing,
                    devices = devices,
                    now = now,
                    onGenerateCode = onGenerateCode,
                    onRevokeCode = onRevokeCode,
                    onDismissPairing = onDismissPairing,
                    onRefreshDevices = {},
                    onOpenDevice = onOpenDevice,
                    onOpenActivity = {},
                    onSwitchMode = {},
                    onSignOut = {},
                )
            }
        }
    }

    @Test
    fun idleGenerateCallsOnGenerateCode() {
        var generated = false
        set(pairing = PairingUi.Idle, onGenerateCode = { generated = true })
        composeRule.onNodeWithText("Generar código de vinculación").performScrollTo().performClick()
        assertTrue(generated)
    }

    @Test
    fun activeShowsUsoUnicoAndRevokeCallsOnRevokeCode() {
        var revoked = false
        set(
            pairing = PairingUi.Active(code = "482917", remainingSeconds = 143, totalSeconds = 180),
            onRevokeCode = { revoked = true },
        )
        composeRule.onNodeWithText("Uso único. Introdúcelo en el teléfono que vas a supervisar.").assertExists()
        composeRule.onNodeWithText("Revocar").performScrollTo().performClick()
        assertTrue(revoked)
    }

    @Test
    fun expiredShowsElCodigoVencio() {
        set(pairing = PairingUi.Expired)
        composeRule.onNodeWithText("El código venció. Genera uno nuevo.").assertExists()
    }

    @Test
    fun failedShowsMessageAndEntendidoCallsOnDismissPairing() {
        var dismissed = false
        set(pairing = PairingUi.Failed("No fue posible contactar la API"), onDismissPairing = { dismissed = true })
        composeRule.onNodeWithText("No fue posible contactar la API").assertExists()
        composeRule.onNodeWithText("Entendido").performScrollTo().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun emptyListShowsTodaviaNoHayDispositivos() {
        set(devices = LoadState.Loaded(emptyList()))
        composeRule.onNodeWithText("Todavía no hay dispositivos vinculados.").performScrollTo().assertExists()
    }

    @Test
    fun tappingDeviceCallsOnOpenDevice() {
        var opened: String? = null
        set(
            devices = LoadState.Loaded(listOf(device("dev-tablet", "Tablet de Sofía"))),
            onOpenDevice = { opened = it },
        )
        composeRule.onNodeWithText("Tablet de Sofía").performScrollTo().performClick()
        assertEquals("dev-tablet", opened)
    }

    @Test
    fun nullOsVersionShowsPlainAndroid() {
        set(devices = LoadState.Loaded(listOf(device("dev-1", "Tablet", osVersion = null))))
        composeRule.onNodeWithText("Android", substring = false).performScrollTo().assertExists()
        composeRule.onNodeWithText("Android null").assertDoesNotExist()
    }

    @Test
    fun statusShownAsEnLineaNotOnline() {
        set(devices = LoadState.Loaded(listOf(device("dev-1", "Tablet", status = "ONLINE"))))
        composeRule.onNodeWithText("En línea").performScrollTo().assertExists()
        composeRule.onNodeWithText("ONLINE").assertDoesNotExist()
    }

    @Test
    fun nullLastSeenAtShowsSinActividad() {
        set(devices = LoadState.Loaded(listOf(device("dev-1", "Tablet", lastSeenAt = null))))
        composeRule.onNodeWithText("Sin actividad registrada todavía.").performScrollTo().assertExists()
    }
}
