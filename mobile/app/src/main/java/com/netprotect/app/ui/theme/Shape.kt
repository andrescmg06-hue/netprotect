package com.netprotect.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios de `DESIGN.md` (8/10/14/16 + pill). */
object NpShapes {
    val Sm = RoundedCornerShape(8.dp)
    val Md = RoundedCornerShape(10.dp)
    val Lg = RoundedCornerShape(14.dp)
    val Xl = RoundedCornerShape(16.dp)
    val Pill = RoundedCornerShape(50)
}

val NetProtectShapes = Shapes(
    small = NpShapes.Sm,
    medium = NpShapes.Md,
    large = NpShapes.Xl,
)
