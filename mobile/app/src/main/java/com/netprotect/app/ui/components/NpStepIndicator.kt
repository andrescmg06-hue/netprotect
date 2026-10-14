package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import java.util.Locale

/**
 * Segmented step indicator (lámina 06): one bar per step, the first [filledCount] in blue, each with its
 * label (uppercase) underneath. The labels sit under their bar while each fits there on one line; otherwise
 * (large font scale) they are listed one per line, so no word is broken letter by letter.
 */
@Composable
fun NpStepIndicator(
    labels: List<String>,
    filledCount: Int,
    modifier: Modifier = Modifier,
) {
    val gap = NpSpace.S
    Column(modifier.fillMaxWidth()) {
        // Decorative: the labels below carry the same information.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(gap),
        ) {
            labels.indices.forEach { index ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(3.dp)
                        .background(if (index < filledCount) NpColors.Blue else NpColors.Hairline),
                )
            }
        }
        Spacer(Modifier.height(NpSpace.S))
        StepLabels(gap = gap, count = labels.size) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label.uppercase(Locale.ROOT),
                    style = NpText.Eyebrow,
                    // InkMuted, not a lighter grey: the pending labels must still pass AA on cream.
                    color = if (index < filledCount) NpColors.Ink else NpColors.InkMuted,
                )
            }
        }
    }
}

@Composable
private fun StepLabels(gap: Dp, count: Int, content: @Composable () -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val gapPx = gap.roundToPx()
        val columns = count.coerceAtLeast(1)
        val cell = ((maxWidth - gapPx * (columns - 1)) / columns).coerceAtLeast(0)
        val fitsBeside = measurables.all { it.maxIntrinsicWidth(Constraints.Infinity) <= cell }
        if (fitsBeside) {
            val placeables = measurables.map { it.measure(Constraints(maxWidth = cell)) }
            val height = placeables.maxOfOrNull { it.height } ?: 0
            layout(maxWidth, height) {
                placeables.forEachIndexed { index, placeable -> placeable.placeRelative(index * (cell + gapPx), 0) }
            }
        } else {
            val placeables = measurables.map { it.measure(Constraints(maxWidth = maxWidth)) }
            val spacing = gapPx / 2
            val height = placeables.sumOf { it.height } + spacing * (placeables.size - 1).coerceAtLeast(0)
            layout(maxWidth, height) {
                var y = 0
                placeables.forEach { placeable ->
                    placeable.placeRelative(0, y)
                    y += placeable.height + spacing
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpStepIndicatorPreview() {
    NetProtectTheme {
        NpStepIndicator(labels = listOf("1 · Leer", "2 · Autorizar", "3 · Android"), filledCount = 2)
    }
}
