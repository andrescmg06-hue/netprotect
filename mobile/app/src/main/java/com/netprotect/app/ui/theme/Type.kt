package com.netprotect.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
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
 * Newsreader (OFL), fuente variable empaquetada (`{res}/font/newsreader.ttf`, licencia en
 * `mobile/third_party_licenses/NEWSREADER-OFL.txt`). Peso 600 con el eje óptico `opsz` igual al
 * tamaño en sp, para que cada título use su diseño óptico. minSdk 26 admite fuentes variables.
 */
@OptIn(ExperimentalTextApi::class)
private fun newsreaderSemiBold(opticalSize: Float) = FontFamily(
    Font(
        resId = R.font.newsreader,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.Setting("opsz", opticalSize),
        ),
    ),
)

val NewsreaderDisplay = newsreaderSemiBold(36f)
val NewsreaderTitle = newsreaderSemiBold(26f)

/**
 * Escala tipográfica v2 (tamaños en sp, interlineado explícito, letterSpacing en em). El color
 * **no** va en los estilos: lo pone el componente.
 */
object NpText {
    val Display = TextStyle(
        fontFamily = NewsreaderDisplay,
        fontSize = 36.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 40.sp,
    )
    val Title = TextStyle(
        fontFamily = NewsreaderTitle,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 32.sp,
    )
    val Subtitle = TextStyle(
        fontFamily = Inter,
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 24.sp,
    )
    val Body = TextStyle(
        fontFamily = Inter,
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 25.sp,
    )
    val BodyStrong = TextStyle(
        fontFamily = Inter,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 25.sp,
    )
    val BodySm = TextStyle(
        fontFamily = Inter,
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 21.sp,
    )
    val Label = TextStyle(
        fontFamily = Inter,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp,
    )
    val Caption = TextStyle(
        fontFamily = Inter,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp,
    )
    /** Small caps-style label; the caller applies `uppercase()`. */
    val Eyebrow = TextStyle(
        fontFamily = Inter,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 16.sp,
        letterSpacing = 0.14.em,
    )
    /** Cifras que no bailan (tnum) para valores de métricas y códigos. */
    val NumericSmall = TextStyle(
        fontFamily = Inter,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.5.sp,
        fontFeatureSettings = "tnum",
    )
}

val NetProtectTypography = Typography(
    displayMedium = NpText.Display,
    headlineMedium = NpText.Title,
    titleMedium = NpText.Subtitle,
    bodyLarge = NpText.Body,
    bodyMedium = NpText.Body,
    bodySmall = NpText.BodySm,
    labelLarge = NpText.Label,
    labelMedium = NpText.Caption,
    labelSmall = NpText.Eyebrow,
)
