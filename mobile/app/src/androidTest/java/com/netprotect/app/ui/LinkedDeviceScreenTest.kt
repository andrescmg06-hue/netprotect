package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LinkedDeviceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-30T15:00:00Z")

    private fun set(
        deviceName: String? = "Tablet de Sofía",
        androidVersion: String = "13",
        tutors: List<String> = listOf("Andrés Mosquera", "María Pérez"),
        lastContact: Instant? = now.minusSeconds(120),
        reachable: Boolean = true,
        pendingPermissions: Int = 2,
        screenShareRequested: Boolean = false,
        onAcceptScreenShare: () -> Unit = {},
        onDeclineScreenShare: () -> Unit = {},
        onOpenPermissions: () -> Unit = {},
        onSwitchMode: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                LinkedDeviceScreen(
                    deviceName = deviceName,
                    androidVersion = androidVersion,
                    tutors = tutors,
                    lastContact = lastContact,
                    reachable = reachable,
                    now = now,
                    pendingPermissions = pendingPermissions,
                    screenShareRequested = screenShareRequested,
                    onAcceptScreenShare = onAcceptScreenShare,
                    onDeclineScreenShare = onDeclineScreenShare,
                    onOpenPermissions = onOpenPermissions,
                    onSwitchMode = onSwitchMode,
                    onSignOut = onSignOut,
                )
            }
        }
    }

    @Test
    fun showsTwoTutors() {
        set(tutors = listOf("Andrés Mosquera", "María Pérez"))
        composeRule.onNodeWithText("Andrés Mosquera").performScrollTo().assertExists()
        composeRule.onNodeWithText("María Pérez").performScrollTo().assertExists()
    }

    @Test
    fun showsSignOutSubtitleNotDesvincular() {
        set()
        composeRule.onNodeWithText("Cierra la sesión en este dispositivo.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Desvincular", substring = true).assertDoesNotExist()
    }

    @Test
    fun twoPendingShowsLabel() {
        set(pendingPermissions = 2)
        composeRule.onNodeWithText("2 permisos pendientes").performScrollTo().assertExists()
    }

    @Test
    fun zeroPendingShowsAllConfigured() {
        set(pendingPermissions = 0)
        composeRule.onNodeWithText("Todos los permisos están configurados").performScrollTo().assertExists()
    }

    @Test
    fun reviewPermissionsCallsOnOpenPermissions() {
        var opened = false
        set(onOpenPermissions = { opened = true })
        composeRule.onNodeWithText("Revisar permisos").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun screenShareRequestedButtonsCallCallbacks() {
        var accepted = false
        var declined = false
        set(
            screenShareRequested = true,
            onAcceptScreenShare = { accepted = true },
            onDeclineScreenShare = { declined = true },
        )
        composeRule.onNodeWithText("Aceptar").performScrollTo().performClick()
        assertTrue(accepted)
        composeRule.onNodeWithText("Ahora no").performScrollTo().performClick()
        assertTrue(declined)
    }

    @Test
    fun noScreenShareHidesButtons() {
        set(screenShareRequested = false)
        composeRule.onNodeWithText("Aceptar").assertDoesNotExist()
        composeRule.onNodeWithText("Ahora no").assertDoesNotExist()
    }

    @Test
    fun unreachableShowsSinConexion() {
        set(reachable = false)
        composeRule.onNodeWithText("Sin conexión").performScrollTo().assertExists()
    }

    @Test
    fun noContactShowsTodaviaNo() {
        set(lastContact = null)
        composeRule.onNodeWithText("Todavía no").performScrollTo().assertExists()
    }

    @Test
    fun switchModeCallsOnSwitchMode() {
        var switched = false
        set(onSwitchMode = { switched = true })
        composeRule.onNodeWithText("Cambiar de modo").performScrollTo().performClick()
        assertTrue(switched)
    }
}
