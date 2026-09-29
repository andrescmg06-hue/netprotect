package com.netprotect.app.feature.tutor.geofences

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
import com.netprotect.app.feature.tutor.sections.GeofencesView

/** INTERIM (Sprint 45, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Geocercas", style = NpText.Display, color = NpColors.ShieldNavy)
        when (geofences) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(geofences.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                geofences.value.items.forEach { item ->
                    Text("${item.geofence.name} · Radio ${item.geofence.radiusMeters.toInt()} m · ${item.lastEvent?.eventType ?: "Sin actividad"}", style = NpText.Body)
                }
                Text("Historial de entradas/salidas", style = NpText.Title)
                geofences.value.history.forEach { event ->
                    Text("${if (event.eventType == "ENTER") "Entró" else "Salió"} · ${event.geofenceName} · ${event.occurredAt}", style = NpText.Body)
                }
            }
        }
    }
}
