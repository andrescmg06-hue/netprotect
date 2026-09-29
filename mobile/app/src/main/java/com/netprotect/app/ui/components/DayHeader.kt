package com.netprotect.app.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Cabecera de grupo de día ("Hoy", "Ayer", "26 de septiembre de 2026"). */
@Composable
fun DayHeader(text: String, modifier: Modifier = Modifier) {
    Text(text = text, style = NpText.Label, color = NpColors.SlateMuted, modifier = modifier)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun DayHeaderPreview() {
    NetProtectTheme {
        DayHeader(text = "Hoy")
    }
}
