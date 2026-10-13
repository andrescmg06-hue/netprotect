package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsDisplayed
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
    fun unconfirmedLocationSaysNoLocationYet() {
        set(view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Unconfirmed, false, null))
        composeRule.onNodeWithText("No hay una ubicación reciente confirmada. Llega a tu tutor aproximadamente cada 15 minutos.")
            .performScrollTo().assertExists()
    }

    @Test
    fun activeLocationSaysEveryFifteenMinutes() {
        set()
        composeRule.onNodeWithText("Comparte la ubicación aproximada. Llega a tu tutor aproximadamente cada 15 minutos.")
            .performScrollTo().assertExists()
    }

    @Test
    fun noScreenShareHidesThePanel() {
        set(view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Active, false, null))
        composeRule.onNodeWithText("VISUALIZACIÓN DE PANTALLA").assertDoesNotExist()
        composeRule.onNodeWithText("Detener ahora").assertDoesNotExist()
    }

    @Test
    fun screenShareShowsThePanelAndDetenerAhoraStopsIt() {
        var stopped = false
        set(
            view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Active, true, now.minusSeconds(30)),
            onStopScreenShare = { stopped = true },
        )
        composeRule.onNodeWithText("VISUALIZACIÓN DE PANTALLA").performScrollTo().assertExists()
        composeRule.onNodeWithText("En curso").performScrollTo().assertExists()
        composeRule.onNodeWithText("Tu tutor está viendo tu pantalla ahora.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Detener ahora").performScrollTo().performClick()
        assertTrue(stopped)
    }

    @Test
    fun lastReportIsShown() {
        set()
        composeRule.onNodeWithText("Último reporte: hace 1 min").performScrollTo().assertExists()
        composeRule.onNodeWithText("Cada minuto mientras NetProtect esté en marcha.").performScrollTo().assertExists()
    }

    @Test
    fun nullLastReportShowsTodaviaNo() {
        set(view = ServicesView(ServiceState.Unconfirmed, null, ServiceState.Active, ServiceState.Active, false, null))
        composeRule.onNodeWithText("Último reporte: todavía no").performScrollTo().assertExists()
    }

    @Test
    fun showsPermanentNotificationsRow() {
        set()
        composeRule.onNodeWithText("Notificaciones permanentes").performScrollTo().assertExists()
        composeRule.onNodeWithText("Android muestra un aviso mientras una función está activa.").performScrollTo().assertExists()
    }

    @Test
    fun saysItDoesNotRecordAndNeverThatItDoesNotControl() {
        set(view = ServicesView(ServiceState.Active, now.minusSeconds(60), ServiceState.Active, ServiceState.Active, true, now))
        // In the fixed footer: always on screen, with no scrollable parent for performScrollTo().
        composeRule.onNodeWithText("NetProtect no graba ni guarda lo que se ve en tu pantalla.").assertIsDisplayed()
        // NetProtect does block apps: no text may claim it doesn't control the device (PLAN §2.1).
        composeRule.onNodeWithText("controla", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test
    fun noCerrarSesion() {
        set()
        composeRule.onNodeWithText("Cerrar sesión").assertDoesNotExist()
    }

    @Test
    fun volverAlInicioCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        // In the fixed footer: always on screen, with no scrollable parent for performScrollTo().
        composeRule.onNodeWithText("Volver al inicio").assertIsDisplayed().performClick()
        assertTrue(back)
    }

    @Test
    fun backArrowCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }
}
