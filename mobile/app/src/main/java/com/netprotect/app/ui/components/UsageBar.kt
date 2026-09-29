package com.netprotect.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpTone

/** Barra de uso de 8 dp: pista `NeutralWash`, relleno `tone.solid`. `fraction` se limita a 0..1 y se
 * anima con `scaleX` (no con `width`, para no provocar relayout). */
@Composable
fun UsageBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    tone: NpTone = NpTone.Info,
    contentDescription: String,
) {
    val animated by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 250),
        label = "usage",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(NpColors.NeutralWash, NpShapes.Sm)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = animated
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
                .background(tone.solid, NpShapes.Sm),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun UsageBarPreview() {
    NetProtectTheme {
        UsageBar(fraction = 0.4f, contentDescription = "40 % usado")
    }
}
