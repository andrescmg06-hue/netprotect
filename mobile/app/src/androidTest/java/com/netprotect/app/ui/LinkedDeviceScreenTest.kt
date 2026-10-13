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
        onOpenServices: () -> Unit = {},
        onOpenPermissions: () -> Unit = {},
        onOpenMoreSettings: () -> Unit = {},
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
                    onOpenServices = onOpenServices,
                    onOpenPermissions = onOpenPermissions,
                    onOpenMoreSettings = onOpenMoreSettings,
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
    fun moreSettingsReplacesSignOut() {
        var opened = false
        set(onOpenMoreSettings = { opened = true })
        composeRule.onNodeWithText("Cerrar sesión").assertDoesNotExist()
        composeRule.onNodeWithText("Desvincular", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Más ajustes").performScrollTo().performClick()
        assertTrue(opened)
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
    fun servicesRowOpensServices() {
        var opened = false
        set(onOpenServices = { opened = true })
        composeRule.onNodeWithText("Estado de NetProtect").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun theConsentIsNoLongerACardHere() {
        set()
        composeRule.onNodeWithText("Aceptar").assertDoesNotExist()
        composeRule.onNodeWithText("Tu tutor quiere ver esta pantalla").assertDoesNotExist()
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
}
