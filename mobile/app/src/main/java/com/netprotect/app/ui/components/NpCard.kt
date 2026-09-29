package com.netprotect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpElevation
import com.netprotect.app.ui.theme.NpShadow
import com.netprotect.app.ui.theme.NpShapes

/** Tarjeta blanca con borde `Hairline`, sombra `NpElevation.Card` y radio 16 dp. Si `onClick` no es
 * null, es pulsable con semántica de botón (objetivo táctil ≥ 48 dp). */
@Composable
fun NpCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = NpShapes.Xl
    // Blue-tinted whisper instead of Surface's black shadowElevation (DESIGN.md: "hairline plus
    // whisper"); the tint only applies on API 28+, older versions fall back to the plain shadow.
    val shadowed = modifier.fillMaxWidth().shadow(
        elevation = NpElevation.Card,
        shape = shape,
        ambientColor = NpShadow.Ambient,
        spotColor = NpShadow.Spot,
    )
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = shadowed,
            shape = shape,
            color = NpColors.PaperWhite,
            border = BorderStroke(1.dp, NpColors.Hairline),
            content = body,
        )
    } else {
        Surface(
            modifier = shadowed,
            shape = shape,
            color = NpColors.PaperWhite,
            border = BorderStroke(1.dp, NpColors.Hairline),
            content = body,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun NpCardPreview() {
    NetProtectTheme {
        NpCard {
            Text("Contenido de la tarjeta")
        }
    }
}
