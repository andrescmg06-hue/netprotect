package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.netprotect.app.ui.components.ActiveShareBanner
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActiveShareBannerTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTextAndDetenerCallsOnStop() {
        var stopped = false
        composeRule.setContent {
            NetProtectTheme {
                ActiveShareBanner(onStop = { stopped = true })
            }
        }
        composeRule.onNodeWithText("Tu tutor está viendo esta pantalla").assertExists()
        composeRule.onNodeWithText("Detener").performClick()
        assertTrue(stopped)
    }
}
