package com.netprotect.app.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Design tokens de color v2 («Sello»). Este es el **único** archivo de la app que puede contener
 * literales `Color(0x…)`; el resto usa `NpColors.*` o los nombres del `ColorScheme`.
 *
 * Reglas de uso: `Blue` es solo para rellenos de acción (nunca texto sobre crema); el texto e
 * iconos azules usan `BlueText`. `InkSubtle` es decorativo (nunca texto de cuerpo). `Navy` es
 * para superficies sólidas (panel de identidad y bloqueo); el texto usa `Ink`. Solo tema claro.
 */
object NpColors {
    val Cream = Color(0xFFF5F3EE)
    val Paper = Color(0xFFFFFFFF)
    val Stone = Color(0xFFF3F1EC)
    val Hairline = Color(0xFFD9D6CF)
    val HairlineStrong = Color(0xFFC2BDB2)
    val Ink = Color(0xFF11243D)
    val InkMuted = Color(0xFF5B6779)
    val InkSubtle = Color(0xFF8A93A1)
    val Navy = Color(0xFF0D1B2A)
    val Blue = Color(0xFF1769FF)
    val BlueText = Color(0xFF1456D9)
    val BlueWash = Color(0xFFE8EFFD)
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
}

/** Solo claro (no hay tema oscuro). */
val NetProtectColorScheme = lightColorScheme(
    primary = NpColors.Blue,
    onPrimary = NpColors.Paper,
    primaryContainer = NpColors.BlueWash,
    onPrimaryContainer = NpColors.BlueText,
    secondary = NpColors.BlueText,
    onSecondary = NpColors.Paper,
    background = NpColors.Cream,
    onBackground = NpColors.Ink,
    surface = NpColors.Cream,
    onSurface = NpColors.Ink,
    surfaceVariant = NpColors.Stone,
    onSurfaceVariant = NpColors.InkMuted,
    outline = NpColors.Hairline,
    outlineVariant = NpColors.Hairline,
    error = NpColors.Danger,
    onError = NpColors.Paper,
    errorContainer = NpColors.DangerWash,
    onErrorContainer = NpColors.DangerText,
)
