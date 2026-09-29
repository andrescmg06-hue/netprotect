package com.netprotect.app.feature.tutor.apps

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
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant
import java.time.LocalDate
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.feature.tutor.sections.sortApps
import com.netprotect.app.ui.format.usageLabel

/** INTERIM (Sprint 45, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
@Composable
fun AppsScreen(
    device: DeviceSummary?,
    apps: LoadState<List<DeviceApplicationSummary>>,
    today: LocalDate,
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
        Text("Apps del dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
        NpButton("Actualizar", onRefresh, variant = NpButtonVariant.Text)
        when (apps) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(apps.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> sortApps(apps.value).forEach { app ->
                val status = if (app.uninstalledAt != null) " · Desinstalada" else ""
                Text("${app.appLabel} — ${usageLabel(app.latestUsageSeconds, app.latestUsageDate, today)}$status", style = NpText.Body)
            }
        }
    }
}
