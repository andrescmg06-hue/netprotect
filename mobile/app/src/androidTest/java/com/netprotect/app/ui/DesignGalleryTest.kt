package com.netprotect.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.netprotect.app.debug.DesignGallery
import com.netprotect.app.ui.theme.NetProtectTheme
import org.junit.Rule
import org.junit.Test

class DesignGalleryTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun galleryRendersEverySectionTitle() {
        composeRule.setContent {
            NetProtectTheme {
                DesignGallery()
            }
        }
        val titles = listOf(
            "NpButton", "StatusPill", "SeverityBadge", "InfoBanner", "SectionHeader",
            "EmptyState", "LoadingState", "ErrorState", "BrandHeader", "NpTopBar",
            "NpBottomBar", "SegmentedControl", "FilterChips", "UsageBar", "TimelineItem",
            "DayHeader", "CodeDisplay", "OtpInput", "PermissionCard", "NpCard", "ListRow",
            "ConfirmDialog",
        )
        titles.forEach { title ->
            composeRule.onNodeWithText(title).performScrollTo().assertIsDisplayed()
        }
    }
}
