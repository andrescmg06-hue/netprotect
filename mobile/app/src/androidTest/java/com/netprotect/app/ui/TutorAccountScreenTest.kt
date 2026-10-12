package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.home.TutorAccountScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TutorAccountScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        loading: Boolean = false,
        error: String? = null,
        onUseAsSupervised: () -> Unit = {},
        onSignOut: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                TutorAccountScreen(
                    loading = loading,
                    error = error,
                    onUseAsSupervised = onUseAsSupervised,
                    onSignOut = onSignOut,
                )
            }
        }
    }

    @Test
    fun showsTitleBodyAndActions() {
        set()
        composeRule.onNodeWithText("Esta app es para el teléfono del menor").performScrollTo().assertExists()
        composeRule.onNodeWithText(
            "Tu cuenta es de tutor. Para ver y configurar los dispositivos que supervisas, " +
                "usa el panel web de NetProtect.",
        ).performScrollTo().assertExists()
        composeRule.onNodeWithText("Usar este teléfono como supervisado").performScrollTo().assertExists()
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().assertExists()
    }

    @Test
    fun primaryButtonCallsOnUseAsSupervised() {
        var used = false
        set(onUseAsSupervised = { used = true })
        composeRule.onNodeWithText("Usar este teléfono como supervisado").performScrollTo().performClick()
        assertTrue(used)
    }

    @Test
    fun signOutCallsOnSignOut() {
        var signedOut = false
        set(onSignOut = { signedOut = true })
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        assertTrue(signedOut)
    }

    @Test
    fun showsErrorMessage() {
        set(error = "Sin conexión con el servidor. Revisa tu conexión e intenta de nuevo.")
        composeRule.onNodeWithText("Sin conexión con el servidor. Revisa tu conexión e intenta de nuevo.")
            .performScrollTo()
            .assertExists()
    }
}
