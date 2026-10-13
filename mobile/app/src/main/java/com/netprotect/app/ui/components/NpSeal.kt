package com.netprotect.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpMotion
import com.netprotect.app.ui.theme.rememberReducedMotion

private const val SEAL_DRAW_MS = 1000
private const val ARC_GAP_DEGREES = 18f

/**
 * "Sello": four arcs around the logo shield, one per permission. [SealArcState.Ready] draws a green
 * arc, [SealArcState.Pending] an amber one and [SealArcState.Inactive] a dashed grey one ([inactiveColor],
 * which a dark surface can soften). The arcs are ordered clockwise from the top-right; any list size is
 * coerced to four (see [normalizeSealStates]).
 *
 * The arcs draw themselves once, in 1 s with [NpMotion.Easing], on first composition. With the system's
 * "remove animations" ([reducedMotion]) they appear complete, with no animation. The seal is decorative
 * unless the caller passes [contentDescription]; it never carries information on its own.
 */
@Composable
fun NpSeal(
    states: List<SealArcState>,
    modifier: Modifier = Modifier,
    size: Dp = 112.dp,
    contentDescription: String? = null,
    inactiveColor: Color = NpColors.HairlineStrong,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val arcs = normalizeSealStates(states)
    val progress = remember { Animatable(sealInitialProgress(reducedMotion)) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else if (progress.value < 1f) {
            progress.animateTo(1f, tween(durationMillis = SEAL_DRAW_MS, easing = NpMotion.Easing))
        }
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.clearAndSetSemantics { this.contentDescription = contentDescription }
    } else {
        Modifier.clearAndSetSemantics { }
    }

    Box(
        modifier = modifier
            .size(size)
            .then(semanticsModifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = this.size.minDimension * 0.045f
            val inset = strokeWidth / 2f
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val sweepFull = 90f - ARC_GAP_DEGREES
            val sweep = sweepFull * progress.value
            if (sweep <= 0.01f) return@Canvas
            arcs.forEachIndexed { index, state ->
                val start = -90f + index * 90f + ARC_GAP_DEGREES / 2f
                val stroke = when (state) {
                    SealArcState.Inactive -> Stroke(
                        width = strokeWidth * 0.7f,
                        cap = StrokeCap.Butt,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokeWidth * 1.1f, strokeWidth * 0.9f)),
                    )
                    else -> Stroke(width = strokeWidth, cap = StrokeCap.Round)
                }
                drawArc(
                    color = when (state) {
                        SealArcState.Ready -> NpColors.Success
                        SealArcState.Pending -> NpColors.Warning
                        SealArcState.Inactive -> inactiveColor
                    },
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        Image(
            painter = painterResource(NpIcons.LogoShield),
            contentDescription = null,
            modifier = Modifier.size(size * 0.4f),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpSealPreview() {
    NetProtectTheme {
        NpSeal(states = sealStatesFor(readyCount = 2), reducedMotion = true)
    }
}
