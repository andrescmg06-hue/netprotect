package com.netprotect.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Sprint 45 (D-07 b): the icon of an app on the *supervised* device, as drawn on the *tutor's*
 * phone. If the same package is installed here, its real icon (read locally from PackageManager —
 * nothing is sent anywhere); otherwise a monogram: the first letter on a tone chosen from the
 * package name, so the same app always gets the same colour. Decorative: the name is next to it. */
@Composable
fun AppIcon(packageName: String, label: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val context = LocalContext.current
    val sizePx = with(androidx.compose.ui.platform.LocalDensity.current) { size.roundToPx() }
    val bitmap: ImageBitmap? = remember(packageName, sizePx) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        val tone = monogramTone(packageName)
        Box(
            modifier = modifier.size(size).background(tone.wash, NpShapes.Md),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = monogramLetter(label, packageName), style = NpText.Title, color = tone.text)
        }
    }
}

private val MONOGRAM_TONES = listOf(NpTone.Info, NpTone.Purple, NpTone.Success, NpTone.Warning, NpTone.Danger)

/** Stable across runs and devices (String.hashCode is specified), never random. */
fun monogramTone(packageName: String): NpTone =
    MONOGRAM_TONES[Math.floorMod(packageName.hashCode(), MONOGRAM_TONES.size)]

fun monogramLetter(label: String, packageName: String): String =
    (label.trim().firstOrNull { it.isLetterOrDigit() } ?: packageName.firstOrNull { it.isLetter() } ?: '?')
        .uppercaseChar().toString()

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun AppIconPreview() {
    NetProtectTheme { AppIcon(packageName = "com.example.nonexistent", label = "Ejemplo") }
}
