package com.netprotect.app.feature.tutor.history

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
import com.netprotect.app.feature.tutor.sections.HistoryDay
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

/** INTERIM (Sprint 46, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
@Composable
fun HistoryScreen(
    device: DeviceSummary?,
    history: LoadState<List<HistoryDay>>,
    now: Instant,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Historial", style = NpText.Display, color = NpColors.ShieldNavy)
        when (history) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(history.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                if (history.value.isEmpty()) Text("Sin eventos en el historial.", style = NpText.Body)
                history.value.forEach { day ->
                    Text(listOfNotNull(day.title, day.subtitle).joinToString(" · "), style = NpText.Title)
                    day.rows.forEach { row ->
                        Text(listOfNotNull(row.title, row.subtitle, row.time).joinToString(" — "), style = NpText.Body)
                    }
                }
            }
        }
    }
}
