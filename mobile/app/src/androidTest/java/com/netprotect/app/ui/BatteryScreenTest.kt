package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.supervised.battery.BATTERY_BODY
import com.netprotect.app.feature.supervised.battery.BATTERY_DONE_TEXT
import com.netprotect.app.feature.supervised.battery.BATTERY_NOTE
import com.netprotect.app.feature.supervised.battery.BatteryScreen
import com.netprotect.app.feature.supervised.battery.batterySteps
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Sprint 67: the battery notice (lámina 13.3); it only calls back, the shell opens Settings. */
class BatteryScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(ignoring: Boolean = false, onOpenSettings: () -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            NetProtectTheme {
                BatteryScreen(ignoring = ignoring, onOpenSettings = onOpenSettings, onBack = onBack)
            }
        }
    }

    @Test
    fun optimizedShowsTitleBodyStepsAndNote() {
        set(ignoring = false)
        composeRule.onNodeWithText("NetProtect puede detenerse en segundo plano").performScrollTo().assertExists()
        composeRule.onNodeWithText(BATTERY_BODY).performScrollTo().assertExists()
        composeRule.onNodeWithText("CÓMO ARREGLARLO").performScrollTo().assertExists()
        batterySteps().forEach { composeRule.onNodeWithText(it).performScrollTo().assertExists() }
        composeRule.onNodeWithText(BATTERY_NOTE).performScrollTo().assertExists()
        composeRule.onNodeWithText("Volver al inicio").assertDoesNotExist()
    }

    @Test
    fun openSettingsCallsBack() {
        var opened = false
        set(onOpenSettings = { opened = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Abrir ajustes de batería").assertIsDisplayed().performClick()
        assertTrue(opened)
    }

    @Test
    fun laterGoesBack() {
        var back = false
        set(onBack = { back = true })
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Más tarde").assertIsDisplayed().performClick()
        assertTrue(back)
    }

    @Test
    fun arrowGoesBack() {
        var back = false
        set(onBack = { back = true })
        composeRule.onNodeWithContentDescription("Atrás").performScrollTo().performClick()
        assertTrue(back)
    }

    @Test
    fun afterComingBackWithItOffItSaysSoAndOffersInicio() {
        var back = false
        set(ignoring = true, onBack = { back = true })
        composeRule.onNodeWithText("Batería revisada").performScrollTo().assertExists()
        composeRule.onNodeWithText(BATTERY_DONE_TEXT).performScrollTo().assertExists()
        composeRule.onNodeWithText("Abrir ajustes de batería").assertDoesNotExist()
        composeRule.onNodeWithText("CÓMO ARREGLARLO").assertDoesNotExist()
        // Fixed footer outside the scroll: it is always on screen, so no performScrollTo.
        composeRule.onNodeWithText("Volver al inicio").assertIsDisplayed().performClick()
        assertTrue(back)
    }
}
