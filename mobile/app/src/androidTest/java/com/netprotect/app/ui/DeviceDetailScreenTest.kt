package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.DeviceSection
import com.netprotect.app.feature.tutor.device.DeviceDetailScreen
import com.netprotect.app.feature.tutor.device.RenameUi
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DeviceDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-29T15:00:00Z")

    private val tablet = DeviceSummary(
        id = "dev-tablet",
        name = "Tablet",
        platform = "ANDROID",
        status = "ONLINE",
        lastSeenAt = now.minusSeconds(720).toString(),
        timezone = "America/Bogota",
        osVersion = "13",
        appVersion = "0.1.0",
    )

    private fun set(
        device: LoadState<DeviceSummary> = LoadState.Loaded(tablet),
        rename: RenameUi? = null,
        onConfirmUnlink: () -> Unit = {},
        onBack: () -> Unit = {},
        onOpenSection: (DeviceSection) -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                DeviceDetailScreen(
                    device = device,
                    now = now,
                    rename = rename,
                    unlinking = false,
                    unlinkError = null,
                    onBack = onBack,
                    onRefresh = {},
                    onStartRename = {},
                    onEditRename = {},
                    onSaveRename = {},
                    onCancelRename = {},
                    onConfirmUnlink = onConfirmUnlink,
                    onOpenSection = onOpenSection,
                )
            }
        }
    }

    @Test
    fun unlinkOpensDialogAndCancelDoesNotCallConfirm() {
        var unlinked = false
        set(onConfirmUnlink = { unlinked = true })
        composeRule.onNodeWithText("Desvincular dispositivo").performScrollTo().performClick()
        composeRule.onNodeWithText("¿Desvincular Tablet?").assertExists()
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()
        assertFalse(unlinked)
    }

    @Test
    fun unlinkConfirmCallsExactlyOnce() {
        var count = 0
        set(onConfirmUnlink = { count++ })
        composeRule.onNodeWithText("Desvincular dispositivo").performScrollTo().performClick()
        composeRule.onNodeWithText("Desvincular").performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    @Test
    fun blankRenameDisablesGuardar() {
        set(rename = RenameUi(text = "   "))
        composeRule.onNodeWithText("Guardar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun failedNotFoundShowsMessageAndVolverCallsOnBack() {
        var back = false
        set(
            device = LoadState.Failed("Este dispositivo ya no existe o no tienes acceso.", notFound = true),
            onBack = { back = true },
        )
        composeRule.onNodeWithText("Este dispositivo ya no existe o no tienes acceso.").assertExists()
        composeRule.onNodeWithText("Volver a dispositivos").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun tappingAlertasCallsOnOpenSection() {
        var section: DeviceSection? = null
        set(onOpenSection = { section = it })
        composeRule.onNodeWithText("Alertas").performScrollTo().performClick()
        assertEquals(DeviceSection.Alerts, section)
    }
}
