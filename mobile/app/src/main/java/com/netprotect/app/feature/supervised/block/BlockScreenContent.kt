package com.netprotect.app.feature.supervised.block

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.ui.components.AppIcon
import com.netprotect.app.ui.components.HeroPanelColors
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/**
 * Pantalla «App bloqueada» del modo supervisado (lámina 07). Se dibuja como actividad y como overlay
 * del sistema, fuera del shell: solo presentación. Panel navy con la etiqueta, el candado y la ficha de
 * la app; debajo, sobre crema, el motivo y su pista; la nota y «Ir al inicio» quedan fijas abajo y el
 * resto hace scroll por encima.
 */
@Composable
fun BlockScreenContent(
    packageName: String,
    appLabel: String,
    categoryLabel: String?,
    reason: BlockReason,
    onGoHome: () -> Unit,
) {
    val presentation = blockPresentation(reason, categoryLabel)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NpColors.Cream),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            BlockPanel(
                label = presentation.title,
                packageName = packageName,
                appLabel = appLabel,
                categoryLabel = categoryLabel,
            )
            BlockReasonBody(presentation)
        }
        BlockFooter(onGoHome)
    }
}

@Composable
private fun BlockPanel(
    label: String,
    packageName: String,
    appLabel: String,
    categoryLabel: String?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NpColors.Navy)
            // The overlay window may not receive insets, so never less than a fixed top margin.
            .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets(top = NpSpace.Xl)))
            .heightIn(min = 280.dp)
            .padding(start = NpSpace.Xl, end = NpSpace.Xl, top = NpSpace.L, bottom = NpSpace.Xl),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = NpSpace.Xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(NpIcons.Lock),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = HeroPanelColors.Eyebrow,
                )
                Spacer(Modifier.width(NpSpace.S))
                Text(text = label, style = NpText.Eyebrow, color = HeroPanelColors.Eyebrow)
            }
            Spacer(Modifier.width(NpSpace.M))
            BlockPadlock()
        }
        Spacer(Modifier.height(NpSpace.Xxl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .border(1.dp, HeroPanelColors.Eyebrow.copy(alpha = 0.32f), NpShapes.Card)
                    .padding(6.dp),
            ) {
                AppIcon(packageName = packageName, label = appLabel, size = 36.dp)
            }
            Spacer(Modifier.width(NpSpace.M))
            Column(modifier = Modifier.weight(1f)) {
                // No maxLines: at 200 % a long name wraps instead of being cut.
                Text(text = appLabel, style = NpText.Title, color = HeroPanelColors.Headline)
                if (categoryLabel != null) {
                    Text(text = categoryLabel, style = NpText.BodySm, color = HeroPanelColors.Summary)
                }
            }
        }
    }
}

@Composable
private fun BlockReasonBody(presentation: BlockPresentation) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = NpSpace.Xl, end = NpSpace.Xl, top = NpSpace.Xl, bottom = NpSpace.L),
    ) {
        Text(
            text = presentation.message,
            style = NpText.Title,
            color = NpColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        if (presentation.hintAsRow) {
            Spacer(Modifier.height(NpSpace.L))
            HintRow(icon = presentation.hintIcon, text = presentation.hint)
        } else {
            Spacer(Modifier.height(NpSpace.S))
            Text(text = presentation.hint, style = NpText.Body, color = NpColors.InkMuted)
        }
    }
}

/** The clue of the reason between two hairlines (lámina 07). */
@Composable
private fun HintRow(icon: Int, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 1.dp.toPx()
                drawLine(NpColors.Hairline, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
                drawLine(
                    NpColors.Hairline,
                    Offset(0f, size.height - stroke / 2),
                    Offset(size.width, size.height - stroke / 2),
                    stroke,
                )
            }
            .padding(vertical = NpSpace.M),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = NpColors.BlueText,
        )
        Spacer(Modifier.width(NpSpace.M))
        Text(text = text, style = NpText.BodyStrong, color = NpColors.Ink, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun BlockFooter(onGoHome: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NpColors.Cream)
            // As at the top: the overlay may get no insets, so keep a fixed bottom margin at least.
            .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets(bottom = NpSpace.L)))
            .padding(start = NpSpace.Xl, end = NpSpace.Xl, top = NpSpace.S, bottom = NpSpace.S),
    ) {
        Text(text = BLOCK_COVER_NOTE, style = NpText.BodySm, color = NpColors.InkMuted)
        Spacer(Modifier.height(NpSpace.M))
        NpButton(
            text = "Ir al inicio",
            onClick = onGoHome,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = NpIcons.Home,
        )
    }
}
