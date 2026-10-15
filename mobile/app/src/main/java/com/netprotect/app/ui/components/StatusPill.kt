package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** The six chip states of the v2 design, each with its default Spanish label and semantic tone. */
enum class NpChipStatus(val label: String, val tone: NpTone) {
    Ready("Listo", NpTone.Success),
    Active("Activo", NpTone.Success),
    Pending("Pendiente", NpTone.Warning),
    Linked("Vinculado", NpTone.Success),
    Error("Error", NpTone.Danger),
    Inactive("Inactivo", NpTone.Neutral),
}

private val ChipTextStyle = NpText.BodySm.copy(fontWeight = FontWeight.SemiBold)

/**
 * Status chip: optional 7 dp dot + text in `tone.text`. [filled] puts it on a `tone.wash` pill;
 * `filled = false` is the bare "dot + text" form (e.g. "Vinculado" under the tutor's name).
 *
 * The text wraps at word boundaries onto a new line instead of being clipped or broken letter by
 * letter (S51). Callers should still give the chip its own node in a `FlowRow` next to other text,
 * never a narrow fixed column.
 */
@Composable
fun StatusPill(
    text: String,
    tone: NpTone,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
    filled: Boolean = true,
) {
    Row(
        modifier = modifier
            .heightIn(min = 26.dp)
            .then(if (filled) Modifier.background(tone.wash, NpShapes.Pill) else Modifier)
            .padding(horizontal = if (filled) 10.dp else 0.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(tone.solid, CircleShape),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(text = text, style = ChipTextStyle, color = tone.text)
    }
}

/** [StatusPill] for one of the six design states; [text] defaults to the state's label. */
@Composable
fun StatusPill(
    status: NpChipStatus,
    modifier: Modifier = Modifier,
    text: String = status.label,
    showDot: Boolean = true,
    filled: Boolean = true,
) {
    StatusPill(text = text, tone = status.tone, modifier = modifier, showDot = showDot, filled = filled)
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun StatusPillPreview() {
    NetProtectTheme {
        StatusPill(status = NpChipStatus.Linked)
    }
}
