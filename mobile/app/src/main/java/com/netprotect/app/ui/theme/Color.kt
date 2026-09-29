package com.netprotect.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Design tokens de color (fuente de verdad: `DESIGN.md`, raíz del repo). Este es el **único**
 * archivo de la app que puede contener literales `Color(0x…)`; el resto usa `NpColors.*` o los
 * nombres del `ColorScheme`.
 *
 * Regla de uso: **`SignalBlue` es para rellenos y bordes; el texto e iconos azules usan
 * `SignalBlueText`** (contraste AA). `SlateSubtle` solo para placeholders. Solo tema claro.
 */
object NpColors {
    val SignalBlue = Color(0xFF246BFE)
    val SignalBlueDeep = Color(0xFF1456D9)
    val SignalBlueText = Color(0xFF1F5FE6)
    val BlueWash = Color(0xFFEAF1FF)
    val BlueMist = Color(0xFFF3F7FF)
    val ShieldNavy = Color(0xFF102B63)
    val SkyGround = Color(0xFFF5F9FF)
    val PaperWhite = Color(0xFFFFFFFF)
    val PaperMuted = Color(0xFFF7F9FC)
    val Hairline = Color(0xFFE6ECF5)
    val HairlineStrong = Color(0xFFD5DFEE)
    val Ink = Color(0xFF1B2B4B)
    val SlateMuted = Color(0xFF5B6B82)
    val SlateSubtle = Color(0xFF94A3B8)
    val SkyGroundEnd = Color(0xFFEEF4FF)
    val SecondaryBorder = Color(0xFFCFDCF7)
    val Success = Color(0xFF16B364)
    val SuccessWash = Color(0xFFE7F8EF)
    val SuccessText = Color(0xFF0B7A42)
    val Danger = Color(0xFFF04438)
    val DangerWash = Color(0xFFFEECEB)
    val DangerText = Color(0xFFC4271C)
    val DangerHoverWash = Color(0xFFFDDCD9)
    val Warning = Color(0xFFF79009)
    val WarningWash = Color(0xFFFEF3E2)
    val WarningText = Color(0xFF9A5000)
    val Violet = Color(0xFF7A5AF8)
    val VioletWash = Color(0xFFF1EDFE)
    val VioletText = Color(0xFF5B3FD6)
    val NeutralWash = Color(0xFFEEF2F7)
}

/** Solo claro (D: no hay tema oscuro). */
val NetProtectColorScheme = lightColorScheme(
    primary = NpColors.SignalBlue,
    onPrimary = NpColors.PaperWhite,
    primaryContainer = NpColors.BlueWash,
    onPrimaryContainer = NpColors.SignalBlueDeep,
    secondary = NpColors.SignalBlueText,
    onSecondary = NpColors.PaperWhite,
    background = NpColors.SkyGround,
    onBackground = NpColors.Ink,
    surface = NpColors.PaperWhite,
    onSurface = NpColors.Ink,
    surfaceVariant = NpColors.BlueMist,
    onSurfaceVariant = NpColors.SlateMuted,
    outline = NpColors.HairlineStrong,
    outlineVariant = NpColors.Hairline,
    error = NpColors.Danger,
    onError = NpColors.PaperWhite,
    errorContainer = NpColors.DangerWash,
    onErrorContainer = NpColors.DangerText,
)
