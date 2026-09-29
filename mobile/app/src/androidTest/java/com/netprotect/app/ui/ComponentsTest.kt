package com.netprotect.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.CodeDisplay
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.NpBottomBar
import com.netprotect.app.ui.components.NpBottomItem
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.OtpInput
import com.netprotect.app.ui.components.SeverityBadge
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComponentsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingButtonDoesNotCallOnClick() {
        var clicked = false
        composeRule.setContent {
            NetProtectTheme {
                NpButton(text = "Guardar", onClick = { clicked = true }, loading = true)
            }
        }
        composeRule.onNodeWithText("Guardar").performTouchInput { click() }
        composeRule.waitForIdle()
        assertFalse(clicked)
    }

    @Test
    fun disabledButtonDoesNotCallOnClick() {
        var clicked = false
        composeRule.setContent {
            NetProtectTheme {
                NpButton(text = "Guardar", onClick = { clicked = true }, enabled = false)
            }
        }
        composeRule.onNodeWithText("Guardar").performTouchInput { click() }
        composeRule.waitForIdle()
        assertFalse(clicked)
    }

    @Test
    fun otpInputFiltersLettersAndTruncatesToSixDigits() {
        var value by mutableStateOf("")
        composeRule.setContent {
            NetProtectTheme {
                OtpInput(value = value, onValueChange = { value = it })
            }
        }
        composeRule.onNode(hasSetTextAction()).performTextInput("12a34 56789")
        composeRule.waitForIdle()
        assertEquals("123456", value)
    }

    @Test
    fun codeDisplayShowsExpiredWhenZeroSeconds() {
        composeRule.setContent {
            NetProtectTheme {
                CodeDisplay(code = "482917", remainingSeconds = 0, totalSeconds = 180)
            }
        }
        composeRule.onNodeWithText("El código venció").assertExists()
    }

    @Test
    fun severityBadgeCriticalShowsCritica() {
        composeRule.setContent {
            NetProtectTheme {
                SeverityBadge(level = "CRITICAL")
            }
        }
        composeRule.onNodeWithText("Crítica").assertExists()
    }

    @Test
    fun errorStateInvokesOnRetry() {
        var retried = false
        composeRule.setContent {
            NetProtectTheme {
                ErrorState(message = "No se pudo cargar.", onRetry = { retried = true })
            }
        }
        composeRule.onNodeWithText("Reintentar").performClick()
        assertTrue(retried)
    }

    @Test
    fun bottomBarItemsAreAtLeast48dpTall() {
        composeRule.setContent {
            NetProtectTheme {
                NpBottomBar(
                    items = listOf(
                        NpBottomItem("Inicio", NpIcons.Home),
                        NpBottomItem("Dispositivos", NpIcons.Smartphone, badgeCount = 3),
                        NpBottomItem("Actividad", NpIcons.Clock),
                        NpBottomItem("Más", NpIcons.MoreHorizontal),
                    ),
                    selectedIndex = 0,
                    onSelect = {},
                )
            }
        }
        val clickable = composeRule.onAllNodes(hasClickAction())
        val count = clickable.fetchSemanticsNodes().size
        assertTrue("expected 4 clickable items, got $count", count >= 4)
        for (i in 0 until count) {
            clickable[i].assertHeightIsAtLeast(48.dp)
        }
    }
}
