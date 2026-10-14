package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpMotion
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.rememberReducedMotion

enum class NpButtonVariant { Primary, Secondary, Text, Danger }

/** Minimum heights of the v2 buttons. */
internal object NpButtonSizes {
    val Main = 60.dp
    val Compact = 48.dp
}

private data class ButtonColors(
    val background: Color,
    val content: Color,
    val border: Color,
    val chipBackground: Color,
    val chipContent: Color,
)

private fun colorsFor(variant: NpButtonVariant): ButtonColors = when (variant) {
    NpButtonVariant.Primary -> ButtonColors(
        background = NpColors.Blue,
        content = NpColors.Paper,
        border = Color.Transparent,
        chipBackground = NpColors.Paper.copy(alpha = 0.16f),
        chipContent = NpColors.Paper,
    )
    NpButtonVariant.Secondary -> ButtonColors(
        background = NpColors.Paper,
        content = NpColors.Ink,
        border = NpColors.Ink,
        chipBackground = NpColors.Ink,
        chipContent = NpColors.Paper,
    )
    NpButtonVariant.Text -> ButtonColors(
        background = Color.Transparent,
        content = NpColors.BlueText,
        border = Color.Transparent,
        chipBackground = Color.Transparent,
        chipContent = NpColors.BlueText,
    )
    NpButtonVariant.Danger -> ButtonColors(
        background = NpColors.DangerWash,
        content = NpColors.DangerText,
        border = NpColors.Danger.copy(alpha = 0.3f),
        chipBackground = NpColors.DangerText,
        chipContent = NpColors.Paper,
    )
}

/**
 * Button, v2. [NpButtonVariant.Primary] is a blue fill ≥ 60 dp tall with a white label;
 * [NpButtonVariant.Secondary] is the outline button (1.5 dp ink border); [NpButtonVariant.Danger] is a
 * red-washed outline; [NpButtonVariant.Text] is ≥ 48 dp with a blue label underlined while pressed.
 * Heights are minimums: the button grows with the font scale and the label wraps instead of clipping.
 * `small = true` lowers every variant to 48 dp, which is still a full touch target.
 *
 * [trailingIcon] on a filled or outlined variant becomes the trailing "chip": a 4 dp-radius square that
 * slides 4 dp right on press ([NpMotion.FAST_MS], immediate with reduced motion), and the label aligns to
 * the start (use it with `Modifier.fillMaxWidth()`). On the text variant it is a plain trailing icon.
 * [icon] is a leading icon; `tintIcon = false` keeps its own colours (Google's "G"). `loading` replaces
 * the leading icon with a spinner and disables the button; `enabled = false` lowers opacity to 55 %.
 */
@Composable
fun NpButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NpButtonVariant = NpButtonVariant.Primary,
    @DrawableRes icon: Int? = null,
    tintIcon: Boolean = true,
    @DrawableRes trailingIcon: Int? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val shape = NpShapes.Field
    val isText = variant == NpButtonVariant.Text
    val minHeight = if (small || isText) NpButtonSizes.Compact else NpButtonSizes.Main
    val colors = colorsFor(variant)
    val hasChip = trailingIcon != null && !isText

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val reducedMotion = rememberReducedMotion()
    val chipShift by animateDpAsState(
        targetValue = if (isPressed && enabled && !loading) 4.dp else 0.dp,
        animationSpec = if (reducedMotion) snap() else tween(NpMotion.FAST_MS, easing = NpMotion.Easing),
        label = "chipShift",
    )

    val horizontalPadding = when {
        isText -> 12.dp
        small -> 16.dp
        else -> 22.dp
    }
    val chipInset = if (small) 6.dp else 7.dp

    Box(
        modifier = modifier
            .heightIn(min = minHeight)
            .alpha(if (enabled) 1f else 0.55f)
            .clip(shape)
            .background(colors.background, shape)
            .then(
                when (variant) {
                    NpButtonVariant.Secondary -> Modifier.border(1.5.dp, colors.border, shape)
                    NpButtonVariant.Danger -> Modifier.border(1.dp, colors.border, shape)
                    else -> Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                enabled = enabled && !loading,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(
                start = horizontalPadding,
                end = if (hasChip) chipInset else horizontalPadding,
                top = if (hasChip) chipInset else 8.dp,
                bottom = if (hasChip) chipInset else 8.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = if (trailingIcon != null) Modifier.fillMaxWidth() else Modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = colors.content,
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (tintIcon) colors.content else Color.Unspecified,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = NpText.Label,
                color = colors.content,
                textAlign = if (hasChip) TextAlign.Start else TextAlign.Center,
                textDecoration = if (isText && isPressed) TextDecoration.Underline else null,
                modifier = if (trailingIcon != null) Modifier.weight(1f) else Modifier,
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(12.dp))
                if (hasChip) {
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(chipShift.roundToPx(), 0) }
                            .size(if (small) 36.dp else 46.dp)
                            .background(colors.chipBackground, shape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(trailingIcon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = colors.chipContent,
                        )
                    }
                } else {
                    Icon(
                        painter = painterResource(trailingIcon),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = colors.content,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpButtonPreview() {
    NetProtectTheme {
        NpButton(text = "Guardar", onClick = {})
    }
}
