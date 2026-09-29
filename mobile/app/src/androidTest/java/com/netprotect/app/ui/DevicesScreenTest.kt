package com.netprotect.app.ui

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.feature.tutor.devices.DevicesScreen
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NetProtectTheme
import java.time.Instant
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DevicesScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun failedShowsMessageAndRetryCallsOnRefresh() {
        var refreshed = false
        composeRule.setContent {
            NetProtectTheme {
                DevicesScreen(
                    devices = LoadState.Failed("Sin conexión con el servidor."),
                    now = Instant.parse("2026-09-29T15:00:00Z"),
                    onRefresh = { refreshed = true },
                    onOpenDevice = {},
                )
            }
        }
        composeRule.onNodeWithText("Sin conexión con el servidor.").assertExists()
        composeRule.onNodeWithText("Reintentar").performScrollTo().performClick()
        assertTrue(refreshed)
    }
}
