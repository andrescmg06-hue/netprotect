package com.netprotect.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.netprotect.app.ui.theme.NpColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class SealAndRailLogicTest {

    @Test
    fun normalizePadsShortListsWithInactive() {
        assertEquals(
            listOf(SealArcState.Ready, SealArcState.Inactive, SealArcState.Inactive, SealArcState.Inactive),
            normalizeSealStates(listOf(SealArcState.Ready)),
        )
        assertEquals(List(4) { SealArcState.Inactive }, normalizeSealStates(emptyList()))
    }

    @Test
    fun normalizeDropsExtraEntries() {
        val six = List(6) { SealArcState.Pending }
        assertEquals(4, normalizeSealStates(six).size)
    }

    @Test
    fun readyArcsFillFromTheTopRightClockwise() {
        assertEquals(
            listOf(SealArcState.Ready, SealArcState.Ready, SealArcState.Pending, SealArcState.Pending),
            sealStatesFor(readyCount = 2),
        )
        assertEquals(List(4) { SealArcState.Ready }, sealStatesFor(readyCount = 4))
        assertEquals(List(4) { SealArcState.Pending }, sealStatesFor(readyCount = 0))
    }

    @Test
    fun noRecentReportMakesEveryArcInactive() {
        assertEquals(List(4) { SealArcState.Inactive }, sealStatesFor(readyCount = 4, reporting = false))
    }

    @Test
    fun captionClampsTheCount() {
        assertEquals("2 de 4 permisos", permissionsCaption(2))
        assertEquals("4 de 4 permisos", permissionsCaption(7))
        assertEquals("0 de 4 permisos", permissionsCaption(-1))
    }

    @Test
    fun reducedMotionStartsTheSealComplete() {
        assertEquals(1f, sealInitialProgress(reducedMotion = true))
        assertEquals(0f, sealInitialProgress(reducedMotion = false))
    }

    @Test
    fun railLabelsAndStateDescriptions() {
        assertEquals("Configurado", railStatusLabel(RailStatus.Done))
        assertEquals("Pendiente · siguiente", railStatusLabel(RailStatus.Next))
        assertEquals("Pendiente", railStatusLabel(RailStatus.Pending))
        assertEquals("Configurado", railStateDescription(RailStatus.Done))
        assertEquals("Siguiente paso", railStateDescription(RailStatus.Next))
        assertEquals("Permiso pendiente", railStateDescription(RailStatus.Pending))
    }

    private fun channel(c: Float): Double =
        if (c <= 0.03928f) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun luminance(c: Color): Double =
        0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)

    private fun contrastOnNavy(foreground: Color): Double {
        val fg = luminance(foreground.compositeOver(NpColors.Navy))
        val bg = luminance(NpColors.Navy)
        return (maxOf(fg, bg) + 0.05) / (minOf(fg, bg) + 0.05)
    }

    @Test
    fun heroTextMeetsAaOnNavy() {
        val summary = contrastOnNavy(HeroPanelColors.Summary)
        val eyebrow = contrastOnNavy(HeroPanelColors.Eyebrow)
        val headline = contrastOnNavy(HeroPanelColors.Headline)
        println("Hero contrast on navy: summary=%.2f eyebrow=%.2f headline=%.2f".format(summary, eyebrow, headline))
        assertTrue("summary $summary", summary >= 4.5)
        assertTrue("eyebrow $eyebrow", eyebrow >= 4.5)
        assertTrue("headline $headline", headline >= 4.5)
    }
}
