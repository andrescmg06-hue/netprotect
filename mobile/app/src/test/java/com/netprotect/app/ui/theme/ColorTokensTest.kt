package com.netprotect.app.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ColorTokensTest {

    private fun assertColor(expectedArgb: Long, actual: Color) {
        assertEquals(Color(expectedArgb), actual)
    }

    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun surfacesAndLines() {
        assertColor(0xFFF5F3EE, NpColors.Cream)
        assertColor(0xFFFFFFFF, NpColors.Paper)
        assertColor(0xFFF3F1EC, NpColors.Stone)
        assertColor(0xFFD9D6CF, NpColors.Hairline)
        assertColor(0xFFC2BDB2, NpColors.HairlineStrong)
        assertColor(0xFF0D1B2A, NpColors.Navy)
    }

    @Test
    fun inkAndBlue() {
        assertColor(0xFF11243D, NpColors.Ink)
        assertColor(0xFF5B6779, NpColors.InkMuted)
        assertColor(0xFF8A93A1, NpColors.InkSubtle)
        assertColor(0xFF1769FF, NpColors.Blue)
        assertColor(0xFF1456D9, NpColors.BlueText)
        assertColor(0xFFE8EFFD, NpColors.BlueWash)
    }

    @Test
    fun semanticTriples() {
        assertColor(0xFF16B364, NpColors.Success)
        assertColor(0xFFE7F8EF, NpColors.SuccessWash)
        assertColor(0xFF0B7A42, NpColors.SuccessText)
        assertColor(0xFFF04438, NpColors.Danger)
        assertColor(0xFFFEECEB, NpColors.DangerWash)
        assertColor(0xFFC4271C, NpColors.DangerText)
        assertColor(0xFFFDDCD9, NpColors.DangerHoverWash)
        assertColor(0xFFF79009, NpColors.Warning)
        assertColor(0xFFFEF3E2, NpColors.WarningWash)
        assertColor(0xFF9A5000, NpColors.WarningText)
        assertColor(0xFF7A5AF8, NpColors.Violet)
        assertColor(0xFFF1EDFE, NpColors.VioletWash)
        assertColor(0xFF5B3FD6, NpColors.VioletText)
    }

    @Test
    fun textTokensMeetAaOnCreamAndPaper() {
        val texts = mapOf(
            "Ink" to NpColors.Ink,
            "InkMuted" to NpColors.InkMuted,
            "BlueText" to NpColors.BlueText,
            "SuccessText" to NpColors.SuccessText,
            "WarningText" to NpColors.WarningText,
            "DangerText" to NpColors.DangerText,
        )
        for ((name, color) in texts) {
            val onCream = contrast(color, NpColors.Cream)
            val onPaper = contrast(color, NpColors.Paper)
            assertTrue("$name on Cream = $onCream", onCream >= 4.5)
            assertTrue("$name on Paper = $onPaper", onPaper >= 4.5)
        }
    }

    @Test
    fun toneTextMeetsAaOnItsWash() {
        for (tone in NpTone.values()) {
            if (tone == NpTone.Purple) continue
            val ratio = contrast(tone.text, tone.wash)
            assertTrue("${tone.name} text on wash = $ratio", ratio >= 4.5)
        }
    }

    @Test
    fun inkSubtleIsDecorativeOnly() {
        // Documented rule: InkSubtle must never carry body text, and indeed it does not reach AA.
        assertFalse(contrast(NpColors.InkSubtle, NpColors.Cream) >= 4.5)
    }

    @Test
    fun reducedMotionOnlyWhenScaleIsZero() {
        assertTrue(isReducedMotion(0f))
        assertFalse(isReducedMotion(1f))
        assertFalse(isReducedMotion(0.5f))
    }
}
