package com.netprotect.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.rememberReducedMotion

/**
 * Thin bar that drains as a deadline approaches: [progress] is the share still left (1 full, 0 empty).
 * Decorative — the text beside it says the same — so it has no semantics. Between updates (once a second)
 * it glides linearly; with reduced motion it jumps.
 */
@Composable
fun NpCountdownBar(progress: Float, modifier: Modifier = Modifier) {
    val reducedMotion = rememberReducedMotion()
    val shown by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = if (reducedMotion) snap() else tween(durationMillis = 1_000, easing = LinearEasing),
        label = "countdown",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(NpColors.Hairline)
            .clearAndSetSemantics { },
    ) {
        Box(
            Modifier
                .fillMaxWidth(shown)
                .fillMaxHeight()
                .background(NpColors.Blue),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpCountdownBarPreview() {
    NetProtectTheme {
        NpCountdownBar(progress = 0.9f)
    }
}
