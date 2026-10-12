package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.BlockReasonItem
import com.netprotect.app.feature.tutor.sections.ComplianceRow
import com.netprotect.app.feature.tutor.sections.NO_USAGE_IN_PERIOD
import com.netprotect.app.feature.tutor.sections.StatsPeriod
import com.netprotect.app.feature.tutor.sections.StatisticsView
import com.netprotect.app.feature.tutor.sections.TopAppRow
import com.netprotect.app.feature.tutor.statistics.StatisticsScreen
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class StatisticsScreenTest {

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

    private val view = StatisticsView(
        topApps = listOf(TopAppRow("com.google.android.youtube", "YouTube", "3 h 24 min", 1f)),
        blocks = listOf(BlockReasonItem("BLOCK", "Bloquear", NpIcons.Ban, NpTone.Danger, 12)),
        compliance = listOf(
            ComplianceRow("YouTube", "Límite: 60 min/día", 0.71f, "5 de 7 días"),
            ComplianceRow("Instagram", "Límite: 30 min/día", null, NO_USAGE_IN_PERIOD),
        ),
    )

    private fun set(
        period: StatsPeriod = StatsPeriod.Week,
        statistics: LoadState<StatisticsView> = LoadState.Loaded(view),
        onSelectPeriod: (StatsPeriod) -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                StatisticsScreen(
                    device = tablet,
                    period = period,
                    statistics = statistics,
                    now = now,
                    onSelectPeriod = onSelectPeriod,
                    onRefresh = {},
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun showsComplianceSubtitleAndNoPercentage() {
        set()
        composeRule.onNodeWithText("Días dentro del límite en este periodo.").performScrollTo().assertExists()
        composeRule.onAllNodes(hasText("Porcentaje", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("%", substring = true)).assertCountEquals(0)
    }

    @Test
    fun showsFiveOfSevenDays() {
        set()
        composeRule.onNodeWithText("5 de 7 días").performScrollTo().assertExists()
    }

    @Test
    fun tappingThirtyDaysCallsOnSelectPeriod() {
        var selected: StatsPeriod? = null
        set(period = StatsPeriod.Week, onSelectPeriod = { selected = it })
        composeRule.onNodeWithText("30 días").performScrollTo().performClick()
        assertEquals(StatsPeriod.Month, selected)
    }

    @Test
    fun emptyShowsAllEmptyMessages() {
        set(statistics = LoadState.Loaded(StatisticsView(emptyList(), emptyList(), emptyList())))
        composeRule.onNodeWithText("Sin datos de uso.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Ninguno en este periodo.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sin reglas de límite diario.").performScrollTo().assertExists()
    }
}
