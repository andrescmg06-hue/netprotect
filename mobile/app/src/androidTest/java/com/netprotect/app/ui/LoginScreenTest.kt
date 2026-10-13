package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.core.network.UiError
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(service: ServiceStatus, error: String? = null) {
        composeRule.setContent {
            NetProtectTheme {
                LoginScreen(error = error, service = service, onSignIn = {}, onRetryService = {})
            }
        }
    }

    @Test
    fun checkingShowsItsLabelAndNoRetry() {
        set(ServiceStatus.Checking)
        composeRule.onNodeWithText("Comprobando servicio…").assertExists()
        composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun readyShowsItsLabel() {
        set(ServiceStatus.Ready)
        composeRule.onNodeWithText("Servicio listo").assertExists()
        composeRule.onNodeWithText("Reintentar").assertDoesNotExist()
    }

    @Test
    fun unavailableShowsItsLabelAndRetry() {
        set(ServiceStatus.Unavailable)
        composeRule.onNodeWithText("Servicio no disponible").assertExists()
        composeRule.onNodeWithText("Reintentar").assertExists()
    }

    @Test
    fun retryCallsOnRetryServiceExactlyOnce() {
        var count = 0
        composeRule.setContent {
            NetProtectTheme {
                LoginScreen(error = null, service = ServiceStatus.Unavailable, onSignIn = {}, onRetryService = { count++ })
            }
        }
        composeRule.onNodeWithText("Reintentar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertEquals(1, count)
    }

    @Test
    fun signInCallsOnSignIn() {
        var signedIn = false
        composeRule.setContent {
            NetProtectTheme {
                LoginScreen(error = null, service = ServiceStatus.Ready, onSignIn = { signedIn = true }, onRetryService = {})
            }
        }
        composeRule.onNodeWithText("Iniciar sesión con Google").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(signedIn)
    }

    @Test
    fun errorIsShownWhenPresent() {
        set(ServiceStatus.Ready, error = "No se pudo iniciar sesión")
        composeRule.onNodeWithText("No se pudo iniciar sesión").assertExists()
    }

    @Test
    fun errorIsAbsentWhenNull() {
        set(ServiceStatus.Ready, error = null)
        composeRule.onNodeWithText("No se pudo iniciar sesión").assertDoesNotExist()
    }

    @Test
    fun serviceDownShowsItsCauseAndRetryWithoutTheStatusLine() {
        set(ServiceStatus.Unavailable, error = UiError.ServiceUnavailable.message)
        composeRule.onNodeWithText(UiError.ServiceUnavailable.message).assertExists()
        composeRule.onNodeWithText("Servicio no disponible").assertDoesNotExist()
        composeRule.onNodeWithText("Reintentar").assertExists()
        composeRule.onNodeWithText("Inicia sesión").assertExists()
    }

    @Test
    fun noInternetShowsItsOwnText() {
        set(ServiceStatus.Unavailable, error = UiError.NoInternet.message)
        composeRule.onNodeWithText(UiError.NoInternet.message).assertExists()
        composeRule.onNodeWithText(UiError.ServiceUnavailable.message).assertDoesNotExist()
    }

    @Test
    fun anExpiredSessionShowsVuelveAEntrar() {
        set(ServiceStatus.Ready, error = UiError.SessionExpired.message)
        composeRule.onNodeWithText("Vuelve a entrar").assertExists()
        composeRule.onNodeWithText("Tu sesión caducó.").assertExists()
        composeRule.onNodeWithText("Inicia sesión").assertDoesNotExist()
        composeRule.onNodeWithText("Servicio listo").assertExists()
    }

    @Test
    fun withoutErrorTheTitleIsIniciaSesion() {
        set(ServiceStatus.Ready)
        composeRule.onNodeWithText("Inicia sesión").assertExists()
        composeRule.onNodeWithText("Vuelve a entrar").assertDoesNotExist()
        composeRule.onNodeWithText("Solo puedes iniciar sesión con Google.").performScrollTo().assertExists()
    }

    @Test
    fun noInfrastructureJargonIsShown() {
        set(ServiceStatus.Unavailable)
        composeRule.onAllNodes(hasText("Redis", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("base de datos", substring = true)).assertCountEquals(0)
        composeRule.onAllNodes(hasText("HTTP", substring = true)).assertCountEquals(0)
    }
}
