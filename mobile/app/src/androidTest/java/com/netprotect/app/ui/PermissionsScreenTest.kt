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
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Permisos (lámina 04), Sprint 64: rail with the pending permissions first, a button on each pending step. */
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
                    onOpenDetail = { calls += "detail:${it.name}" },
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
    fun everyPendingStepHasItsOwnButton() {
        set()
        // Overlay is the next step, device admin the other pending one: two buttons, each with its own callback.
        // Device admin opens Android's activation screen, not Settings (PLAN §2.1): «Activar protección».
        composeRule.onAllNodesWithText("Activar en Ajustes").assertCountEquals(1)
        composeRule.onNodeWithText("Activar en Ajustes").performScrollTo().performClick()
        composeRule.onNodeWithText("Activar protección").performScrollTo().performClick()
        assertEquals(listOf("requestOverlay", "requestDeviceAdmin"), calls)
    }

    @Test
    fun withAllPendingEachButtonCallsItsOwnCallback() {
        set(permissions = PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false))
        // Only usage access and overlay open Settings; location is Android's dialog, device admin its activation screen.
        composeRule.onAllNodesWithText("Activar en Ajustes").assertCountEquals(2)
        composeRule.onNodeWithText("Permitir ubicación precisa").performScrollTo().performClick()
        composeRule.onAllNodesWithText("Activar en Ajustes")[0].performScrollTo().performClick()
        composeRule.onAllNodesWithText("Activar en Ajustes")[1].performScrollTo().performClick()
        composeRule.onNodeWithText("Activar protección").performScrollTo().performClick()
        assertEquals(listOf("requestLocation", "openUsage", "requestOverlay", "requestDeviceAdmin"), calls)
    }

    @Test
    fun locationAsNextAsksForTheApproximateLocation() {
        set(permissions = PermissionsUi(usageAccess = true, location = false, overlay = true, deviceAdmin = true))
        composeRule.onNodeWithText("Permitir ubicación precisa").performScrollTo().performClick()
        assertEquals(listOf("requestLocation"), calls)
    }

    @Test
    fun deviceAdminAsNextRequestsDeviceAdmin() {
        set(permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = false))
        composeRule.onNodeWithText("Activar en Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Activar protección").performScrollTo().performClick()
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
        composeRule.onNodeWithText("Activar protección").assertDoesNotExist()
        composeRule.onNodeWithText("Permitir", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Si revocas un permiso en Ajustes, lo comprobamos al volver a la app.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Verificar de nuevo").performScrollTo().performClick()
        assertEquals(listOf("recheckUsage", "recheckLocation", "recheckOverlay"), calls)
        composeRule.onNodeWithText("Volver al inicio").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun everyStepPendingOrConfiguredOpensItsDetail() {
        set()
        // Rail order: pending first (overlay, device admin), then the configured ones (usage access, location).
        composeRule.onAllNodesWithText("Ver detalle").assertCountEquals(4)
        repeat(4) { index -> composeRule.onAllNodesWithText("Ver detalle")[index].performScrollTo().performClick() }
        assertEquals(
            listOf(SupervisedPermission.Overlay, SupervisedPermission.DeviceAdmin, SupervisedPermission.UsageAccess, SupervisedPermission.Location)
                .map { "detail:${it.name}" },
            calls,
        )
    }

    // Precise location since 2026-10-14 (owner's decision): nothing may still promise only the approximate one.
    @Test
    fun asksForThePreciseLocation() {
        set()
        composeRule.onNodeWithText("Permite reportar a tu tutor la ubicación precisa", substring = true).performScrollTo().assertExists()
        composeRule.onAllNodes(hasText("No se usa la ubicación precisa", substring = true)).assertCountEquals(0)
    }

    @Test
    fun approximateOnlyIsPendingWithItsOwnTextAndButton() {
        set(
            permissions = PermissionsUi(
                usageAccess = true, location = false, overlay = true, deviceAdmin = true, locationApproximateOnly = true,
            ),
        )
        composeRule.onNodeWithText("Ahora solo está permitida la ubicación aproximada", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("Permitir ubicación precisa").performScrollTo().performClick()
        assertEquals(listOf("requestLocation"), calls)
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
