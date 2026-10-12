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
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PermissionsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        permissions: PermissionsUi = PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false),
        onOpenUsageAccessSettings: () -> Unit = {},
        onRecheckUsageAccess: () -> Unit = {},
        onRequestLocation: () -> Unit = {},
        onRecheckLocation: () -> Unit = {},
        onRequestOverlay: () -> Unit = {},
        onRecheckOverlay: () -> Unit = {},
        onRequestDeviceAdmin: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                PermissionsScreen(
                    permissions = permissions,
                    onOpenUsageAccessSettings = onOpenUsageAccessSettings,
                    onRecheckUsageAccess = onRecheckUsageAccess,
                    onRequestLocation = onRequestLocation,
                    onRecheckLocation = onRecheckLocation,
                    onRequestOverlay = onRequestOverlay,
                    onRecheckOverlay = onRecheckOverlay,
                    onRequestDeviceAdmin = onRequestDeviceAdmin,
                    onBack = onBack,
                )
            }
        }
    }

    @Test
    fun allPendingButtonsCallCallbacks() {
        var openUsage = false
        var recheckUsage = false
        var requestLocation = false
        var recheckLocation = false
        var requestOverlay = false
        var recheckOverlay = false
        var requestDeviceAdmin = false
        set(
            onOpenUsageAccessSettings = { openUsage = true },
            onRecheckUsageAccess = { recheckUsage = true },
            onRequestLocation = { requestLocation = true },
            onRecheckLocation = { recheckLocation = true },
            onRequestOverlay = { requestOverlay = true },
            onRecheckOverlay = { recheckOverlay = true },
            onRequestDeviceAdmin = { requestDeviceAdmin = true },
        )
        composeRule.onAllNodesWithText("Abrir Ajustes")[0].performScrollTo().performClick()
        assertTrue(openUsage)
        composeRule.onAllNodesWithText("Ya lo activé, verificar de nuevo")[0].performScrollTo().performClick()
        assertTrue(recheckUsage)
        composeRule.onNodeWithText("Permitir ubicación aproximada").performScrollTo().performClick()
        assertTrue(requestLocation)
        composeRule.onAllNodesWithText("Ya lo activé, verificar de nuevo")[1].performScrollTo().performClick()
        assertTrue(recheckLocation)
        composeRule.onAllNodesWithText("Abrir Ajustes")[1].performScrollTo().performClick()
        assertTrue(requestOverlay)
        composeRule.onAllNodesWithText("Ya lo activé, verificar de nuevo")[2].performScrollTo().performClick()
        assertTrue(recheckOverlay)
        composeRule.onNodeWithText("Activar protección").performScrollTo().performClick()
        assertTrue(requestDeviceAdmin)
    }

    @Test
    fun allGrantedShowsNoButtonsAndFourConfigured() {
        set(permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true))
        composeRule.onNodeWithText("Abrir Ajustes").assertDoesNotExist()
        composeRule.onNodeWithText("Permitir", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Activar protección").assertDoesNotExist()
        composeRule.onAllNodesWithText("Configurado").assertCountEquals(4)
    }

    @Test
    fun noPrecisaText() {
        set()
        composeRule.onNodeWithText("No se usa la ubicación precisa.", substring = true).performScrollTo().assertExists()
        composeRule.onAllNodes(hasText("precisa", substring = true)).assertCountEquals(1)
    }

    @Test
    fun backCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performClick()
        assertTrue(back)
    }
}
