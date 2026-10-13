package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScreenShareConsentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val requestedAt = Instant.parse("2026-10-13T15:00:00Z")

    private fun set(
        onContinue: () -> Unit = {},
        onDecline: () -> Unit = {},
        requestedAt: Instant? = this.requestedAt,
        now: Instant = this.requestedAt,
        onBack: (() -> Unit)? = null,
    ) {
        composeRule.setContent {
            NetProtectTheme {
                ScreenShareConsentScreen(
                    onContinue = onContinue,
                    onDecline = onDecline,
                    requestedAt = requestedAt,
                    now = now,
                    onBack = onBack,
                )
            }
        }
    }

    // «Continuar», «Ahora no» and the hint sit in the fixed footer, outside the scrolling column: they are
    // always on screen, and performScrollTo() would fail there (no scrollable parent). They are asserted
    // displayed before each click instead.

    private fun tapAuthorize() {
        composeRule.onNodeWithText("Autorizo a mi tutor a ver la pantalla de este dispositivo ahora")
            .performScrollTo().performClick()
    }

    @Test
    fun continueStartsDisabledWithItsHint() {
        set()
        composeRule.onNodeWithText("Marca la casilla para continuar.").assertIsDisplayed()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test
    fun tappingCheckboxTextEnablesAndCallsOnContinue() {
        var continued = false
        set(onContinue = { continued = true })
        tapAuthorize()
        composeRule.onNodeWithText("Marca la casilla para continuar.").assertDoesNotExist()
        composeRule.onNodeWithText("Continuar").assertIsDisplayed().assertIsEnabled().performClick()
        assertTrue(continued)
    }

    @Test
    fun continueWhileDisabledDoesNothing() {
        var continued = false
        set(onContinue = { continued = true })
        composeRule.onNodeWithText("Continuar").assertIsDisplayed().performClick()
        assertFalse(continued)
    }

    @Test
    fun declineCallsOnDeclineWithoutContinuing() {
        var declined = false
        var continued = false
        set(onDecline = { declined = true }, onContinue = { continued = true })
        composeRule.onNodeWithText("Ahora no").assertIsDisplayed().performClick()
        assertTrue(declined)
        assertFalse(continued)
    }

    @Test
    fun showsTheThreeStepsAndTheRequest() {
        set()
        composeRule.onNodeWithText("1 · LEER").performScrollTo().assertExists()
        composeRule.onNodeWithText("2 · AUTORIZAR").performScrollTo().assertExists()
        composeRule.onNodeWithText("3 · ANDROID").performScrollTo().assertExists()
        composeRule.onNodeWithText("SOLICITUD DE TU TUTOR").performScrollTo().assertExists()
        composeRule.onNodeWithText("Quiere ver tu pantalla").performScrollTo().assertExists()
    }

    @Test
    fun countdownStartsAtTwoMinutes() {
        set(now = requestedAt)
        composeRule.onNodeWithText("Esta solicitud caduca en 2 min").performScrollTo().assertExists()
    }

    @Test
    fun countdownFollowsNow() {
        set(now = requestedAt.plusSeconds(12))
        composeRule.onNodeWithText("Esta solicitud caduca en 1 min 48 s").performScrollTo().assertExists()
    }

    @Test
    fun noCountdownWithoutARequest() {
        set(requestedAt = null)
        composeRule.onNodeWithText("Esta solicitud caduca", substring = true).assertDoesNotExist()
        // The rest of the screen is still there.
        composeRule.onNodeWithText("Quiere ver tu pantalla").performScrollTo().assertExists()
    }

    @Test
    fun backArrowCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun noBackArrowWithoutCallback() {
        set(onBack = null)
        composeRule.onNodeWithContentDescription("Atrás").assertDoesNotExist()
    }

    @Test
    fun noRevokeOrSettingsText() {
        set()
        composeRule.onNodeWithText("ajustes", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("revocar", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("redirigir", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("desde la app NetProtect", substring = true).assertDoesNotExist()
    }

    @Test
    fun saysItDoesNotRecordAndNeverThatItDoesNotControl() {
        set()
        composeRule.onNodeWithText("NetProtect no graba ni guarda lo que se ve en tu pantalla.")
            .performScrollTo().assertExists()
        // NetProtect does block apps: no text may claim it doesn't control the device (PLAN §2.1).
        composeRule.onNodeWithText("controla", substring = true, ignoreCase = true).assertDoesNotExist()
    }
}
