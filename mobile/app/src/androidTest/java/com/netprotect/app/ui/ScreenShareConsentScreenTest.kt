package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScreenShareConsentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        onContinue: () -> Unit = {},
        onDecline: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                ScreenShareConsentScreen(onContinue = onContinue, onDecline = onDecline)
            }
        }
    }

    @Test
    fun continueStartsDisabled() {
        set()
        composeRule.onNodeWithText("Continuar").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun tappingCheckboxTextEnablesAndCallsOnContinue() {
        var continued = false
        set(onContinue = { continued = true })
        composeRule.onNodeWithText("Autorizo a mi tutor a ver la pantalla de este dispositivo ahora")
            .performScrollTo().performClick()
        composeRule.onNodeWithText("Continuar").performScrollTo().assertIsEnabled().performClick()
        assertTrue(continued)
    }

    @Test
    fun declineCallsOnDecline() {
        var declined = false
        set(onDecline = { declined = true })
        composeRule.onNodeWithText("Ahora no").performScrollTo().performClick()
        assertTrue(declined)
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
    fun showsNoGrabarNiControlar() {
        set()
        composeRule.onNodeWithText("No se graba ni se controla el dispositivo.", substring = true)
            .performScrollTo().assertExists()
    }
}
