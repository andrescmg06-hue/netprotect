package com.netprotect.app.feature.tutor.geofences

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.sections.GeofenceItem
import com.netprotect.app.feature.tutor.sections.GeofencesView
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.SectionHeader
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.components.TimelineItem
import com.netprotect.app.ui.format.eventTimeLabel
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

/** Geocercas del dispositivo (solo lectura). */
@Composable
fun GeofencesScreen(
    device: DeviceSummary?,
    geofences: LoadState<GeofencesView>,
    today: LocalDate,
    now: Instant,
    onRefresh: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
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
            Text(text = "Geocercas", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(12.dp))
            if (device != null) {
                SectionDeviceHeader(
                    device = device,
                    lines = listOf(
                        "Detección aproximada · ~15 min",
                        "Las entradas y salidas se detectan en el siguiente reporte.",
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            SectionHeader(
                title = "Geocercas registradas",
                actionLabel = "Actualizar",
                actionIcon = NpIcons.RefreshCw,
                onAction = onRefresh,
            )
            Spacer(Modifier.height(8.dp))
            when (geofences) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = geofences.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    val view = geofences.value
                    if (view.items.isEmpty()) {
                        EmptyState(
                            icon = NpIcons.Map,
                            title = "No hay geocercas configuradas",
                            message = "Se crean desde el panel web.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            view.items.forEach { item -> GeofenceCard(item, today, zone) }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SectionHeader(title = "Historial de entradas/salidas")
                    Spacer(Modifier.height(8.dp))
                    NpCard {
                        if (view.history.isEmpty()) {
                            Text(
                                text = "Sin entradas ni salidas registradas.",
                                style = NpText.Body,
                                color = NpColors.SlateMuted,
                            )
                        } else {
                            Column {
                                view.history.forEachIndexed { index, event ->
                                    TimelineItem(
                                        icon = if (event.eventType == "ENTER") NpIcons.LogIn else NpIcons.LogOut,
                                        tone = if (event.eventType == "ENTER") NpTone.Success else NpTone.Warning,
                                        title = if (event.eventType == "ENTER") "Entró · ${event.geofenceName}" else "Salió · ${event.geofenceName}",
                                        time = eventTimeLabel(event.occurredAt, today, zone),
                                        isLast = index == view.history.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Las entradas y salidas de las geocercas se detectan en el siguiente reporte del dispositivo y pueden tener un retraso de algunos minutos (~15 min).",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GeofenceCard(item: GeofenceItem, today: LocalDate, zone: ZoneId) {
    NpCard {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon = NpIcons.MapPin, tone = NpTone.Info, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.geofence.name, style = NpText.Title, color = NpColors.ShieldNavy)
                Text(
                    text = "Radio ${item.geofence.radiusMeters.roundToInt()} m",
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val eventType = item.lastEvent?.eventType
                    StatusPill(
                        text = when (eventType) {
                            "ENTER" -> "Último evento: Entró"
                            "EXIT" -> "Último evento: Salió"
                            else -> "Sin actividad"
                        },
                        tone = when (eventType) {
                            "ENTER" -> NpTone.Success
                            "EXIT" -> NpTone.Warning
                            else -> NpTone.Neutral
                        },
                    )
                    if (item.lastEvent != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = eventTimeLabel(item.lastEvent.occurredAt, today, zone),
                            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                            color = NpColors.SlateMuted,
                        )
                    }
                }
            }
        }
    }
}
