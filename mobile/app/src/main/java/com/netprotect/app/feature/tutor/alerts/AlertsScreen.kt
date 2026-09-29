package com.netprotect.app.feature.tutor.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.AlertFilter
import com.netprotect.app.feature.tutor.sections.AlertItem
import com.netprotect.app.feature.tutor.sections.filterAlerts
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.components.FilterChips
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

/** INTERIM (Sprint 46, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Alertas", style = NpText.Display, color = NpColors.ShieldNavy)
        FilterChips(
            options = AlertFilter.entries.map { it.label },
            selectedIndex = filter.ordinal,
            onSelect = { onSelectFilter(AlertFilter.entries[it]) },
        )
        actionError?.let { Text(it, style = NpText.Body, color = NpColors.DangerText) }
        when (alerts) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(alerts.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                if (alerts.value.isEmpty()) Text("Sin alertas para este dispositivo.", style = NpText.Body)
                filterAlerts(alerts.value, filter).forEach { item ->
                    Text(
                        listOfNotNull(item.level, item.title, item.time, item.repeated, if (item.unread) "No leída" else null)
                            .joinToString(" · "),
                        style = NpText.Body,
                    )
                    if (item.unread) {
                        NpButton("Marcar como leída", { onMarkRead(item.id) }, enabled = busyAlertId == null, small = true)
                    }
                    if (item.silenced) {
                        Text("Silenciada", style = NpText.Caption)
                    } else {
                        NpButton(
                            "Silenciar",
                            { onAskSilence(item.id) },
                            variant = NpButtonVariant.Secondary,
                            enabled = busyAlertId == null,
                            small = true,
                        )
                    }
                }
            }
        }
    }
    if (silenceTarget != null) {
        ConfirmDialog(
            title = "¿Silenciar esta alerta?",
            message = "No volverás a recibir esta alerta de este dispositivo. Puedes quitar el silencio desde el panel web.",
            confirmLabel = "Silenciar",
            dismissLabel = "Cancelar",
            onConfirm = onConfirmSilence,
            onDismiss = onDismissSilence,
        )
    }
}
