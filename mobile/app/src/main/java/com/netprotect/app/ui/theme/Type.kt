package com.netprotect.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.netprotect.app.R

/** Inter, empaquetada en `{res}/font/` (D-04: sin Google Fonts descargable). Pesos 400/500/600/700. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/**
 * Escala tipográfica de `DESIGN.md` (tamaños en sp, interlineado = tamaño × factor, letterSpacing
 * en em). El color **no** va en los estilos: lo pone el componente.
 */
object NpText {
    val Display = TextStyle(
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 38.4.sp,
        letterSpacing = (-0.02).em,
    )
    val Headline = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 33.6.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    )
    val Title = TextStyle(
        fontSize = 18.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 21.6.sp,
    )
    val BodyLead = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.5.sp,
    )
    val Body = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
    )
    val BodyStrong = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 21.sp,
    )
    val Label = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 19.5.sp,
    )
    val Caption = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 18.sp,
    )
    /** Cifras que no bailan (tnum) para conteos y valores de métricas. */
    val Numeric = TextStyle(
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 33.6.sp,
        letterSpacing = (-0.01).em,
        fontFeatureSettings = "tnum",
    )
    val NumericSmall = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.5.sp,
        fontFeatureSettings = "tnum",
    )
}

val NetProtectTypography = Typography(
    displayMedium = NpText.Display,
    headlineMedium = NpText.Headline,
    titleMedium = NpText.Title,
    bodyLarge = NpText.BodyLead,
    bodyMedium = NpText.Body,
    labelLarge = NpText.Label,
    labelSmall = NpText.Caption,
)
