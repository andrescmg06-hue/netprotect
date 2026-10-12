package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.ui.theme.NetProtectTheme
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
    fun linkingDisablesLink() {
        set(code = "123456", linking = true)
        composeRule.onNodeWithText("Vincular dispositivo").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun errorShowsMessage() {
        set(code = "123456", error = "El código no es válido o ya venció.")
        composeRule.onNodeWithText("El código no es válido o ya venció.").performScrollTo().assertExists()
    }

    @Test
    fun checkLinkCallsOnCheckLink() {
        var checked = false
        set(onCheckLink = { checked = true })
        composeRule.onNodeWithText("Comprobar vínculo").performScrollTo().performClick()
        assertTrue(checked)
    }

    @Test
    fun signOutCallsOnSignOut() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        assertTrue(signedOut)
    }
}
