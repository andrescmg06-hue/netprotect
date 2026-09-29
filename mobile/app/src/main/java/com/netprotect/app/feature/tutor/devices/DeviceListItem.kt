package com.netprotect.app.feature.tutor.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.DeviceStatusLabels
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

private const val NO_ACTIVITY = "Sin actividad registrada todavía."

private fun lastActivityText(device: DeviceSummary, now: Instant): String {
    val relative = device.lastSeenAt?.let { raw ->
        runCatching { RelativeTime.format(Instant.parse(raw), now, ZoneId.systemDefault()) }.getOrNull()
    }
    return relative?.let { "Última actividad: $it" } ?: NO_ACTIVITY
}

private fun platformLabel(device: DeviceSummary): String =
    device.osVersion?.let { "Android $it" } ?: "Android"

/** Un dispositivo en la lista (compartido por Inicio y Dispositivos). */
@Composable
fun DeviceListItem(
    device: DeviceSummary,
    now: Instant,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (statusLabel, statusTone) = DeviceStatusLabels.label(device.status)
    NpCard(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon = NpIcons.Smartphone, tone = NpTone.Info, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, style = NpText.Title, color = NpColors.ShieldNavy)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = platformLabel(device), style = NpText.Body, color = NpColors.SlateMuted)
                    Spacer(Modifier.width(8.dp))
                    StatusPill(text = statusLabel, tone = statusTone, showDot = false)
                }
                Text(
                    text = lastActivityText(device, now),
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
                Text(
                    text = "Los datos se sincronizan periódicamente.",
                    style = NpText.Caption,
                    color = NpColors.SlateMuted,
                )
            }
            Icon(
                painter = painterResource(NpIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
        }
    }
}

/** Los cuatro estados de la lista de dispositivos (compartido por Inicio y Dispositivos). */
@Composable
internal fun DeviceList(
    devices: LoadState<List<DeviceSummary>>,
    now: Instant,
    onOpenDevice: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (devices) {
        LoadState.Loading -> LoadingState(modifier = modifier)
        is LoadState.Failed -> ErrorState(message = devices.message, onRetry = onRetry, modifier = modifier)
        is LoadState.Loaded -> {
            if (devices.value.isEmpty()) {
                EmptyState(
                    icon = NpIcons.Smartphone,
                    title = "Todavía no hay dispositivos vinculados.",
                    message = "Genera un código de vinculación desde Inicio para añadir uno.",
                    modifier = modifier,
                )
            } else {
                Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    devices.value.forEach { device ->
                        DeviceListItem(
                            device = device,
                            now = now,
                            onClick = { onOpenDevice(device.id) },
                        )
                    }
                }
            }
        }
    }
}
