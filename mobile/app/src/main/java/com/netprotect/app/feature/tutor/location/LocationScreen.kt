package com.netprotect.app.feature.tutor.location

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.feature.tutor.sections.LocationView
import com.netprotect.app.feature.tutor.sections.SectionDeviceHeader
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.format.eventTimeLabel
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/** Ubicación del dispositivo (solo lectura; sin mapa, coordenadas ni nombre de lugar). */
@Composable
fun LocationScreen(
    device: DeviceSummary?,
    location: LoadState<LocationView>,
    now: Instant,
    onRefresh: () -> Unit,
    onOpenMap: () -> Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    var mapMissing by rememberSaveable { mutableStateOf(false) }
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
            Text(text = "Ubicación", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(12.dp))
            if (device != null) {
                val relative = device.lastSeenAt?.let { raw ->
                    runCatching { RelativeTime.format(Instant.parse(raw), now, zone) }.getOrNull()
                }
                SectionDeviceHeader(
                    device = device,
                    lines = listOf(
                        relative?.let { "Última comunicación: $it" } ?: "Sin comunicación registrada todavía.",
                        "Los datos se sincronizan periódicamente.",
                    ),
                )
                Spacer(Modifier.height(16.dp))
            }
            when (location) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = location.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    val view = location.value
                    val report = view.report
                    if (report == null) {
                        EmptyState(
                            icon = NpIcons.MapPin,
                            title = "Sin ubicación todavía",
                            message = "Este dispositivo todavía no ha reportado su ubicación.",
                        )
                    } else {
                        LocationCard(
                            report = report,
                            insideGeofence = view.insideGeofence,
                            today = now.atZone(zone).toLocalDate(),
                            zone = zone,
                            now = now,
                            mapMissing = mapMissing,
                            onOpenMap = { mapMissing = !onOpenMap() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "La ubicación es aproximada y se sincroniza periódicamente. Puede tener un retraso de algunos minutos.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LocationCard(
    report: LocationReport,
    insideGeofence: String?,
    today: java.time.LocalDate,
    zone: ZoneId,
    now: Instant,
    mapMissing: Boolean,
    onOpenMap: () -> Unit,
) {
    val relative = runCatching { RelativeTime.format(Instant.parse(report.capturedAt), now, zone) }.getOrNull()
    val capitalized = relative?.replaceFirstChar { it.uppercase() } ?: report.capturedAt
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = NpIcons.MapPin, tone = NpTone.Purple, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Última ubicación conocida", style = NpText.Title, color = NpColors.ShieldNavy)
                Text(text = capitalized, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (insideGeofence != null) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    painter = painterResource(NpIcons.ShieldCheck),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NpColors.Success,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(text = "Dentro de «$insideGeofence»", style = NpText.BodyStrong, color = NpColors.ShieldNavy)
                    Text(
                        text = "Según la última lectura y su precisión.",
                        style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                        color = NpColors.SlateMuted,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(NpIcons.Clock),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Última lectura: ${eventTimeLabel(report.capturedAt, today, zone)}",
                style = NpText.Body,
                color = NpColors.Ink,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(NpIcons.LocateFixed),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Precisión aproximada: ~${report.accuracyMeters.roundToInt()} m",
                style = NpText.Body,
                color = NpColors.Ink,
            )
        }
        Spacer(Modifier.height(16.dp))
        NpButton(
            text = "Abrir en mapa",
            onClick = onOpenMap,
            modifier = Modifier.fillMaxWidth(),
            icon = NpIcons.Map,
            trailingIcon = NpIcons.ExternalLink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Se abrirá tu aplicación de mapas con la última ubicación conocida del dispositivo.",
            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
            color = NpColors.SlateMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (mapMissing) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "No hay una aplicación de mapas en este teléfono.",
                style = NpText.Body,
                color = NpColors.DangerText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
