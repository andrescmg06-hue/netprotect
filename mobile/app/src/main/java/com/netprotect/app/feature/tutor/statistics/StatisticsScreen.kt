package com.netprotect.app.feature.tutor.statistics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.COMPLIANCE_SUBTITLE
import com.netprotect.app.feature.tutor.sections.BlockReasonItem
import com.netprotect.app.feature.tutor.sections.ComplianceRow
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.feature.tutor.sections.StatsPeriod
import com.netprotect.app.feature.tutor.sections.StatisticsView
import com.netprotect.app.feature.tutor.sections.TopAppRow
import com.netprotect.app.ui.components.AppIcon
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.SegmentedControl
import com.netprotect.app.ui.components.UsageBar
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Estadísticas del dispositivo (solo lectura). */
@Composable
fun StatisticsScreen(
    device: DeviceSummary?,
    period: StatsPeriod,
    statistics: LoadState<StatisticsView>,
    now: Instant,
    onSelectPeriod: (StatsPeriod) -> Unit,
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
            Text(text = "Estadísticas", style = NpText.Display, color = NpColors.ShieldNavy)
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
            SegmentedControl(
                options = StatsPeriod.entries.map { it.label },
                selectedIndex = period.ordinal,
                onSelect = { onSelectPeriod(StatsPeriod.entries[it]) },
            )
            Spacer(Modifier.height(16.dp))
            when (statistics) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = statistics.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    val view = statistics.value
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(
                            icon = NpIcons.LayoutGrid,
                            tone = NpTone.Info,
                            title = "Apps más usadas",
                            subtitle = "Tiempo de uso en este periodo.",
                        ) {
                            if (view.topApps.isEmpty()) {
                                Text(text = "Sin datos de uso.", style = NpText.Body, color = NpColors.SlateMuted)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    view.topApps.forEach { row -> TopAppRowView(row) }
                                }
                            }
                        }
                        StatCard(
                            icon = NpIcons.Ban,
                            tone = NpTone.Danger,
                            title = "Bloqueos",
                            subtitle = "Total de bloqueos en este periodo, por motivo.",
                        ) {
                            if (view.blocks.isEmpty()) {
                                Text(text = "Ninguno en este periodo.", style = NpText.Body, color = NpColors.SlateMuted)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    view.blocks.chunked(3).forEach { chunk ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            chunk.forEach { item -> BlockCell(item, Modifier.weight(1f)) }
                                            repeat(3 - chunk.size) { Spacer(Modifier.weight(1f)) }
                                        }
                                    }
                                }
                            }
                        }
                        StatCard(
                            icon = NpIcons.CircleCheck,
                            tone = NpTone.Success,
                            title = "Cumplimiento de límites diarios",
                            subtitle = COMPLIANCE_SUBTITLE,
                        ) {
                            if (view.compliance.isEmpty()) {
                                Text(text = "Sin reglas de límite diario.", style = NpText.Body, color = NpColors.SlateMuted)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    view.compliance.forEach { row -> ComplianceRowView(row) }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Los datos de uso se sincronizan periódicamente y pueden tener algunos minutos de retraso.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatCard(
    @androidx.annotation.DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    NpCard {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon = icon, tone = tone, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NpText.Title, color = NpColors.ShieldNavy)
                Text(text = subtitle, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
private fun TopAppRowView(row: TopAppRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppIcon(packageName = row.packageName, label = row.name, size = 32.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = row.name,
            style = NpText.Body,
            color = NpColors.Ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp),
        )
        UsageBar(
            fraction = row.fraction,
            modifier = Modifier.weight(1f),
            tone = NpTone.Info,
            contentDescription = "${row.name}: ${row.duration}",
        )
        Spacer(Modifier.width(12.dp))
        Text(text = row.duration, style = NpText.BodyStrong, color = NpColors.Ink)
    }
}

@Composable
private fun BlockCell(item: BlockReasonItem, modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(item.tone.wash, NpShapes.Md).padding(12.dp)) {
        Column {
            Icon(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = item.tone.text,
            )
            Spacer(Modifier.height(8.dp))
            Text(text = item.label, style = NpText.Caption, color = item.tone.text)
            Text(text = "${item.count}", style = NpText.Title, color = NpColors.ShieldNavy)
        }
    }
}

@Composable
private fun ComplianceRowView(row: ComplianceRow) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = row.name, style = NpText.BodyStrong, color = NpColors.Ink, modifier = Modifier.weight(1f))
            Text(text = row.days, style = NpText.Body, color = NpColors.SlateMuted)
        }
        Text(text = row.limit, style = NpText.Caption.copy(fontWeight = FontWeight.Normal), color = NpColors.SlateMuted)
        if (row.fraction != null) {
            Spacer(Modifier.height(6.dp))
            UsageBar(
                fraction = row.fraction,
                modifier = Modifier.fillMaxWidth(),
                tone = NpTone.Success,
                contentDescription = "${row.name}: ${row.days}",
            )
        }
    }
}
