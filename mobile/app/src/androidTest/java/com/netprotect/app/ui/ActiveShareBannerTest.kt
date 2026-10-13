package com.netprotect.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
                // Scrollable host only so the click goes through performScrollTo, as in every UI test.
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ActiveShareBanner(onStop = { stopped = true })
                }
            }
        }
        composeRule.onNodeWithText("Tu tutor está viendo tu pantalla").performScrollTo().assertExists()
        composeRule.onNodeWithText("Detener").performScrollTo().performClick()
        assertTrue(stopped)
    }
}
