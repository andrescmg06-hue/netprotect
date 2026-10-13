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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes

/** Tarjeta (blanca por defecto; `containerColor`/`borderColor` para variantes como la zona de
 * peligro) con borde `Hairline`, sin sombra (elevación 0) y radio 4 dp. Si `onClick` no es
 * null, es pulsable con semántica de botón (objetivo táctil ≥ 48 dp). */
@Composable
fun NpCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = NpColors.Paper,
    borderColor: Color = NpColors.Hairline,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = NpShapes.Card
    val cardModifier = modifier.fillMaxWidth()
    val body: @Composable () -> Unit = {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = body,
        )
    } else {
        Surface(
            modifier = cardModifier,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            content = body,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpCardPreview() {
    NetProtectTheme {
        NpCard {
            Text("Contenido de la tarjeta")
        }
    }
}
