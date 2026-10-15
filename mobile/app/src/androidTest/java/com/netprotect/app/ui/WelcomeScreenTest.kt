package com.netprotect.app.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.privacy.PRIVACY_NO_RECORDING
import com.netprotect.app.feature.supervised.privacy.privacySections
import com.netprotect.app.feature.supervised.welcome.WelcomeScreen
import com.netprotect.app.feature.supervised.welcome.WelcomeStep
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 67: the welcome after a new link (láminas 13.1 and 13.2); every claim of step 2 comes from Privacidad. */
class WelcomeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        step: WelcomeStep,
        pendingPermissions: Int = 4,
        onNext: () -> Unit = {},
        onBack: () -> Unit = {},
        onFinish: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                WelcomeScreen(
                    step = step,
                    pendingPermissions = pendingPermissions,
                    onNext = onNext,
                    onBack = onBack,
                    onFinish = onFinish,
                )
            }
        }
    }

    @Test
    fun stepOneShowsTheSlideTexts() {
        set(WelcomeStep.Linked)
        composeRule.onNodeWithText("VINCULACIÓN COMPLETADA").performScrollTo().assertExists()
        composeRule.onNodeWithText("Este teléfono ya está vinculado").performScrollTo().assertExists()
        composeRule.onNodeWithText("Antes de seguir, te explicamos en dos pasos qué hace NetProtect aquí.")
            .performScrollTo().assertExists()
        composeRule.onNodeWithContentDescription("Paso 1 de 2").assertExists()
    }

    @Test
    fun stepOneButtonCallsOnNext() {
        var next = false
        set(WelcomeStep.Linked, onNext = { next = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Ver qué hace NetProtect").assertIsDisplayed().performClick()
        assertTrue(next)
    }

    @Test
    fun stepTwoShowsEveryPrivacyItem() {
        set(WelcomeStep.TutorCanSee)
        composeRule.onNodeWithText("PASO 2 DE 2").performScrollTo().assertExists()
        composeRule.onNodeWithText("Esto es lo que tu tutor puede ver").performScrollTo().assertExists()
        composeRule.onNodeWithText("Y TÚ DECIDES").performScrollTo().assertExists()
        privacySections.flatMap { it.items }.forEach { item ->
            composeRule.onNodeWithText(item.title, substring = true).performScrollTo().assertExists()
            composeRule.onNodeWithText(item.body, substring = true).performScrollTo().assertExists()
        }
        composeRule.onNodeWithText(PRIVACY_NO_RECORDING).performScrollTo().assertExists()
    }

    @Test
    fun stepTwoFinishCallsOnFinish() {
        var finished = false
        set(WelcomeStep.TutorCanSee, pendingPermissions = 2, onFinish = { finished = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Entendido, configurar permisos").assertIsDisplayed().performClick()
        assertTrue(finished)
    }

    @Test
    fun stepTwoSaysOnlyEntendidoWhenNothingIsPending() {
        set(WelcomeStep.TutorCanSee, pendingPermissions = 0)
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Entendido").assertIsDisplayed()
        composeRule.onAllNodes(hasText("configurar permisos", substring = true)).assertCountEquals(0)
    }

    @Test
    fun stepTwoBackArrowCallsOnBack() {
        var back = false
        set(WelcomeStep.TutorCanSee, onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun stepOneHasNoFalseClaimNoRetentionNoDesignNote() {
        set(WelcomeStep.Linked)
        assertNoBannedWords()
    }

    @Test
    fun stepTwoHasNoFalseClaimNoRetentionNoDesignNote() {
        set(WelcomeStep.TutorCanSee)
        assertNoBannedWords()
    }

    private fun assertNoBannedWords() {
        listOf("ni controla", "controla", "retención", "conserva", "días", "meses", "esté abierta", "pendiente de validar")
            .forEach { word -> composeRule.onAllNodes(hasText(word, substring = true, ignoreCase = true)).assertCountEquals(0) }
    }
}
