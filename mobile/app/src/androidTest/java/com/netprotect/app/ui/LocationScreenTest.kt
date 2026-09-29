package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.feature.tutor.location.LocationScreen
import com.netprotect.app.feature.tutor.sections.LocationView
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LocationScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-09-29T15:00:00Z")

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

    private val report = LocationReport(
        latitude = 4.15123,
        longitude = -73.63456,
        accuracyMeters = 200.0,
        capturedAt = now.minusSeconds(720).toString(),
        receivedAt = now.minusSeconds(600).toString(),
    )

    private fun set(
        location: LoadState<LocationView> = LoadState.Loaded(LocationView(report, null)),
        onOpenMap: () -> Boolean = { true },
    ) {
        composeRule.setContent {
            NetProtectTheme {
                LocationScreen(
                    device = tablet,
                    location = location,
                    now = now,
                    onRefresh = {},
                    onOpenMap = onOpenMap,
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun insideGeofenceShowsDentroDe() {
        set(location = LoadState.Loaded(LocationView(report, "Casa")))
        composeRule.onNodeWithText("Dentro de «Casa»").assertExists()
    }

    @Test
    fun noGeofenceShowsNoDentroDe() {
        set(location = LoadState.Loaded(LocationView(report, null)))
        composeRule.onAllNodes(hasText("Dentro de", substring = true)).assertCountEquals(0)
    }

    @Test
    fun noCoordinatesAreShown() {
        set()
        composeRule.onAllNodes(hasText("4.15", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("-73.6", substring = true)).assertCountEquals(0)
    }

    @Test
    fun abrirEnMapaCallsOnOpenMap() {
        var opened = false
        set(onOpenMap = { opened = true; true })
        composeRule.onNodeWithText("Abrir en mapa").performScrollTo().performClick()
        assertTrue(opened)
    }

    @Test
    fun mapMissingShowsNoHayAplicacionDeMapas() {
        set(onOpenMap = { false })
        composeRule.onNodeWithText("Abrir en mapa").performScrollTo().performClick()
        composeRule.onNodeWithText("No hay una aplicación de mapas en este teléfono.").assertExists()
    }

    @Test
    fun nullReportShowsSinUbicacionTodavia() {
        set(location = LoadState.Loaded(LocationView(null, null)))
        composeRule.onNodeWithText("Este dispositivo todavía no ha reportado su ubicación.").performScrollTo().assertExists()
    }
}
