package com.netprotect.app.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.feature.supervised.block.BLOCK_COVER_NOTE
import com.netprotect.app.feature.supervised.block.BlockScreenContent
import com.netprotect.app.feature.supervised.block.blockPresentation
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BlockScreenContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun set(
        reason: BlockReason,
        categoryLabel: String? = "Streaming",
        onGoHome: () -> Unit = {},
    ) {
        composeRule.setContent {
            NetProtectTheme {
                BlockScreenContent(
                    packageName = "com.test.app",
                    appLabel = "MiApp",
                    categoryLabel = categoryLabel,
                    reason = reason,
                    onGoHome = onGoHome,
                )
            }
        }
    }

    @Test
    fun everyReasonShowsContentAndGoHome() {
        var wentHome = false
        val reasonState = mutableStateOf(BlockReason.BLOCK)
        composeRule.setContent {
            NetProtectTheme {
                BlockScreenContent(
                    packageName = "com.test.app",
                    appLabel = "MiApp",
                    categoryLabel = "Streaming",
                    reason = reasonState.value,
                    onGoHome = { wentHome = true },
                )
            }
        }
        BlockReason.entries.forEach { reason ->
            composeRule.runOnIdle {
                reasonState.value = reason
                wentHome = false
            }
            composeRule.onNodeWithText("APP BLOQUEADA").assertExists()
            composeRule.onNodeWithText("MiApp").assertExists()
            val p = blockPresentation(reason, "Streaming")
            composeRule.onNodeWithText(p.message).assertExists()
            composeRule.onNodeWithText(p.hint).assertExists()
            composeRule.onNodeWithText("Ir al inicio").performScrollTo().performClick()
            assertTrue(wentHome)
        }
    }

    @Test
    fun categoryStreamingShown() {
        set(BlockReason.CATEGORY, categoryLabel = "Streaming")
        composeRule.onNodeWithText("Streaming").assertExists()
    }

    @Test
    fun nullCategoryShowsNoNullText() {
        set(BlockReason.BLOCK, categoryLabel = null)
        composeRule.onNodeWithText("null").assertDoesNotExist()
    }

    @Test
    fun dailyLimitShowsManana() {
        set(BlockReason.DAILY_LIMIT)
        composeRule.onNodeWithText("mañana", substring = true).assertExists()
    }

    @Test
    fun weeklyLimitShowsLunes() {
        set(BlockReason.WEEKLY_LIMIT)
        composeRule.onNodeWithText("lunes", substring = true).assertExists()
    }

    @Test
    fun otherReasonsNoMananaLunesNorTime() {
        val reasonState = mutableStateOf(BlockReason.BLOCK)
        composeRule.setContent {
            NetProtectTheme {
                BlockScreenContent(
                    packageName = "com.test.app",
                    appLabel = "MiApp",
                    categoryLabel = "Streaming",
                    reason = reasonState.value,
                    onGoHome = {},
                )
            }
        }
        BlockReason.entries
            .filter { it != BlockReason.DAILY_LIMIT && it != BlockReason.WEEKLY_LIMIT }
            .forEach { reason ->
                composeRule.runOnIdle { reasonState.value = reason }
                composeRule.onNodeWithText("mañana", substring = true).assertDoesNotExist()
                composeRule.onNodeWithText("lunes", substring = true).assertDoesNotExist()
                val p = blockPresentation(reason, "Streaming")
                assertFalse(Regex("""\d{1,2}:\d{2}""").containsMatchIn("${p.message} ${p.hint}"))
            }
    }

    @Test
    fun noUnlockOrRequestText() {
        set(BlockReason.BLOCK)
        composeRule.onNodeWithText("Desbloquear", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Pedir", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("Solicitar", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("más tiempo", substring = true).assertDoesNotExist()
    }

    @Test
    fun seesBlockCoverNote() {
        set(BlockReason.BLOCK)
        composeRule.onNodeWithText(BLOCK_COVER_NOTE).assertExists()
    }
}
