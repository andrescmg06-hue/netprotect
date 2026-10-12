package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Control segmentado (una opción entre varias). La elegida en `SignalBlue` + blanco, radio
 * `NpShapes.Sm`; el resto `BlueMist`. Alto ≥ 48 dp. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NpColors.PaperWhite, NpShapes.Sm)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .background(
                        if (selected) NpColors.SignalBlue else NpColors.BlueMist,
                        NpShapes.Sm,
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = option,
                    style = NpText.Label,
                    color = if (selected) NpColors.PaperWhite else NpColors.SlateMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun SegmentedControlPreview() {
    NetProtectTheme {
        SegmentedControl(
            options = listOf("Hoy", "7 días", "30 días"),
            selectedIndex = 0,
            onSelect = {},
        )
    }
}
