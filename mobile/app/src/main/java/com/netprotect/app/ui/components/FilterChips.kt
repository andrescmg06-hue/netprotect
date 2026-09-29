package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Chips de filtro en fila desplazable horizontal; ídem colores que `SegmentedControl`. */
@Composable
fun FilterChips(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(options) { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .background(
                        if (selected) NpColors.SignalBlue else NpColors.BlueMist,
                        NpShapes.Pill,
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = NpText.Label,
                    color = if (selected) NpColors.PaperWhite else NpColors.SlateMuted,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun FilterChipsPreview() {
    NetProtectTheme {
        FilterChips(
            options = listOf("Todas", "Redes sociales", "Juegos", "Streaming"),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
