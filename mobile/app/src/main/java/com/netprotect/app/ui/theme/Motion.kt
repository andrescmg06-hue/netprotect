package com.netprotect.app.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Movimiento v2: solo ante eventos, curva de salida exponencial, sin bucles decorativos. */
object NpMotion {
    const val FAST_MS = 120
    const val BASE_MS = 240
    const val SLOW_MS = 360
    val Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    const val StaggerMs = 80
    val EnterOffset = 14.dp
}

/** Pure rule behind [rememberReducedMotion]: the system "remove animations" switch sets the scale to 0. */
fun isReducedMotion(animatorDurationScale: Float): Boolean = animatorDurationScale == 0f

/** True when the user turned system animations off (`ANIMATOR_DURATION_SCALE` = 0). */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        isReducedMotion(Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f))
    }
}
