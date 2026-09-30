package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.history.HistoryScreen
import com.netprotect.app.feature.tutor.sections.HistoryDay
import com.netprotect.app.feature.tutor.sections.HistoryKind
import com.netprotect.app.feature.tutor.sections.HistoryRow
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HistoryScreenTest {

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

    private val twoDays = listOf(
        HistoryDay(
            date = LocalDate.of(2026, 9, 29),
            title = "Hoy",
            subtitle = "29 de septiembre de 2026",
            rows = listOf(
                HistoryRow("h1", HistoryKind.Block, "Bloqueo de app · YouTube", "Motivo: Límite diario", "DAILY_LIMIT", "3:42 p. m."),
                HistoryRow("h2", HistoryKind.Enter, "Entró · Casa", null, null, "2:10 p. m."),
            ),
        ),
        HistoryDay(
            date = LocalDate.of(2026, 9, 28),
            title = "Ayer",
            subtitle = "28 de septiembre de 2026",
            rows = listOf(
                HistoryRow("h3", HistoryKind.Exit, "Salió · Colegio", null, null, "8:30 a. m."),
            ),
        ),
    )

    private fun set(history: LoadState<List<HistoryDay>>, onRefresh: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                HistoryScreen(device = tablet, history = history, now = now, onRefresh = onRefresh, onBack = {})
            }
        }
    }

    @Test
    fun showsTwoDaysAndRows() {
        set(LoadState.Loaded(twoDays))
        composeRule.onNodeWithText("Hoy").performScrollTo().assertExists()
        composeRule.onNodeWithText("29 de septiembre de 2026").performScrollTo().assertExists()
        composeRule.onNodeWithText("Bloqueo de app · YouTube").performScrollTo().assertExists()
        composeRule.onNodeWithText("Motivo: Límite diario").performScrollTo().assertExists()
    }

    @Test
    fun emptyShowsSinEventos() {
        set(LoadState.Loaded(emptyList()))
        composeRule.onNodeWithText("Sin eventos en el historial.").performScrollTo().assertExists()
    }

    @Test
    fun errorRetryCallsOnRefresh() {
        var refreshed = false
        set(LoadState.Failed("Sin conexión con el servidor."), onRefresh = { refreshed = true })
        composeRule.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertTrue(refreshed)
    }
}
