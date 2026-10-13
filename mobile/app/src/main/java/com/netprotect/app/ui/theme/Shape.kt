package com.netprotect.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Radios v2: 4 dp en campos, botones y tarjetas; píldora solo para chips de estado. */
object NpShapes {
    val Field = RoundedCornerShape(4.dp)
    val Card = RoundedCornerShape(4.dp)
    val Pill = RoundedCornerShape(50)
}

val NetProtectShapes = Shapes(
    small = NpShapes.Field,
    medium = NpShapes.Field,
    large = NpShapes.Card,
)
