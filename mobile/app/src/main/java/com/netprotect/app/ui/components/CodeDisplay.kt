package com.netprotect.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Código de vinculación: 6 dígitos en `NpText.Numeric` (cifras tabulares), separados por un espacio
 * fino, con la vigencia debajo y "El código venció" cuando expira. */
@Composable
fun CodeDisplay(
    code: String,
    remainingSeconds: Int,
    totalSeconds: Int,
    modifier: Modifier = Modifier,
) {
    val digits = code.filter { it.isDigit() }
    val grouped = digits.chunked(3).joinToString("\u2009") // espacio fino (thin space)
    Column(modifier = modifier) {
        Text(
            text = grouped,
            style = NpText.Numeric,
            color = NpColors.ShieldNavy,
            modifier = Modifier.semantics {
                // Lectura dígito a dígito (no como un número largo).
                contentDescription = digits.toList().joinToString(" ") { it.toString() }
            },
        )
        if (remainingSeconds <= 0) {
            Spacer(Modifier.height(12.dp))
            Text(text = "El código venció", style = NpText.BodyStrong, color = NpColors.DangerText)
        } else {
            Spacer(Modifier.height(12.dp))
            UsageBar(
                fraction = remainingSeconds.toFloat() / totalSeconds.coerceAtLeast(1),
                contentDescription = "Vigencia del código",
            )
            Spacer(Modifier.height(8.dp))
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            Text(
                text = "Vence en $minutes:${seconds.toString().padStart(2, '0')}",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun CodeDisplayPreview() {
    NetProtectTheme {
        CodeDisplay(code = "482917", remainingSeconds = 143, totalSeconds = 180)
    }
}
