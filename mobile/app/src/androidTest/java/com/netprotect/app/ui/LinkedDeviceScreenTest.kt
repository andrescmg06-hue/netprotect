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

/** Inicio (lámina 03), Sprint 64: headline from rule §2.3 and the rows under the panel. */
class LinkedDeviceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-30T15:00:00Z")

    private fun set(
        deviceName: String? = "Tablet de Sofía",
        androidVersion: String = "13",
        tutors: List<String> = listOf("Andrés Mosquera", "María Pérez"),
        lastContact: Instant? = now.minusSeconds(30),
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
    fun allWorkingShowsHeadlineCaptionAndNoButton() {
        set(pendingPermissions = 0)
        composeRule.onNodeWithText("Todo funciona").performScrollTo().assertExists()
        composeRule.onNodeWithText("4 DE 4 PERMISOS").performScrollTo().assertExists()
        composeRule.onNodeWithText(
            "Los 4 permisos están configurados y el dispositivo se comunicó hace un momento.",
        ).performScrollTo().assertExists()
        composeRule.onNodeWithText("Revisar permisos").assertDoesNotExist()
        composeRule.onNodeWithText("Todos configurados").performScrollTo().assertExists()
        composeRule.onNodeWithText("Conectado").performScrollTo().assertExists()
    }

    @Test
    fun twoPendingShowsMissingStepAndReviewButton() {
        var opened = false
        set(pendingPermissions = 2, onOpenPermissions = { opened = true })
        composeRule.onNodeWithText("Falta un paso").performScrollTo().assertExists()
        composeRule.onNodeWithText("2 DE 4 PERMISOS").performScrollTo().assertExists()
        composeRule.onNodeWithText("Faltan 2 permisos para que NetProtect funcione bien en este teléfono.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("2 pendientes").performScrollTo().assertExists()
        composeRule.onNodeWithText("Revisar permisos").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun onePendingUsesTheSingular() {
        set(pendingPermissions = 1)
        composeRule.onNodeWithText("Falta 1 permiso para que NetProtect funcione bien en este teléfono.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("1 pendiente").performScrollTo().assertExists()
    }

    @Test
    fun staleReportShowsNoRecentReport() {
        set(pendingPermissions = 0, lastContact = now.minusSeconds(12 * 60))
        composeRule.onNodeWithText("Sin reporte reciente").performScrollTo().assertExists()
        composeRule.onNodeWithText("No recibimos una actualización reciente del dispositivo. El vínculo sigue activo.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Hace 12 min").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sin actualización reciente").performScrollTo().assertExists()
        composeRule.onNodeWithText("Conectado").assertDoesNotExist()
        composeRule.onNodeWithText("Revisar permisos").assertDoesNotExist()
    }

    @Test
    fun beforeTheFirstReportItNeverSaysConnected() {
        set(pendingPermissions = 0, lastContact = null)
        composeRule.onNodeWithText("Todavía no").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sin reporte reciente").performScrollTo().assertExists()
        composeRule.onNodeWithText("Conectado").assertDoesNotExist()
    }

    @Test
    fun aPendingPermissionWinsOverAMissingReport() {
        set(pendingPermissions = 1, lastContact = null)
        composeRule.onNodeWithText("Falta un paso").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sin reporte reciente").assertDoesNotExist()
    }

    @Test
    fun showsDeviceAndEveryTutor() {
        set(tutors = listOf("Andrés Mosquera", "María Pérez"))
        composeRule.onNodeWithText("Tablet de Sofía").performScrollTo().assertExists()
        composeRule.onNodeWithText("Android 13").performScrollTo().assertExists()
        composeRule.onNodeWithText("Andrés Mosquera").performScrollTo().assertExists()
        composeRule.onNodeWithText("María Pérez").performScrollTo().assertExists()
        composeRule.onNodeWithText("Vinculado").performScrollTo().assertExists()
    }

    @Test
    fun permissionsTileOpensPermissions() {
        var opened = false
        set(pendingPermissions = 0, onOpenPermissions = { opened = true })
        composeRule.onNodeWithText("Permisos").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun servicesTileOpensServices() {
        var opened = false
        set(onOpenServices = { opened = true })
        composeRule.onNodeWithText("Estado de NetProtect").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun moreSettingsOpensAndThereIsNoPrivacyOrSignOutYet() {
        var opened = false
        set(onOpenMoreSettings = { opened = true })
        composeRule.onNodeWithText("Privacidad").assertDoesNotExist()
        composeRule.onNodeWithText("Cerrar sesión").assertDoesNotExist()
        composeRule.onNodeWithText("Desvincular", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Más ajustes").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun theConsentIsNoLongerACardHere() {
        set()
        composeRule.onNodeWithText("Aceptar").assertDoesNotExist()
        composeRule.onNodeWithText("Tu tutor quiere ver esta pantalla").assertDoesNotExist()
    }
}
