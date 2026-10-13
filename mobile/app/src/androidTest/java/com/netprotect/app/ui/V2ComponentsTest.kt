package com.netprotect.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpHeroPanel
import com.netprotect.app.ui.components.NpRail
import com.netprotect.app.ui.components.NpRailStep
import com.netprotect.app.ui.components.NpSeal
import com.netprotect.app.ui.components.RailStatus
import com.netprotect.app.ui.components.permissionsCaption
import com.netprotect.app.ui.components.sealStatesFor
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class V2ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun state(value: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)

    @Test
    fun railExposesEachStepTextAndState() {
        composeRule.setContent {
            NetProtectTheme {
                NpRail(
                    steps = listOf(
                        NpRailStep("Mostrar sobre otras apps", "Necesario para mostrar la pantalla de bloqueo cuando corresponda.", RailStatus.Next),
                        NpRailStep("Protección contra desinstalación", "Avisa a tu tutor si alguien intenta desinstalar NetProtect.", RailStatus.Pending),
                        NpRailStep("Acceso al uso de apps", "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.", RailStatus.Done),
                        NpRailStep("Ubicación aproximada", "Permite reportar la última ubicación conocida. No se usa la ubicación precisa.", RailStatus.Done),
                    ),
                )
            }
        }
        composeRule.onNodeWithText("Mostrar sobre otras apps").assert(state("Siguiente paso"))
        composeRule.onNodeWithText("Protección contra desinstalación").assert(state("Permiso pendiente"))
        composeRule.onNodeWithText("Acceso al uso de apps").assert(state("Configurado"))
        composeRule.onNodeWithText("Pendiente · siguiente").assertExists()
        composeRule.onNodeWithText("Pendiente").assertExists()
        composeRule.onAllNodesWithText("Configurado").assertCountEquals(2)
        composeRule.onNodeWithText("Avisa a tu tutor si alguien intenta desinstalar NetProtect.").assertExists()
    }

    @Test
    fun railActionInsideAStepIsClickable() {
        var clicked = false
        composeRule.setContent {
            NetProtectTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    NpRail(
                        steps = listOf(
                            NpRailStep(
                                "Mostrar sobre otras apps",
                                "Necesario para mostrar la pantalla de bloqueo cuando corresponda.",
                                RailStatus.Next,
                                action = {
                                    NpButton(
                                        "Activar en Ajustes", { clicked = true }, Modifier.fillMaxWidth(),
                                        trailingIcon = NpIcons.ExternalLink,
                                    )
                                },
                            ),
                            NpRailStep("Acceso al uso de apps", "Permite saber cuánto tiempo se usan las apps.", RailStatus.Done),
                        ),
                    )
                }
            }
        }
        composeRule.onNodeWithText("Activar en Ajustes").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun heroShowsHeadlineCaptionSummaryAndAction() {
        var clicked = false
        composeRule.setContent {
            NetProtectTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    NpHeroPanel(
                        eyebrow = "Dispositivo supervisado",
                        headline = "Falta un paso",
                        sealStates = sealStatesFor(readyCount = 2),
                        sealCaption = permissionsCaption(2),
                        summary = "Faltan 2 permisos para que NetProtect funcione bien en este teléfono.",
                        action = {
                            NpButton("Revisar permisos", { clicked = true }, Modifier.fillMaxWidth(), trailingIcon = NpIcons.ArrowRight)
                        },
                    )
                }
            }
        }
        composeRule.onNodeWithText("DISPOSITIVO SUPERVISADO").assertIsDisplayed()
        composeRule.onNodeWithText("Falta un paso").assertIsDisplayed()
        composeRule.onNodeWithText("2 DE 4 PERMISOS").assertIsDisplayed()
        composeRule.onNodeWithText("Faltan 2 permisos para que NetProtect funcione bien en este teléfono.").assertIsDisplayed()
        composeRule.onNodeWithText("Revisar permisos").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(clicked)
    }

    @Test
    fun primaryButtonIsAtLeast60dpAndSmallAtLeast48dp() {
        composeRule.setContent {
            NetProtectTheme {
                Column {
                    NpButton("Principal", {})
                    NpButton("Pequeño", {}, small = true)
                    NpButton("Texto", {}, variant = NpButtonVariant.Text)
                }
            }
        }
        composeRule.onNodeWithText("Principal").assertHeightIsAtLeast(60.dp)
        composeRule.onNodeWithText("Pequeño").assertHeightIsAtLeast(48.dp)
        composeRule.onNodeWithText("Texto").assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun loadingButtonIsDisabledAndIgnoresClicks() {
        var clicked = false
        composeRule.setContent {
            NetProtectTheme {
                NpButton("Revisar permisos", { clicked = true }, loading = true, trailingIcon = NpIcons.ArrowRight)
            }
        }
        composeRule.onNodeWithText("Revisar permisos").assertIsNotEnabled().performTouchInput { click() }
        composeRule.waitForIdle()
        assertFalse(clicked)
    }

    @Test
    fun sealRendersWithAndWithoutReducedMotion() {
        composeRule.setContent {
            NetProtectTheme {
                Column {
                    NpSeal(states = sealStatesFor(readyCount = 4), contentDescription = "Sello normal", reducedMotion = false)
                    NpSeal(states = sealStatesFor(readyCount = 2), contentDescription = "Sello sin animaciones", reducedMotion = true)
                    NpSeal(states = sealStatesFor(readyCount = 4, reporting = false))
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(1_200)
        composeRule.onNodeWithContentDescription("Sello normal").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Sello sin animaciones").assertIsDisplayed()
    }
}
