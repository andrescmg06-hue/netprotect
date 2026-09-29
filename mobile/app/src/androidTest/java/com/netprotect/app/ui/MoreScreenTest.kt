package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.tutor.more.MoreScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MoreScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(onSwitchMode: () -> Unit = {}, onSignOut: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                MoreScreen(appVersion = "0.1.0", onSwitchMode = onSwitchMode, onSignOut = onSignOut)
            }
        }
    }

    @Test
    fun showsVersionAndPrivacyText() {
        set()
        composeRule.onNodeWithText("Versión 0.1.0").assertExists()
        composeRule.onNodeWithText(
            "NetProtect solo muestra lo que el dispositivo supervisado comparte con tu cuenta. La ubicación es aproximada, nada se graba y cada acción queda en tu registro de actividad."
        ).assertExists()
    }

    @Test
    fun switchModeCardCallsOnSwitchMode() {
        var switched = false
        set(onSwitchMode = { switched = true })
        composeRule.onNodeWithText("Cambiar de modo").performScrollTo().performClick()
        assertTrue(switched)
    }

    @Test
    fun signOutCardCallsOnSignOut() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        assertTrue(signedOut)
    }
}
