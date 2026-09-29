package com.netprotect.app.feature.tutor.statistics

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
import com.netprotect.app.feature.tutor.sections.StatisticsView
import com.netprotect.app.feature.tutor.sections.StatsPeriod
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.SegmentedControl
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

/** INTERIM (Sprint 46, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Estadísticas", style = NpText.Display, color = NpColors.ShieldNavy)
        SegmentedControl(
            options = StatsPeriod.entries.map { it.label },
            selectedIndex = period.ordinal,
            onSelect = { onSelectPeriod(StatsPeriod.entries[it]) },
        )
        when (statistics) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(statistics.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                val view = statistics.value
                Text("Apps más usadas", style = NpText.Title)
                if (view.topApps.isEmpty()) Text("Sin datos de uso.", style = NpText.Body)
                view.topApps.forEach { Text("${it.name} — ${it.duration}", style = NpText.Body) }
                Text("Bloqueos", style = NpText.Title)
                if (view.blocks.isEmpty()) Text("Ninguno en este periodo.", style = NpText.Body)
                view.blocks.forEach { Text("${it.label}: ${it.count}", style = NpText.Body) }
                Text("Cumplimiento de límites diarios", style = NpText.Title)
                if (view.compliance.isEmpty()) Text("Sin reglas de límite diario.", style = NpText.Body)
                view.compliance.forEach { Text("${it.name} · ${it.limit} · ${it.days}", style = NpText.Body) }
            }
        }
    }
}
