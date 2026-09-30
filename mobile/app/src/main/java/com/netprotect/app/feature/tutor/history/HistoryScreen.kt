package com.netprotect.app.feature.tutor.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.HistoryDay
import com.netprotect.app.feature.tutor.sections.HistoryKind
import com.netprotect.app.feature.tutor.sections.HistoryRow
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.TimelineItem
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.format.RuleLabels
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Historial del dispositivo (bloqueos y entradas/salidas de geocercas, solo lectura). */
@Composable
fun HistoryScreen(
    device: DeviceSummary?,
    history: LoadState<List<HistoryDay>>,
    now: Instant,
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
            Text(text = "Historial", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(12.dp))
            if (device != null) {
                val relative = device.lastSeenAt?.let { raw ->
                    runCatching { RelativeTime.format(Instant.parse(raw), now, ZoneId.systemDefault()) }.getOrNull()
                }
                SectionDeviceHeader(
                    device = device,
                    lines = listOf(
                        relative?.let { "Datos sincronizados periódicamente · última actualización $it" }
                            ?: "Datos sincronizados periódicamente",
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            when (history) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = history.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    if (history.value.isEmpty()) {
                        EmptyState(
                            icon = NpIcons.History,
                            title = "Sin eventos en el historial.",
                            message = "Aquí aparecerán los bloqueos de apps y las entradas y salidas de geocercas.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            history.value.forEach { day -> DayCard(day) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "El historial puede tener retrasos porque los eventos se registran cuando el dispositivo envía su siguiente reporte.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayCard(day: HistoryDay) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = day.title, style = NpText.Title, color = NpColors.ShieldNavy)
            if (day.subtitle != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = day.subtitle, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        day.rows.forEachIndexed { index, row ->
            TimelineItem(
                icon = historyIcon(row),
                tone = historyTone(row),
                title = row.title,
                subtitle = row.subtitle,
                time = row.time,
                isLast = index == day.rows.lastIndex,
            )
        }
    }
}

@Composable
private fun historyIcon(row: HistoryRow): Int = when (row.kind) {
    HistoryKind.Block -> RuleLabels.ruleTypeIcon(row.ruleType ?: "BLOCK")
    HistoryKind.Enter -> NpIcons.LogIn
    HistoryKind.Exit -> NpIcons.LogOut
}

@Composable
private fun historyTone(row: HistoryRow): NpTone = when (row.kind) {
    HistoryKind.Block -> RuleLabels.ruleTypeTone(row.ruleType ?: "BLOCK")
    HistoryKind.Enter -> NpTone.Success
    HistoryKind.Exit -> NpTone.Warning
}
