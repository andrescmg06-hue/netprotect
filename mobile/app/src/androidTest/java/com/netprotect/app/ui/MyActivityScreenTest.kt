package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.tutor.activity.ActivityDay
import com.netprotect.app.feature.tutor.activity.ActivityRow
import com.netprotect.app.feature.tutor.activity.MyActivityScreen
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpTone
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MyActivityScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val twoDays = listOf(
        ActivityDay(
            date = LocalDate.of(2026, 9, 29),
            title = "Hoy",
            subtitle = "29 de septiembre de 2026",
            rows = listOf(
                ActivityRow("r1", NpIcons.MapPin, NpTone.Danger, "Ubicación consultada", "Tablet de Sofía", "3:42 p. m."),
                ActivityRow("r2", NpIcons.KeyRound, NpTone.Neutral, "Renovación de sesión", null, "11:30 a. m."),
            ),
        ),
        ActivityDay(
            date = LocalDate.of(2026, 9, 28),
            title = "Ayer",
            subtitle = "28 de septiembre de 2026",
            rows = listOf(
                ActivityRow("r3", NpIcons.Bell, NpTone.Warning, "Alerta silenciada", "Alerta", "3:10 p. m."),
            ),
        ),
    )

    private fun set(
        activity: LoadState<List<ActivityDay>> = LoadState.Loaded(twoDays),
        hasMore: Boolean = false,
        loadingMore: Boolean = false,
        moreError: String? = null,
        onLoadMore: () -> Unit = {},
        onRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                MyActivityScreen(
                    activity = activity,
                    hasMore = hasMore,
                    loadingMore = loadingMore,
                    moreError = moreError,
                    onLoadMore = onLoadMore,
                    onRefresh = onRefresh,
                )
            }
        }
    }

    @Test
    fun showsTwoDaysAndRows() {
        set()
        composeRule.onNodeWithText("Hoy").performScrollTo().assertExists()
        composeRule.onNodeWithText("Ubicación consultada").performScrollTo().assertExists()
        composeRule.onNodeWithText("Tablet de Sofía").performScrollTo().assertExists()
    }

    @Test
    fun nullSubtitleLeavesNoNullText() {
        set()
        composeRule.onNodeWithText("null").assertDoesNotExist()
    }

    @Test
    fun loadMoreCallsOnLoadMore() {
        var loaded = false
        set(hasMore = true, onLoadMore = { loaded = true })
        composeRule.onNodeWithText("Cargar más").performScrollTo().performClick()
        assertTrue(loaded)
    }

    @Test
    fun noHasMoreHidesLoadMore() {
        set(hasMore = false)
        composeRule.onNodeWithText("Cargar más").assertDoesNotExist()
    }

    @Test
    fun loadingMoreDisablesLoadMore() {
        set(hasMore = true, loadingMore = true)
        composeRule.onNodeWithText("Cargar más").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun moreErrorShowsMessage() {
        set(moreError = "Sin conexión con el servidor.")
        composeRule.onNodeWithText("Sin conexión con el servidor.").performScrollTo().assertExists()
    }

    @Test
    fun emptyShowsSinAcciones() {
        set(activity = LoadState.Loaded(emptyList()))
        composeRule.onNodeWithText("Sin acciones registradas todavía.").assertExists()
    }

    @Test
    fun errorRetryCallsOnRefresh() {
        var refreshed = false
        set(activity = LoadState.Failed("Sin conexión con el servidor."), onRefresh = { refreshed = true })
        composeRule.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertTrue(refreshed)
    }

    @Test
    fun noExportOrDeleteActions() {
        set()
        composeRule.onNodeWithText("Exportar").assertDoesNotExist()
        composeRule.onNodeWithText("Borrar").assertDoesNotExist()
        composeRule.onNodeWithText("Eliminar").assertDoesNotExist()
        composeRule.onNodeWithText("Editar").assertDoesNotExist()
    }

    @Test
    fun showsAppAndWebNotOnlyApp() {
        set()
        composeRule.onNodeWithText("(app y panel web)", substring = true).assertExists()
        composeRule.onNodeWithText("solo las acciones que tú realizas en la app", substring = true).assertDoesNotExist()
    }
}
