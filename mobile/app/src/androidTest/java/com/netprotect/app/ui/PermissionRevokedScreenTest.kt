package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.permissions.PermissionRevokedScreen
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 67: «Se desactivó un permiso» (lámina 13.4); it only calls back, the shell changes the route. */
class PermissionRevokedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        permission: SupervisedPermission = SupervisedPermission.Overlay,
        onActivate: () -> Unit = {},
        onHome: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                PermissionRevokedScreen(permission = permission, onActivate = onActivate, onHome = onHome)
            }
        }
    }

    @Test
    fun overlayShowsTitlePermissionChipAndNoTutorLine() {
        set(SupervisedPermission.Overlay)
        composeRule.onNodeWithText("PERMISO DESACTIVADO").performScrollTo().assertExists()
        composeRule.onNodeWithText("Se desactivó un permiso").performScrollTo().assertExists()
        composeRule.onNodeWithText(
            "«Mostrar sobre otras apps» se quitó en Ajustes de Android. " +
                "Mientras tanto, es posible que NetProtect no pueda mostrar la pantalla de bloqueo.",
        ).performScrollTo().assertExists()
        composeRule.onNodeWithText("Mostrar sobre otras apps").performScrollTo().assertExists()
        composeRule.onNodeWithText("Pendiente").performScrollTo().assertExists()
        composeRule.onNodeWithText("Activa el permiso para que NetProtect vuelva a funcionar.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Tu tutor puede ver que este permiso está desactivado.").assertDoesNotExist()
    }

    @Test
    fun usageAccessSaysTheTutorCanSeeIt() {
        set(SupervisedPermission.UsageAccess)
        composeRule.onNodeWithText("Acceso al uso de apps").performScrollTo().assertExists()
        composeRule.onNodeWithText("Tu tutor puede ver que este permiso está desactivado.").performScrollTo().assertExists()
        composeRule.onNodeWithText("Activa el permiso para que NetProtect vuelva a funcionar.").assertDoesNotExist()
    }

    @Test
    fun activateAgainCallsBack() {
        var activated = false
        set(onActivate = { activated = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Activar de nuevo").assertIsDisplayed().performClick()
        assertTrue(activated)
    }

    @Test
    fun backToHomeCallsBack() {
        var home = false
        set(onHome = { home = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Volver al inicio").assertIsDisplayed().performClick()
        assertTrue(home)
    }

    @Test
    fun arrowGoesHome() {
        var home = false
        set(onHome = { home = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(home)
    }
}
