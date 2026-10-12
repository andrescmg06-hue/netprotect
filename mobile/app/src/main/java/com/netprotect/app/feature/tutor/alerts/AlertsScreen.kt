package com.netprotect.app.feature.tutor.alerts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.AlertFilter
import com.netprotect.app.feature.tutor.sections.AlertItem
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.feature.tutor.sections.filterAlerts
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.FilterChips
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.SeverityBadge
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Alertas del dispositivo (marcar leída y silenciar, solo lectura del resto). */
@Composable
fun AlertsScreen(
    device: DeviceSummary?,
    alerts: LoadState<List<AlertItem>>,
    filter: AlertFilter,
    busyAlertId: String?,
    actionError: String?,
    silenceTarget: AlertItem?,
    now: Instant,
    onSelectFilter: (AlertFilter) -> Unit,
    onMarkRead: (alertId: String) -> Unit,
    onAskSilence: (alertId: String) -> Unit,
    onConfirmSilence: () -> Unit,
    onDismissSilence: () -> Unit,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            NpTopBar(onBack = onBack)
            Text(text = "Alertas", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(12.dp))
            if (device != null) {
                val relative = device.lastSeenAt?.let { raw ->
                    runCatching { RelativeTime.format(Instant.parse(raw), now, ZoneId.systemDefault()) }.getOrNull()
                }
                SectionDeviceHeader(
                    device = device,
                    lines = listOf(
                        relative?.let { "Última actualización: $it" } ?: "Sin comunicación registrada todavía.",
                        "Las alertas pueden aparecer en el siguiente reporte.",
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            when (alerts) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = alerts.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    FilterChips(
                        options = AlertFilter.entries.map { it.label },
                        selectedIndex = filter.ordinal,
                        onSelect = { onSelectFilter(AlertFilter.entries[it]) },
                    )
                    Spacer(Modifier.height(12.dp))
                    if (actionError != null) {
                        InfoBanner(
                            title = "No se pudo completar",
                            text = actionError,
                            tone = NpTone.Danger,
                            icon = NpIcons.CircleAlert,
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    val visible = filterAlerts(alerts.value, filter)
                    when {
                        alerts.value.isEmpty() -> EmptyState(
                            icon = NpIcons.Bell,
                            title = "Sin alertas para este dispositivo.",
                            message = "Aparecerán aquí cuando el dispositivo reporte algo que requiera tu atención.",
                        )
                        visible.isEmpty() -> Text(
                            text = "No hay alertas en este filtro.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                        else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            visible.forEach { item ->
                                AlertCard(
                                    item = item,
                                    busyAlertId = busyAlertId,
                                    onMarkRead = onMarkRead,
                                    onAskSilence = onAskSilence,
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Las alertas se generan cuando el dispositivo envía su siguiente reporte y pueden tener un retraso de algunos minutos.",
            )
            Spacer(Modifier.height(24.dp))
        }

        if (silenceTarget != null) {
            ConfirmDialog(
                title = "¿Silenciar «${silenceTarget.title}»?",
                message = "No volverás a recibir esta alerta de este dispositivo. Puedes quitar el silencio desde el panel web.",
                confirmLabel = "Silenciar",
                dismissLabel = "Cancelar",
                onConfirm = onConfirmSilence,
                onDismiss = onDismissSilence,
                danger = true,
            )
        }
    }
}

@Composable
private fun AlertCard(
    item: AlertItem,
    busyAlertId: String?,
    onMarkRead: (String) -> Unit,
    onAskSilence: (String) -> Unit,
) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SeverityBadge(level = item.level)
            Spacer(Modifier.weight(1f))
            if (item.unread) {
                StatusPill(text = "No leída", tone = NpTone.Info, showDot = false)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(text = item.title, style = NpText.Title, color = NpColors.ShieldNavy)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(NpIcons.Clock),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = NpColors.SlateMuted,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = item.time,
                style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                color = NpColors.SlateMuted,
            )
            if (item.repeated != null) {
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = painterResource(NpIcons.RefreshCw),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = NpColors.SlateMuted,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = item.repeated,
                    style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                    color = NpColors.SlateMuted,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.unread) {
                NpButton(
                    text = "Marcar leída",
                    onClick = { onMarkRead(item.id) },
                    modifier = Modifier.weight(1f),
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.Check,
                    small = true,
                    // No spinner: while any action is sent every button is disabled, and the
                    // controller doesn't say which action is running (a spinner here would also
                    // spin while silencing).
                    enabled = busyAlertId == null,
                )
            }
            if (item.silenced) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    StatusPill(text = "Silenciada", tone = NpTone.Neutral, showDot = false)
                }
            } else {
                NpButton(
                    text = "Silenciar",
                    onClick = { onAskSilence(item.id) },
                    modifier = Modifier.weight(1f),
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.BellOff,
                    small = true,
                    enabled = busyAlertId == null,
                )
            }
        }
    }
}
