package com.netprotect.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTokensTest {

    private fun assertColor(expectedArgb: Long, actual: Color) {
        assertEquals(Color(expectedArgb), actual)
    }

    @Test
    fun primaryBlues() {
        assertColor(0xFF246BFE, NpColors.SignalBlue)
        assertColor(0xFF1456D9, NpColors.SignalBlueDeep)
        assertColor(0xFF1F5FE6, NpColors.SignalBlueText)
        assertColor(0xFFEAF1FF, NpColors.BlueWash)
        assertColor(0xFFF3F7FF, NpColors.BlueMist)
    }

    @Test
    fun neutrals() {
        assertColor(0xFF102B63, NpColors.ShieldNavy)
        assertColor(0xFFF5F9FF, NpColors.SkyGround)
        assertColor(0xFFFFFFFF, NpColors.PaperWhite)
        assertColor(0xFFF7F9FC, NpColors.PaperMuted)
        assertColor(0xFFE6ECF5, NpColors.Hairline)
        assertColor(0xFFD5DFEE, NpColors.HairlineStrong)
        assertColor(0xFF1B2B4B, NpColors.Ink)
        assertColor(0xFF5B6B82, NpColors.SlateMuted)
        assertColor(0xFF94A3B8, NpColors.SlateSubtle)
        assertColor(0xFFEEF4FF, NpColors.SkyGroundEnd)
        assertColor(0xFFCFDCF7, NpColors.SecondaryBorder)
    }

    @Test
    fun success() {
        assertColor(0xFF16B364, NpColors.Success)
        assertColor(0xFFE7F8EF, NpColors.SuccessWash)
        assertColor(0xFF0B7A42, NpColors.SuccessText)
    }

    @Test
    fun danger() {
        assertColor(0xFFF04438, NpColors.Danger)
        assertColor(0xFFFEECEB, NpColors.DangerWash)
        assertColor(0xFFC4271C, NpColors.DangerText)
        assertColor(0xFFFDDCD9, NpColors.DangerHoverWash)
    }

    @Test
    fun warning() {
        assertColor(0xFFF79009, NpColors.Warning)
        assertColor(0xFFFEF3E2, NpColors.WarningWash)
        assertColor(0xFF9A5000, NpColors.WarningText)
    }

    @Test
    fun violetAndNeutral() {
        assertColor(0xFF7A5AF8, NpColors.Violet)
        assertColor(0xFFF1EDFE, NpColors.VioletWash)
        assertColor(0xFF5B3FD6, NpColors.VioletText)
        assertColor(0xFFEEF2F7, NpColors.NeutralWash)
    }
}
