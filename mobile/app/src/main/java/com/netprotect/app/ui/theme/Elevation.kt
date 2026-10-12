package com.netprotect.app.ui.theme

import androidx.compose.ui.unit.dp

/** Elevaciones y sombras de `DESIGN.md`. */
object NpElevation {
    val Card = 2.dp
    val Pop = 12.dp
    val Glow = 4.dp
}

/** Colores de sombra (dos capas). Las sombras de `DESIGN.md` son de dos capas y no se reproducen
 * exactas en Compose; Claude las ajusta al ver la galería. */
object NpShadow {
    val Ambient = NpColors.ShieldNavy.copy(alpha = 0.08f)
    val Spot = NpColors.ShieldNavy.copy(alpha = 0.10f)
}
