package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.unlinked.UnlinkedScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 64 (B1): «Este dispositivo ya no está vinculado» (lámina 10.4). */
class UnlinkedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(onRelink: () -> Unit = {}, onSignOut: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                UnlinkedScreen(onRelink = onRelink, onSignOut = onSignOut)
            }
        }
    }

    @Test
    fun showsTheLiteralTextsOfTheSlide() {
        set()
        composeRule.onNodeWithText("VÍNCULO TERMINADO").performScrollTo().assertExists()
        composeRule.onNodeWithText("Este dispositivo ya no está vinculado").performScrollTo().assertExists()
        composeRule.onNodeWithText(
            "Tu tutor terminó el vínculo. Si fue un error, pídele un código nuevo para vincularlo otra vez.",
        ).performScrollTo().assertExists()
    }

    @Test
    fun relinkCallsOnRelinkOnly() {
        var relinked = false
        var signedOut = false
        set(onRelink = { relinked = true }, onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Vincular de nuevo").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(relinked)
        assertFalse(signedOut)
    }

    @Test
    fun signOutCallsOnSignOutWithoutAConfirmation() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(signedOut)
        composeRule.onNodeWithText("¿Cerrar sesión?").assertDoesNotExist()
    }
}
