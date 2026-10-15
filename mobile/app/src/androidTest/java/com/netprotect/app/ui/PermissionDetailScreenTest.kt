package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.permissions.PermissionDetailScreen
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission
import com.netprotect.app.feature.supervised.permissions.permissionDetailSteps
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Sprint 66: Detalle de permiso (lámina 11) with the real steps of each permission. */
class PermissionDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private val nonePending = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true)
    private val allPending = PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false)

    private fun set(permission: SupervisedPermission, permissions: PermissionsUi = allPending) {
        composeRule.setContent {
            NetProtectTheme {
                PermissionDetailScreen(
                    permission = permission,
                    permissions = permissions,
                    onOpenUsageAccessSettings = { calls += "openUsage" },
                    onRequestLocation = { calls += "requestLocation" },
                    onRequestOverlay = { calls += "requestOverlay" },
                    onRequestDeviceAdmin = { calls += "requestDeviceAdmin" },
                    onOpenAppSettings = { calls += "openAppSettings" },
                    onBack = { calls += "back" },
                )
            }
        }
    }

    @Test
    fun overlayPendingShowsPositionChipAndItsSteps() {
        set(SupervisedPermission.Overlay)
        composeRule.onNodeWithText("PERMISO 3 DE 4").performScrollTo().assertExists()
        composeRule.onNodeWithText("Mostrar sobre otras apps").performScrollTo().assertExists()
        composeRule.onNodeWithText("Pendiente").performScrollTo().assertExists()
        composeRule.onNodeWithText("QUÉ HABILITA").performScrollTo().assertExists()
        composeRule.onNodeWithText("Necesario para mostrar la pantalla de bloqueo cuando corresponda.").performScrollTo().assertExists()
        composeRule.onNodeWithText("CÓMO ACTIVARLO").performScrollTo().assertExists()
        permissionDetailSteps(SupervisedPermission.Overlay).forEach {
            composeRule.onNodeWithText(it).performScrollTo().assertExists()
        }
        composeRule.onNodeWithText("Configurado").assertDoesNotExist()
    }

    @Test
    fun eachPendingPermissionFiresItsOwnLauncher() {
        // Footer buttons are pinned outside the scroll: assertIsDisplayed instead of performScrollTo.
        set(SupervisedPermission.UsageAccess)
        composeRule.onNodeWithText("PERMISO 1 DE 4").performScrollTo().assertExists()
        composeRule.onNodeWithText("Activar en Ajustes").assertIsDisplayed().performClick()
        assertEquals(listOf("openUsage"), calls)
    }

    @Test
    fun overlayActivatesWithTheOverlayLauncher() {
        set(SupervisedPermission.Overlay)
        // Pinned footer button.
        composeRule.onNodeWithText("Activar en Ajustes").assertIsDisplayed().performClick()
        assertEquals(listOf("requestOverlay"), calls)
    }

    @Test
    fun locationIsAndroidsDialogWithTheAppSettingsWayOut() {
        set(SupervisedPermission.Location)
        composeRule.onNodeWithText("PERMISO 2 DE 4").performScrollTo().assertExists()
        composeRule.onNodeWithText("Si el cuadro de Android no aparece, actívala desde Ajustes de la app.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Activar en Ajustes").assertDoesNotExist()
        // Pinned footer buttons.
        composeRule.onNodeWithText("Permitir ubicación precisa").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("Abrir Ajustes de la app").assertIsDisplayed().performClick()
        assertEquals(listOf("requestLocation", "openAppSettings"), calls)
    }

    @Test
    fun deviceAdminOpensItsActivationScreenAndOnlyWarns() {
        set(SupervisedPermission.DeviceAdmin)
        composeRule.onNodeWithText("PERMISO 4 DE 4").performScrollTo().assertExists()
        composeRule.onNodeWithText("Avisa a tu tutor si alguien intenta desinstalar NetProtect.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Solo avisa a tu tutor: no bloquea la desinstalación.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Activar en Ajustes").assertDoesNotExist()
        composeRule.onAllNodes(hasText("impide", substring = true, ignoreCase = true)).assertCountEquals(0)
        // Pinned footer button.
        composeRule.onNodeWithText("Activar protección").assertIsDisplayed().performClick()
        assertEquals(listOf("requestDeviceAdmin"), calls)
    }

    @Test
    fun onlyLocationOffersTheAppSettings() {
        set(SupervisedPermission.Overlay)
        composeRule.onNodeWithText("Abrir Ajustes de la app").assertDoesNotExist()
    }

    @Test
    fun configuredShowsStateAndNoActivation() {
        set(SupervisedPermission.Overlay, permissions = nonePending)
        composeRule.onNodeWithText("Configurado").performScrollTo().assertExists()
        composeRule.onNodeWithText("ESTADO").performScrollTo().assertExists()
        composeRule.onNodeWithText("Android confirma que este permiso está activo.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Para desactivarlo, ve a los ajustes de Android.").performScrollTo().assertExists()
        composeRule.onNodeWithText("CÓMO ACTIVARLO").assertDoesNotExist()
        composeRule.onNodeWithText("Activar en Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Pendiente").assertDoesNotExist()
        // Pinned footer button.
        composeRule.onNodeWithText("Volver a permisos").assertIsDisplayed().performClick()
        assertEquals(listOf("back"), calls)
    }

    @Test
    fun locationConfiguredHasNoAppSettingsButton() {
        set(SupervisedPermission.Location, permissions = nonePending)
        composeRule.onNodeWithText("Abrir Ajustes de la app").assertDoesNotExist()
        composeRule.onNodeWithText("Permitir ubicación precisa").assertDoesNotExist()
    }

    @Test
    fun backArrowAndSecondaryButtonCallOnBack() {
        set(SupervisedPermission.UsageAccess)
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        // Pinned footer button.
        composeRule.onNodeWithText("Volver a permisos").assertIsDisplayed().performClick()
        assertEquals(listOf("back", "back"), calls)
    }
}
