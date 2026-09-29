package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.apps.AppsScreen
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-29T15:00:00Z")
    private val today = LocalDate.of(2026, 9, 29)

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

    private fun app(
        pkg: String,
        label: String,
        uninstalled: Boolean = false,
        seconds: Int? = 3600,
        date: String? = "2026-09-29",
    ) = DeviceApplicationSummary(
        packageName = pkg,
        appLabel = label,
        isSystemApp = false,
        uninstalledAt = if (uninstalled) "2026-09-20T10:00:00Z" else null,
        latestUsageDate = date,
        latestUsageSeconds = seconds,
    )

    private fun set(
        apps: LoadState<List<DeviceApplicationSummary>> = LoadState.Loaded(emptyList()),
        onRefresh: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                AppsScreen(
                    device = tablet,
                    apps = apps,
                    today = today,
                    now = now,
                    onRefresh = onRefresh,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun searchFiltersApps() {
        set(
            apps = LoadState.Loaded(
                listOf(
                    app("com.android.chrome", "Chrome"),
                    app("com.google.android.youtube", "YouTube"),
                    app("com.instagram.android", "Instagram"),
                )
            )
        )
        composeRule.onNode(hasSetTextAction()).performTextInput("YouTube")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Chrome").assertDoesNotExist()
        composeRule.onNodeWithText("Instagram").assertDoesNotExist()
        // "YouTube" aparece dos veces: el valor del campo de búsqueda y la fila de la app.
        composeRule.onAllNodes(hasText("YouTube")).assertCountEquals(2)
    }

    @Test
    fun noMatchShowsNingunaAppCoincide() {
        set(
            apps = LoadState.Loaded(
                listOf(
                    app("com.android.chrome", "Chrome"),
                    app("com.google.android.youtube", "YouTube"),
                )
            )
        )
        composeRule.onNode(hasSetTextAction()).performTextInput("zzz")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ninguna app coincide con «zzz».").assertExists()
    }

    @Test
    fun uninstalledAppShowsDesinstalada() {
        set(
            apps = LoadState.Loaded(
                listOf(app("com.spotify.music", "Spotify", uninstalled = true, seconds = 1200, date = "2026-09-19"))
            )
        )
        composeRule.onNodeWithText("Desinstalada").assertExists()
    }

    @Test
    fun noAppRowIsClickable() {
        set(
            apps = LoadState.Loaded(
                listOf(
                    app("com.android.chrome", "Chrome"),
                    app("com.google.android.youtube", "YouTube"),
                )
            )
        )
        composeRule.onAllNodes(hasClickAction() and hasText("Chrome", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasClickAction() and hasText("YouTube", substring = true)).assertCountEquals(0)
    }

    @Test
    fun actualizarCallsOnRefresh() {
        var refreshed = false
        set(onRefresh = { refreshed = true })
        composeRule.onNodeWithText("Actualizar").performScrollTo().performClick()
        assertTrue(refreshed)
    }

    @Test
    fun emptyShowsSinAppsSincronizadas() {
        set(apps = LoadState.Loaded(emptyList()))
        composeRule.onNodeWithText("Sin apps sincronizadas").performScrollTo().assertExists()
    }
}
