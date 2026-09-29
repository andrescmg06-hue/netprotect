package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

enum class NpButtonVariant { Primary, Secondary, Text, Danger }

/** Botón con 4 variantes. Alto ≥ 48 dp (`small` = 40 dp mínimo). `loading` sustituye el icono por un
 * spinner y desactiva el botón (sin dobles toques); `enabled = false` baja la opacidad al 55 %. El
 * texto puede envolver a dos líneas sin aplastarse en vertical (no es un `TextButton` de Material). */
@Composable
fun NpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NpButtonVariant = NpButtonVariant.Primary,
    @DrawableRes icon: Int? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val shape = NpShapes.Md
    val minHeight = if (small) 40.dp else 48.dp

    val (background, baseContentColor, borderColor) = when (variant) {
        NpButtonVariant.Primary -> Triple(NpColors.SignalBlue, NpColors.PaperWhite, Color.Transparent)
        NpButtonVariant.Secondary -> Triple(NpColors.PaperWhite, NpColors.SignalBlueText, NpColors.SecondaryBorder)
        NpButtonVariant.Text -> Triple(Color.Transparent, NpColors.SlateMuted, Color.Transparent)
        NpButtonVariant.Danger -> Triple(NpColors.DangerWash, NpColors.DangerText, NpColors.Danger.copy(alpha = 0.3f))
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    // Variante Text: al pulsar el texto pasa a SignalBlueText.
    val contentColor = if (variant == NpButtonVariant.Text && isPressed) NpColors.SignalBlueText else baseContentColor

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(background, shape)
            .then(
                if (variant == NpButtonVariant.Secondary || variant == NpButtonVariant.Danger) {
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                enabled = enabled && !loading,
                onClick = onClick,
            )
            .padding(horizontal = if (small) 12.dp else 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = contentColor,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = contentColor,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = NpText.BodyStrong,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun NpButtonPreview() {
    NetProtectTheme {
        NpButton(text = "Guardar", onClick = {})
    }
}
