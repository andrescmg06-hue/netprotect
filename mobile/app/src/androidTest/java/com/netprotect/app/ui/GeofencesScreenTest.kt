package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.Geofence
import com.netprotect.app.core.network.GeofenceEvent
import com.netprotect.app.feature.tutor.geofences.GeofencesScreen
import com.netprotect.app.feature.tutor.sections.geofencesView
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test

class GeofencesScreenTest {

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

    private fun set(geofences: LoadState<com.netprotect.app.feature.tutor.sections.GeofencesView>) {
        composeRule.setContent {
            NetProtectTheme {
                GeofencesScreen(
                    device = tablet,
                    geofences = geofences,
                    today = today,
                    now = now,
                    onRefresh = {},
                    onBack = {},
                )
            }
        }
    }

    private fun view(geofences: List<Geofence>, events: List<GeofenceEvent>) =
        geofencesView(geofences, events)

    @Test
    fun enterShowsUltimoEventoEntro() {
        set(
            LoadState.Loaded(
                view(
                    listOf(Geofence("g1", "Casa", 4.0, -73.0, 150.0)),
                    listOf(GeofenceEvent("e1", "Casa", "ENTER", now.minusSeconds(3600).toString(), "g1")),
                )
            )
        )
        composeRule.onNodeWithText("Último evento: Entró").assertExists()
    }

    @Test
    fun exitShowsUltimoEventoSalio() {
        set(
            LoadState.Loaded(
                view(
                    listOf(Geofence("g1", "Casa", 4.0, -73.0, 150.0)),
                    listOf(GeofenceEvent("e1", "Casa", "EXIT", now.minusSeconds(3600).toString(), "g1")),
                )
            )
        )
        composeRule.onNodeWithText("Último evento: Salió").assertExists()
    }

    @Test
    fun noEventShowsSinActividad() {
        set(
            LoadState.Loaded(
                view(listOf(Geofence("g1", "Casa", 4.0, -73.0, 150.0)), emptyList())
            )
        )
        composeRule.onNodeWithText("Sin actividad").assertExists()
    }

    @Test
    fun showsRadio() {
        set(
            LoadState.Loaded(
                view(
                    listOf(Geofence("g1", "Casa", 4.0, -73.0, 150.0)),
                    listOf(GeofenceEvent("e1", "Casa", "ENTER", now.minusSeconds(3600).toString(), "g1")),
                )
            )
        )
        composeRule.onNodeWithText("Radio 150 m").assertExists()
    }

    @Test
    fun noCreateEditDeleteText() {
        set(
            LoadState.Loaded(
                view(
                    listOf(Geofence("g1", "Casa", 4.0, -73.0, 150.0)),
                    listOf(GeofenceEvent("e1", "Casa", "ENTER", now.minusSeconds(3600).toString(), "g1")),
                )
            )
        )
        composeRule.onAllNodes(hasText("Crear", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Editar", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("Eliminar", substring = true)).assertCountEquals(0)
    }

    @Test
    fun emptyShowsNoHayGeocercas() {
        set(LoadState.Loaded(view(emptyList(), emptyList())))
        composeRule.onNodeWithText("No hay geocercas configuradas").performScrollTo().assertExists()
    }
}
