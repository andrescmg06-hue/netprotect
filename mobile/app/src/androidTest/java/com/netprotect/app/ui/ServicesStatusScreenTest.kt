package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.feature.supervised.services.ServicesStatusScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ServicesStatusScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val now = Instant.parse("2026-10-01T15:00:00Z")

    private fun set(
        view: ServicesView = ServicesView(
            report = ServiceState.Active,
            lastReport = now.minusSeconds(60),
            appControl = ServiceState.Active,
            location = ServiceState.Active,
            screenShareActive = false,
            screenShareSince = null,
        ),
        onStopScreenShare: () -> Unit = {},
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                ServicesStatusScreen(view = view, now = now, onStopScreenShare = onStopScreenShare, onBack = onBack)
            }
        }
    }

    @Test
    fun showsActiveInactiveUnconfirmedLabels() {
        set(
            view = ServicesView(
                report = ServiceState.Active,
                lastReport = now.minusSeconds(60),
                appControl = ServiceState.Inactive,
                location = ServiceState.Unconfirmed,
                screenShareActive = false,
                screenShareSince = null,
            ),
        )
        composeRule.onNodeWithText("Activo").performScrollTo().assertExists()
        composeRule.onNodeWithText("Inactivo").performScrollTo().assertExists()
        composeRule.onNodeWithText("Sin confirmar").performScrollTo().assertExists()
    }

    @Test
    fun noScreenShareHidesVisualizationAndDetener() {
        set(view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Active, false, null))
        composeRule.onNodeWithText("Visualización de pantalla").assertDoesNotExist()
        composeRule.onNodeWithText("Detener").assertDoesNotExist()
    }

    @Test
    fun screenShareShowsVisualizationAndDetener() {
        var stopped = false
        set(
            view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Active, true, now.minusSeconds(30)),
            onStopScreenShare = { stopped = true },
        )
        composeRule.onNodeWithText("Visualización de pantalla").performScrollTo().assertExists()
        composeRule.onNodeWithText("Detener").performScrollTo().performClick()
        assertTrue(stopped)
    }

    @Test
    fun nullLastReportShowsTodaviaNo() {
        set(view = ServicesView(ServiceState.Unconfirmed, null, ServiceState.Active, ServiceState.Active, false, null))
        composeRule.onNodeWithText("Último reporte: todavía no").performScrollTo().assertExists()
    }

    @Test
    fun showsThreeNotificationTitles() {
        set()
        composeRule.onNodeWithText("“NetProtect está activo” — control de apps.").performScrollTo().assertExists()
        composeRule.onNodeWithText("“NetProtect comparte la ubicación” — ubicación.").performScrollTo().assertExists()
        composeRule.onNodeWithText("“Tu tutor está viendo esta pantalla” — vista remota.").performScrollTo().assertExists()
    }

    @Test
    fun noCerrarSesion() {
        set()
        composeRule.onNodeWithText("Cerrar sesión").assertDoesNotExist()
    }

    @Test
    fun backButtonsCallOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithText("Volver al inicio").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun backArrowCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performClick()
        assertTrue(back)
    }
}
