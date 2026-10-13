package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.privacy.PrivacyScreen
import com.netprotect.app.feature.supervised.privacy.privacySections
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 66: Privacidad (lámina 10.5), static text whose every claim comes from the code. */
class PrivacyScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(onBack: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                PrivacyScreen(onBack = onBack)
            }
        }
    }

    @Test
    fun showsTheHeaderAndBothSections() {
        set()
        composeRule.onNodeWithText("Privacidad").performScrollTo().assertExists()
        composeRule.onNodeWithText("Qué hace NetProtect en este teléfono.").performScrollTo().assertExists()
        composeRule.onNodeWithText("QUÉ HACE").performScrollTo().assertExists()
        composeRule.onNodeWithText("VISTA DE PANTALLA").performScrollTo().assertExists()
    }

    @Test
    fun everyItemIsOnScreen() {
        set()
        privacySections.flatMap { it.items }.forEach { item ->
            composeRule.onNodeWithText(item.title, substring = true).performScrollTo().assertExists()
            composeRule.onNodeWithText(item.body, substring = true).performScrollTo().assertExists()
        }
    }

    @Test
    fun saysWhatTheSlideLeftOutAndTheRealCadences() {
        set()
        composeRule.onNodeWithText("Cada minuto mientras la app esté abierta.", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("cada 5 minutos mientras la app esté abierta", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("la versión de Android y de NetProtect, la zona horaria", substring = true)
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("aproximadamente cada 15 minutos, no en tiempo real", substring = true)
            .performScrollTo().assertExists()
        composeRule.onNodeWithText("puedes detenerla en cualquier momento", substring = true).performScrollTo().assertExists()
        composeRule.onNodeWithText("NetProtect no graba ni guarda lo que se ve en tu pantalla.").performScrollTo().assertExists()
    }

    @Test
    fun noDesignNoteNoFalseClaimNoRetention() {
        set()
        listOf("pendiente de validar", "ni controla", "controla", "impide", "conserva", "retención", "días", "meses")
            .forEach { word -> composeRule.onAllNodes(hasText(word, substring = true, ignoreCase = true)).assertCountEquals(0) }
    }

    @Test
    fun backCallsOnBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }
}
