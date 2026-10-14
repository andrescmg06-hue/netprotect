package com.netprotect.app.feature.supervised.block

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpMotion
import com.netprotect.app.ui.theme.rememberReducedMotion

private const val PADLOCK_CLOSE_MS = 560
private const val PADLOCK_DELAY_MS = 120

/** How far the shackle starts above its closed place, as a fraction of the padlock's height. */
private const val SHACKLE_MAX_LIFT = 0.16f

/** Start of the closing animation: already closed with "remove animations" on, open otherwise. */
fun padlockInitialProgress(reducedMotion: Boolean): Float = if (reducedMotion) 1f else 0f

/** Shackle lift (fraction of the padlock's height) for a closing [progress] in 0..1; 0 = closed. */
fun padlockShackleLift(progress: Float): Float = SHACKLE_MAX_LIFT * (1f - progress.coerceIn(0f, 1f))

/**
 * The blue padlock of the block panel (lámina 07): filled body with a keyhole and a stroked shackle.
 * It closes once, the first time it is composed (the shackle drops into the body); with the system's
 * "remove animations" it is drawn closed from the start. Decorative: the panel label says the same.
 */
@Composable
fun BlockPadlock(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    color: Color = NpColors.Blue,
    keyholeColor: Color = NpColors.Navy,
    reducedMotion: Boolean = rememberReducedMotion(),
) {
    val progress = remember { Animatable(padlockInitialProgress(reducedMotion)) }
    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            progress.snapTo(1f)
        } else if (progress.value < 1f) {
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(PADLOCK_CLOSE_MS, delayMillis = PADLOCK_DELAY_MS, easing = NpMotion.Easing),
            )
        }
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.11f
        val bodyTop = h * 0.46f
        val bodyLeft = w * 0.14f
        val bodyWidth = w * 0.72f

        // Shackle: two legs joined by a half circle, drawn behind the body so its feet hide in it.
        val lift = h * padlockShackleLift(progress.value)
        val legLeft = w * 0.30f
        val legRight = w * 0.70f
        val radius = (legRight - legLeft) / 2f
        val archCentreY = h * 0.30f - lift
        val shackle = Path().apply {
            moveTo(legLeft, bodyTop + stroke - lift)
            lineTo(legLeft, archCentreY)
            arcTo(
                rect = Rect(
                    left = legLeft,
                    top = archCentreY - radius,
                    right = legRight,
                    bottom = archCentreY + radius,
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false,
            )
            lineTo(legRight, bodyTop + stroke - lift)
        }
        drawPath(shackle, color = color, style = Stroke(width = stroke, cap = StrokeCap.Butt))

        drawRoundRect(
            color = color,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.06f),
        )
        val keyholeCentre = Offset(w / 2f, bodyTop + h * 0.20f)
        drawCircle(color = keyholeColor, radius = w * 0.075f, center = keyholeCentre)
        drawRect(
            color = keyholeColor,
            topLeft = Offset(w / 2f - w * 0.03f, keyholeCentre.y),
            size = Size(w * 0.06f, h * 0.12f),
        )
    }
}
