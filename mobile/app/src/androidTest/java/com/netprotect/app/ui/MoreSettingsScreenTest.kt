package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.SIGN_OUT_CONFIRM_TAG
import com.netprotect.app.feature.supervised.settings.MoreSettingsScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 63: «Más ajustes» and its «¿Cerrar sesión?» confirmation (lámina 10). */
class MoreSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(onSignOut: () -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                MoreSettingsScreen(onSignOut = onSignOut, onBack = onBack)
            }
        }
    }

    @Test
    fun showsSignOutAndTheUnlinkNoteWithoutPrivacy() {
        set()
        composeRule.onNodeWithText("Salir de tu cuenta en este teléfono.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Para desvincular este dispositivo, pídeselo a tu tutor. Solo tu tutor puede hacerlo.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Privacidad").assertDoesNotExist()
        composeRule.onNodeWithText("Cambiar de modo").assertDoesNotExist()
    }

    @Test
    fun signOutAsksFirstAndConfirmCallsOnSignOut() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.onNodeWithText("¿Cerrar sesión?").assertExists()
        composeRule.onNodeWithText(
            "Saldrás de tu cuenta en este teléfono. Para volver a entrar tendrás que iniciar sesión otra vez.",
        ).assertExists()
        assertFalse(signedOut)
        composeRule.onNodeWithTag(SIGN_OUT_CONFIRM_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(signedOut)
    }

    @Test
    fun cancellingTheSignOutChangesNothing() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.onNodeWithText("Cancelar").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertFalse(signedOut)
        composeRule.onNodeWithText("¿Cerrar sesión?").assertDoesNotExist()
    }

    @Test
    fun backCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }
}
