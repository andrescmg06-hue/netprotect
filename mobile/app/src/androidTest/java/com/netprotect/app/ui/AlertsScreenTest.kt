package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.alerts.AlertsScreen
import com.netprotect.app.feature.tutor.sections.AlertFilter
import com.netprotect.app.feature.tutor.sections.AlertItem
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AlertsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-29T20:00:00Z")

    private val tablet = DeviceSummary(
        id = "dev-tablet",
        name = "Tablet de Sofía",
        platform = "ANDROID",
        status = "ONLINE",
        lastSeenAt = now.minusSeconds(720).toString(),
        timezone = "America/Bogota",
        osVersion = "13",
        appVersion = "0.1.0",
    )

    private val unread = AlertItem("a1", "HIGH", "Se bloqueó YouTube", "Hoy, 2:10 p. m.", null, unread = true, silenced = false)
    private val read = AlertItem("a2", "INFO", "Salió de Casa", "Hoy, 9:15 a. m.", null, unread = false, silenced = false)
    private val silenced = AlertItem("a3", "WARNING", "Se alcanzó el límite de tiempo de Instagram", "Ayer, 6:40 p. m.", null, unread = false, silenced = true)

    private fun set(
        alerts: LoadState<List<AlertItem>> = LoadState.Loaded(listOf(unread, read, silenced)),
        filter: AlertFilter = AlertFilter.All,
        busyAlertId: String? = null,
        actionError: String? = null,
        silenceTarget: AlertItem? = null,
        onSelectFilter: (AlertFilter) -> Unit = {},
        onMarkRead: (String) -> Unit = {},
        onAskSilence: (String) -> Unit = {},
        onConfirmSilence: () -> Unit = {},
        onDismissSilence: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                AlertsScreen(
                    device = tablet,
                    alerts = alerts,
                    filter = filter,
                    busyAlertId = busyAlertId,
                    actionError = actionError,
                    silenceTarget = silenceTarget,
                    now = now,
                    onSelectFilter = onSelectFilter,
                    onMarkRead = onMarkRead,
                    onAskSilence = onAskSilence,
                    onConfirmSilence = onConfirmSilence,
                    onDismissSilence = onDismissSilence,
                    onRefresh = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun markReadCallsOnMarkRead() {
        var marked: String? = null
        set(onMarkRead = { marked = it })
        composeRule.onNodeWithText("Marcar como leída").performScrollTo().performClick()
        assertEquals("a1", marked)
    }

    @Test
    fun readAlertHasNoMarkRead() {
        set(alerts = LoadState.Loaded(listOf(read)))
        composeRule.onNodeWithText("Marcar como leída").assertDoesNotExist()
    }

    @Test
    fun silenciarCallsOnAskSilence() {
        var asked: String? = null
        set(onAskSilence = { asked = it })
        composeRule.onAllNodesWithText("Silenciar")[0].performScrollTo().performClick()
        assertEquals("a1", asked)
    }

    @Test
    fun silencedShowsSilenciadaAndNoSilenciarButton() {
        set(alerts = LoadState.Loaded(listOf(silenced)))
        composeRule.onNodeWithText("Silenciada").assertExists()
        composeRule.onNodeWithText("Silenciar").assertDoesNotExist()
    }

    @Test
    fun busyDisablesButtons() {
        set(busyAlertId = "a1")
        composeRule.onNodeWithText("Marcar como leída").performScrollTo().assertIsNotEnabled()
        composeRule.onAllNodesWithText("Silenciar")[0].performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun silenceTargetShowsDialogAndCallbacksWork() {
        var confirmed = false
        var dismissed = false
        val target = AlertItem("a4", "HIGH", "Se bloqueó Instagram", "Hoy, 1:05 p. m.", null, unread = false, silenced = false)
        set(
            alerts = LoadState.Loaded(listOf(target)),
            silenceTarget = target,
            onConfirmSilence = { confirmed = true },
            onDismissSilence = { dismissed = true },
        )
        composeRule.onNodeWithText(
            "No volverás a recibir esta alerta de este dispositivo. Puedes quitar el silencio desde el panel web."
        ).assertExists()
        // El botón de confirmar del diálogo es el último "Silenciar" (el de la tarjeta va antes).
        val nodes = composeRule.onAllNodesWithText("Silenciar")
        nodes[nodes.fetchSemanticsNodes().size - 1].performClick()
        composeRule.waitForIdle()
        assertTrue(confirmed)
        composeRule.onNodeWithText("Cancelar").performClick()
        composeRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun criticalFilterCallsOnSelectFilterAndFilters() {
        var selected: AlertFilter? = null
        set(
            alerts = LoadState.Loaded(listOf(unread, read, silenced)),
            onSelectFilter = { selected = it },
        )
        composeRule.onNodeWithText("Críticas").performScrollTo().performClick()
        assertEquals(AlertFilter.Critical, selected)
    }

    @Test
    fun criticalFilterHidesWarning() {
        set(filter = AlertFilter.Critical)
        composeRule.onNodeWithText("Se alcanzó el límite de tiempo de Instagram").assertDoesNotExist()
    }

    @Test
    fun actionErrorShowsMessage() {
        set(actionError = "Sin conexión con el servidor.")
        composeRule.onNodeWithText("Sin conexión con el servidor.").assertExists()
    }

    @Test
    fun emptyShowsSinAlertas() {
        set(alerts = LoadState.Loaded(emptyList()))
        composeRule.onNodeWithText("Sin alertas para este dispositivo.").performScrollTo().assertExists()
    }

    @Test
    fun noLevelCodesShown() {
        set()
        composeRule.onAllNodes(hasText("CRITICAL", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("HIGH", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("WARNING", substring = true)).assertCountEquals(0)
    }
}
