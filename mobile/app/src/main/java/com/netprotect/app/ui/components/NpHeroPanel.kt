package com.netprotect.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import java.util.Locale

/**
 * Text colours on the navy panel. Kept here (not in the theme) because they only make sense over
 * [NpColors.Navy]; `HeroPanelContrastTest` checks each against AA (4.5:1) once composited over navy.
 */
internal object HeroPanelColors {
    val Headline = NpColors.Paper
    val Summary = NpColors.Cream.copy(alpha = 0.86f)
    val Eyebrow = NpColors.Cream.copy(alpha = 0.72f)

    /** Inactive seal arcs are softened on navy so "no recent report" reads as dormant, not alarming. */
    val InactiveArc = NpColors.HairlineStrong.copy(alpha = 0.5f)
}

/**
 * Identity panel of the supervised home (lámina 03): navy, flat (no elevation), square corners, full width.
 * Eyebrow row (small shield + [eyebrow] uppercase), serif [headline], the [NpSeal] with its [sealCaption]
 * (uppercase) on the right, a [summary] paragraph and an optional [action] (e.g. "Revisar permisos").
 *
 * The headline sits beside the seal only while its longest word fits there; otherwise (large font scale,
 * long headline) the seal moves under it, so nothing is cut or broken letter by letter. No text has a
 * fixed height.
 *
 * The panel does **not** pad for the status bar: the caller adds `Modifier.statusBarsPadding()` (or the
 * equivalent inset) inside the navy area, e.g. by passing it through [modifier] after the background, or
 * by placing the panel under a navy inset spacer.
 */
@Composable
fun NpHeroPanel(
    eyebrow: String,
    headline: String,
    sealStates: List<SealArcState>,
    sealCaption: String,
    summary: String,
    modifier: Modifier = Modifier,
    sealContentDescription: String? = null,
    horizontalPadding: Dp = NpSpace.Gutter,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NpColors.Navy)
            .padding(horizontal = horizontalPadding)
            .padding(top = NpSpace.Xl, bottom = NpSpace.Xl),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(NpIcons.LogoShield),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(NpSpace.S))
            Text(
                text = eyebrow.uppercase(Locale.ROOT),
                style = NpText.Eyebrow,
                color = HeroPanelColors.Eyebrow,
            )
        }
        Spacer(Modifier.height(NpSpace.L))
        HeadlineBesideSeal(
            gap = NpSpace.L,
            headline = {
                Text(
                    text = headline,
                    style = NpText.Display,
                    color = HeroPanelColors.Headline,
                    modifier = Modifier.semantics { heading() },
                )
            },
            seal = {
                Column(horizontalAlignment = Alignment.End) {
                    NpSeal(
                        states = sealStates,
                        contentDescription = sealContentDescription,
                        inactiveColor = HeroPanelColors.InactiveArc,
                    )
                    Spacer(Modifier.height(NpSpace.S))
                    Text(
                        text = sealCaption.uppercase(Locale.ROOT),
                        style = NpText.Eyebrow,
                        color = HeroPanelColors.Eyebrow,
                        textAlign = TextAlign.End,
                    )
                }
            },
        )
        Spacer(Modifier.height(NpSpace.M))
        Text(text = summary, style = NpText.Body, color = HeroPanelColors.Summary)
        if (action != null) {
            Spacer(Modifier.height(NpSpace.Xl))
            Box(Modifier.fillMaxWidth()) { action() }
        }
    }
}

/**
 * Places [headline] and [seal] side by side (headline vertically centred on the seal column) when the
 * headline's longest word fits in the space left by the seal; otherwise stacks them, seal aligned to the
 * end. Deciding on the min intrinsic width (the longest word) is what keeps a 200 % headline from being
 * squeezed into a narrow column.
 */
@Composable
private fun HeadlineBesideSeal(
    gap: Dp,
    headline: @Composable () -> Unit,
    seal: @Composable () -> Unit,
) {
    Layout(
        content = {
            headline()
            seal()
        },
    ) { measurables, constraints ->
        val maxWidth = constraints.maxWidth
        val headlineMeasurable = measurables[0]
        val sealPlaceable = measurables[1].measure(Constraints(maxWidth = maxWidth))
        val gapPx = gap.roundToPx()
        val besideWidth = maxWidth - sealPlaceable.width - gapPx
        val fitsBeside = besideWidth > 0 &&
            headlineMeasurable.minIntrinsicWidth(Constraints.Infinity) <= besideWidth

        if (fitsBeside) {
            val headlinePlaceable = headlineMeasurable.measure(Constraints(maxWidth = besideWidth))
            val height = maxOf(headlinePlaceable.height, sealPlaceable.height)
            layout(maxWidth, height) {
                headlinePlaceable.placeRelative(0, (height - headlinePlaceable.height) / 2)
                sealPlaceable.placeRelative(maxWidth - sealPlaceable.width, (height - sealPlaceable.height) / 2)
            }
        } else {
            val headlinePlaceable = headlineMeasurable.measure(Constraints(maxWidth = maxWidth))
            val height = headlinePlaceable.height + gapPx + sealPlaceable.height
            layout(maxWidth, height) {
                headlinePlaceable.placeRelative(0, 0)
                sealPlaceable.placeRelative(maxWidth - sealPlaceable.width, headlinePlaceable.height + gapPx)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NpHeroPanelPreview() {
    NetProtectTheme {
        NpHeroPanel(
            eyebrow = "Dispositivo supervisado",
            headline = "Falta un paso",
            sealStates = sealStatesFor(readyCount = 2),
            sealCaption = permissionsCaption(2),
            summary = "Faltan 2 permisos para que NetProtect funcione bien en este teléfono.",
            action = { NpButton("Revisar permisos", {}, Modifier.fillMaxWidth(), trailingIcon = NpIcons.ArrowRight) },
        )
    }
}
