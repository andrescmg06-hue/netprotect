package com.netprotect.app.feature.tutor.apps

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.feature.tutor.sections.filterApps
import com.netprotect.app.feature.tutor.sections.sortApps
import com.netprotect.app.ui.components.AppIcon
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.format.usageLabel
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Apps del dispositivo (solo lectura). */
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
    var query by rememberSaveable { mutableStateOf("") }
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
            Text(text = "Apps del dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(12.dp))
            if (device != null) {
                val relative = device.lastSeenAt?.let { raw ->
                    runCatching { RelativeTime.format(Instant.parse(raw), now, ZoneId.systemDefault()) }.getOrNull()
                }
                SectionDeviceHeader(
                    device = device,
                    lines = listOf(
                        relative?.let { "Uso sincronizado periódicamente · última actualización $it" }
                            ?: "Uso sincronizado periódicamente",
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            NpCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        placeholder = { Text("Buscar aplicación…") },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(NpIcons.Search),
                                contentDescription = null,
                                tint = NpColors.SlateMuted,
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NpColors.SignalBlue,
                            unfocusedBorderColor = NpColors.HairlineStrong,
                            errorBorderColor = NpColors.Danger,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    NpButton(
                        text = "Actualizar",
                        onClick = onRefresh,
                        variant = NpButtonVariant.Secondary,
                        icon = NpIcons.RefreshCw,
                        small = true,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            when (apps) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = apps.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    if (apps.value.isEmpty()) {
                        EmptyState(
                            icon = NpIcons.LayoutGrid,
                            title = "Sin apps sincronizadas",
                            message = "Todavía no se sincronizó ninguna app desde este dispositivo.",
                        )
                    } else {
                        val visible = filterApps(sortApps(apps.value), query)
                        if (visible.isEmpty()) {
                            Text(
                                text = "Ninguna app coincide con «$query».",
                                style = NpText.Body,
                                color = NpColors.SlateMuted,
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                visible.forEach { app -> AppRow(app, today) }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Los tiempos de uso se sincronizan periódicamente y pueden tener un retraso de algunos minutos.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AppRow(app: DeviceApplicationSummary, today: LocalDate) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppIcon(packageName = app.packageName, label = app.appLabel, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = app.appLabel, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
                Text(
                    text = usageLabel(app.latestUsageSeconds, app.latestUsageDate, today),
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
            }
            if (app.uninstalledAt != null) {
                StatusPill(text = "Desinstalada", tone = NpTone.Danger, showDot = false)
            }
        }
    }
}
