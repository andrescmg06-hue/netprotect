package com.netprotect.app.feature.tutor.location

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
import com.netprotect.app.feature.tutor.sections.LocationView

/** INTERIM (Sprint 45, Claude): plain but complete, so the section keeps working between
 * commits. DeepSeek replaces this body with the redesign; the signature is final. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Ubicación", style = NpText.Display, color = NpColors.ShieldNavy)
        when (location) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(location.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                val report = location.value.report
                if (report == null) {
                    Text("Este dispositivo todavía no ha reportado su ubicación.", style = NpText.Body)
                } else {
                    Text("Última lectura: ${report.capturedAt}", style = NpText.Body)
                    Text("Precisión aproximada: ~${report.accuracyMeters.toInt()} m", style = NpText.Body)
                    location.value.insideGeofence?.let { Text("Dentro de «$it»", style = NpText.BodyStrong) }
                    NpButton("Abrir en mapa", { onOpenMap() })
                }
            }
        }
    }
}
