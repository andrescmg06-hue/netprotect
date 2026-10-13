package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.SIGN_OUT_CONFIRM_TAG
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LinkDeviceScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        code: String = "",
        linking: Boolean = false,
        error: String? = null,
        rechecking: Boolean = false,
        onLink: () -> Unit = {},
        onCheckLink: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                LinkDeviceScreen(
                    code = code,
                    onCodeChange = {},
                    linking = linking,
                    error = error,
                    rechecking = rechecking,
                    onLink = onLink,
                    onCheckLink = onCheckLink,
                    onSignOut = onSignOut,
                )
            }
        }
    }

    @Test
    fun fiveDigitsDisablesLink() {
        set(code = "12345")
        composeRule.onNodeWithText("Vincular dispositivo").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun sixDigitsEnablesLinkAndCallsOnLink() {
        var linked = false
        set(code = "123456", onLink = { linked = true })
        composeRule.onNodeWithText("Vincular dispositivo").performScrollTo().assertIsEnabled().performClick()
        assertTrue(linked)
    }

    @Test
    fun linkingShowsVinculandoDisabled() {
        set(code = "123456", linking = true)
        composeRule.onNodeWithText("Vinculando…").performScrollTo().assertIsNotEnabled()
        composeRule.onNodeWithText("No cierres la app.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Confirmando el código con tu tutor").performScrollTo().assertExists()
        composeRule.onNodeWithText("Comprobar vínculo").assertDoesNotExist()
    }

    @Test
    fun waitingShowsThePasteHintAndItsStatus() {
        set()
        composeRule.onNodeWithText("Puedes pegar los 6 dígitos de una vez. El botón se activa con los 6 dígitos.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("Esperando el código").performScrollTo().assertExists()
    }

    @Test
    fun errorShowsMessage() {
        val message = "El código no es válido o ya venció. Pídele uno nuevo a tu tutor."
        set(code = "123456", error = message)
        composeRule.onNodeWithText(message).performScrollTo().assertExists()
        composeRule.onNodeWithText("El código no se pudo usar").performScrollTo().assertExists()
    }

    @Test
    fun tooManyAttemptsShowsItsText() {
        val message = "Hiciste demasiados intentos. Espera unos minutos y vuelve a intentarlo."
        set(code = "123456", error = message)
        composeRule.onNodeWithText(message).performScrollTo().assertExists()
    }

    @Test
    fun checkLinkCallsOnCheckLink() {
        var checked = false
        set(onCheckLink = { checked = true })
        composeRule.onNodeWithText("Comprobar vínculo").performScrollTo().performClick()
        assertTrue(checked)
    }

    @Test
    fun signOutAsksFirstAndConfirmCallsOnSignOut() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.onNodeWithText("¿Cerrar sesión?").assertExists()
        assertFalse(signedOut)
        composeRule.onNodeWithTag(SIGN_OUT_CONFIRM_TAG).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(signedOut)
        composeRule.onNodeWithText("¿Cerrar sesión?").assertDoesNotExist()
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
}
