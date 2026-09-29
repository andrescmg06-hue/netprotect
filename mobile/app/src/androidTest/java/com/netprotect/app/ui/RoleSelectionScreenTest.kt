package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.home.RoleSelectionScreen
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RoleSelectionScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun greetsWithFirstName() {
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés Mosquera",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("Hola, Andrés").assertExists()
    }

    @Test
    fun greetsWithEmailWhenNameIsNull() {
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = null,
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("Hola, tutor@example.com").assertExists()
    }

    @Test
    fun greetsWithEmailWhenNameIsBlank() {
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "  ",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("Hola, tutor@example.com").assertExists()
    }

    @Test
    fun tutorChoiceCallsOnSelectTutor() {
        var tutor = false
        var supervised = false
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = { tutor = true },
                    onSelectSupervised = { supervised = true },
                    onSignOut = {},
                )
            }
        }
        composeRule.onAllNodesWithText("Elegir")[0].performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(tutor)
        assertTrue(!supervised)
    }

    @Test
    fun supervisedChoiceCallsOnSelectSupervised() {
        var tutor = false
        var supervised = false
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = { tutor = true },
                    onSelectSupervised = { supervised = true },
                    onSignOut = {},
                )
            }
        }
        composeRule.onAllNodesWithText("Elegir")[1].performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(supervised)
        assertTrue(!tutor)
    }

    @Test
    fun signOutCallsOnSignOut() {
        var signedOut = false
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = { signedOut = true },
                )
            }
        }
        composeRule.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        composeRule.waitForIdle()
        assertTrue(signedOut)
    }

    @Test
    fun errorShowsWhenPresent() {
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés",
                    email = "tutor@example.com",
                    error = "No se pudo guardar el modo",
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("No se pudo guardar el modo").assertExists()
    }

    @Test
    fun errorAbsentWhenNull() {
        composeRule.setContent {
            NetProtectTheme {
                RoleSelectionScreen(
                    displayName = "Andrés",
                    email = "tutor@example.com",
                    error = null,
                    onSelectTutor = {},
                    onSelectSupervised = {},
                    onSignOut = {},
                )
            }
        }
        composeRule.onNodeWithText("No se pudo guardar el modo").assertDoesNotExist()
    }
}
