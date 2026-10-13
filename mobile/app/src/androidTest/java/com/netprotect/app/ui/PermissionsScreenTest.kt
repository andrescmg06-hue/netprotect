package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Permisos (lámina 04), Sprint 64: rail with the pending permissions first and one button, on the next one. */
class PermissionsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun set(
        permissions: PermissionsUi = PermissionsUi(usageAccess = true, location = true, overlay = false, deviceAdmin = false),
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                PermissionsScreen(
                    permissions = permissions,
                    onOpenUsageAccessSettings = { calls += "openUsage" },
                    onRecheckUsageAccess = { calls += "recheckUsage" },
                    onRequestLocation = { calls += "requestLocation" },
                    onRecheckLocation = { calls += "recheckLocation" },
                    onRequestOverlay = { calls += "requestOverlay" },
                    onRecheckOverlay = { calls += "recheckOverlay" },
                    onRequestDeviceAdmin = { calls += "requestDeviceAdmin" },
                    onBack = onBack,
                )
            }
        }
    }

    @Test
    fun twoPendingShowsTheSlideTexts() {
        set()
        composeRule.onNodeWithText("Permisos").performScrollTo().assertExists()
        composeRule.onNodeWithText("NetProtect necesita estos cuatro permisos en este teléfono.").performScrollTo().assertExists()
        composeRule.onNodeWithText("2 de 4 configurados").performScrollTo().assertExists()
        composeRule.onNodeWithText("Pendiente · siguiente").performScrollTo().assertExists()
        composeRule.onNodeWithText("Necesario para mostrar la pantalla de bloqueo cuando corresponda.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Avisa a tu tutor si alguien intenta desinstalar NetProtect.").performScrollTo().assertExists()
        composeRule.onAllNodesWithText("Pendiente").assertCountEquals(1)
        composeRule.onAllNodesWithText("Configurado").assertCountEquals(2)
    }

    @Test
    fun onlyTheNextStepHasAButtonAndItRequestsThatPermission() {
        set()
        composeRule.onAllNodesWithText("Activar en Ajustes").assertCountEquals(1)
        composeRule.onNodeWithText("Activar en Ajustes").performScrollTo().performClick()
        assertEquals(listOf("requestOverlay"), calls)
    }

    @Test
    fun eachPermissionAsNextCallsItsOwnCallback() {
        set(permissions = PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false))
        composeRule.onNodeWithText("Activar en Ajustes").performScrollTo().performClick()
        assertEquals(listOf("openUsage"), calls)
    }

    @Test
    fun locationAsNextAsksForTheApproximateLocation() {
        set(permissions = PermissionsUi(usageAccess = true, location = false, overlay = true, deviceAdmin = true))
        composeRule.onNodeWithText("Permitir ubicación aproximada").performScrollTo().performClick()
        assertEquals(listOf("requestLocation"), calls)
    }

    @Test
    fun deviceAdminAsNextRequestsDeviceAdmin() {
        set(permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = false))
        composeRule.onNodeWithText("Activar en Ajustes").performScrollTo().performClick()
        assertEquals(listOf("requestDeviceAdmin"), calls)
    }

    @Test
    fun recheckButtonRechecksEveryPermissionItCan() {
        set()
        composeRule.onNodeWithText("Ya lo activé, verificar de nuevo").performScrollTo().performClick()
        assertEquals(listOf("recheckUsage", "recheckLocation", "recheckOverlay"), calls)
    }

    @Test
    fun allConfiguredShowsNoActivationAndGoesBackHome() {
        var back = false
        set(permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true), onBack = { back = true })
        composeRule.onNodeWithText("Todos los permisos están configurados").performScrollTo().assertExists()
        composeRule.onAllNodesWithText("Configurado").assertCountEquals(4)
        composeRule.onNodeWithText("Activar en Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Permitir", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Si revocas un permiso en Ajustes, lo comprobamos al volver a la app.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Verificar de nuevo").performScrollTo().performClick()
        assertEquals(listOf("recheckUsage", "recheckLocation", "recheckOverlay"), calls)
        composeRule.onNodeWithText("Volver al inicio").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun noPrecisaText() {
        set()
        composeRule.onNodeWithText("No se usa la ubicación precisa.", substring = true).performScrollTo().assertExists()
        composeRule.onAllNodes(hasText("precisa", substring = true)).assertCountEquals(1)
    }

    @Test
    fun neverClaimsToPreventUninstalling() {
        set()
        composeRule.onAllNodes(hasText("impide", substring = true)).assertCountEquals(0)
    }

    @Test
    fun backCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }
}
