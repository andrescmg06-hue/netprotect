package com.netprotect.app.feature.tutor.sections

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.DeviceStatusLabels
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Cabecera compacta del dispositivo dentro de una sección (sin tarjeta, sobre el fondo). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SectionDeviceHeader(
    device: DeviceSummary,
    lines: List<String>,
    modifier: Modifier = Modifier,
) {
    val (statusLabel, statusTone) = DeviceStatusLabels.label(device.status)
    Row(modifier = modifier, verticalAlignment = Alignment.Top) {
        IconTile(icon = NpIcons.Smartphone, tone = NpTone.Info, size = 56.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = device.name, style = NpText.Title, color = NpColors.ShieldNavy)
            // FlowRow: at large font scales the pill moves to its own line instead of being clipped.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = device.osVersion?.let { "Android $it" } ?: "Android",
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
                StatusPill(text = statusLabel, tone = statusTone, showDot = false)
            }
            lines.forEach { line ->
                Text(
                    text = line,
                    style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                    color = NpColors.SlateMuted,
                )
            }
        }
    }
}
