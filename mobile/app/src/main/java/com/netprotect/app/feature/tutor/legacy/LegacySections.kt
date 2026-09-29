package com.netprotect.app.feature.tutor.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.auth.TokenSession
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.network.AlertsClient
import com.netprotect.app.core.network.AuditClient
import com.netprotect.app.core.network.AuditLogEntry
import com.netprotect.app.core.network.DeviceAlert
import com.netprotect.app.core.network.DeviceStatistics
import com.netprotect.app.core.network.HistoryClient
import com.netprotect.app.core.network.HistoryEvent
import com.netprotect.app.core.network.StatisticsClient
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.feature.tutor.DeviceSection
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/* Sprint 44: the seven sections of the pre-redesign TutorScreen.kt (apps, ubicación, geocercas,
 * historial, estadísticas, alertas, auditoría), moved here VERBATIM — same composables, same texts,
 * same dark look — so splitting the monolith loses nothing. Only the loaders at the bottom are new:
 * they run on navigation what the old screen ran when a section was expanded. Sprints 45–47 replace
 * these sections one by one with the redesigned screens and delete them from this file. */

private sealed interface HistoryState {
    data object Loading : HistoryState
    // Read-only, same as GeofenceState: this screen only displays the unified timeline the
    // backend already merges (bloqueos + entradas/salidas de geocercas, Sprint 15).
    data class Loaded(val events: List<HistoryEvent>) : HistoryState
    data class Error(val message: String) : HistoryState
}

private sealed interface StatisticsState {
    data object Loading : StatisticsState
    // Read-only, same as HistoryState: aggregates the backend already computes (Sprint 16), one
    // period ("today"/"7d"/"30d") at a time.
    data class Loaded(val period: String, val stats: DeviceStatistics) : StatisticsState
    data class Error(val message: String) : StatisticsState
}

private sealed interface AlertsState {
    data object Loading : AlertsState
    // Read-only, same as HistoryState/StatisticsState: the inbox the backend already generates
    // and deduplicates (Sprint 17). Marcar leída/silenciar son sólo del panel web.
    data class Loaded(val alerts: List<DeviceAlert>) : AlertsState
    data class Error(val message: String) : AlertsState
}

private sealed interface AuditState {
    data object Loading : AuditState
    // Account-level, not per-device (backend/app/api/v1/endpoints/audit.py scopes it to
    // actor_user_id == current_user.id, not to a device) — the tutor's own audited actions.
    // Read-only and filter-less here, same split as history/alerts: filtering and CSV export
    // live only in the web panel.
    data class Loaded(val logs: List<AuditLogEntry>) : AuditState
    data class Error(val message: String) : AuditState
}

/** Sprint 15, read-only: a single chronological list merging what were already two separate
 * event logs (bloqueos de reglas, entradas/salidas de geocercas) — the backend already sorts
 * and merges them (GET /devices/{id}/history), so this just renders what it returns.
 */
@Composable
private fun HistorySection(state: HistoryState?) {
    when (state) {
        null, HistoryState.Loading -> Text("Cargando historial…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is HistoryState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is HistoryState.Loaded -> {
            if (state.events.isEmpty()) {
                Text(
                    "Todavía no hay eventos registrados para este dispositivo.",
                    color = Color(0xFFABB5C4),
                    fontSize = 13.sp,
                )
            } else {
                Column {
                    state.events.forEach { event -> HistoryEventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun HistoryEventRow(event: HistoryEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val label = if (event.eventType == "APP_RULE") {
            "Bloqueo (${event.ruleTypeApplied}) de ${event.packageName}"
        } else {
            val verb = if (event.geofenceEventType == "ENTER") "Entró a" else "Salió de"
            "$verb ${event.geofenceName}"
        }
        Text(label, color = Color.White, fontSize = 13.sp)
        Text(formatCapturedAt(event.occurredAt), color = Color(0xFF7D899A), fontSize = 11.sp)
    }
}

private val STATISTICS_PERIODS = listOf("today" to "Hoy", "7d" to "7 días", "30d" to "30 días")

private fun formatSeconds(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours} h ${minutes} min"
        minutes > 0 -> "$minutes min"
        else -> "< 1 min"
    }
}

/** Sprint 16, read-only: renders the aggregates the backend already computes (apps más usadas,
 * por categoría, bloqueos y cumplimiento de límites diarios) for whichever period is selected —
 * same read-only split already established for geocercas/historial (creating rules stays
 * web-only).
 */
@Composable
private fun StatisticsSection(state: StatisticsState?, onChangePeriod: (String) -> Unit) {
    Column {
        Row {
            STATISTICS_PERIODS.forEach { (value, label) ->
                val isSelected = (state as? StatisticsState.Loaded)?.period == value
                TextButton(onClick = { onChangePeriod(value) }, enabled = !isSelected) {
                    Text(label)
                }
            }
        }
        when (state) {
            null, StatisticsState.Loading ->
                Text("Cargando estadísticas…", color = Color(0xFFABB5C4), fontSize = 13.sp)
            is StatisticsState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
            is StatisticsState.Loaded -> {
                val stats = state.stats
                Text("Apps más usadas", color = Color(0xFF7D899A), fontSize = 11.sp)
                if (stats.topApps.isEmpty()) {
                    Text("Sin datos de uso.", color = Color(0xFFABB5C4), fontSize = 13.sp)
                } else {
                    stats.topApps.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.appLabel ?: entry.packageName, color = Color.White, fontSize = 13.sp)
                            Text(formatSeconds(entry.totalSeconds), color = Color(0xFFABB5C4), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Bloqueos", color = Color(0xFF7D899A), fontSize = 11.sp)
                if (stats.blocksByReason.isEmpty()) {
                    Text("Ninguno en este periodo.", color = Color(0xFFABB5C4), fontSize = 13.sp)
                } else {
                    stats.blocksByReason.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.ruleTypeApplied, color = Color.White, fontSize = 13.sp)
                            Text("${entry.count}", color = Color(0xFFABB5C4), fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Cumplimiento de límites diarios", color = Color(0xFF7D899A), fontSize = 11.sp)
                if (stats.compliance.isEmpty()) {
                    Text("Sin reglas de límite diario.", color = Color(0xFFABB5C4), fontSize = 13.sp)
                } else {
                    stats.compliance.forEach { entry ->
                        val rate = entry.complianceRate
                        val rateText = if (rate == null) "sin datos" else "${(rate * 100).toInt()}%"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.packageName ?: entry.category ?: "?", color = Color.White, fontSize = 13.sp)
                            Text(
                                "$rateText (${entry.daysCompliant}/${entry.daysEvaluated} días)",
                                color = Color(0xFFABB5C4),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun alertLabel(alert: DeviceAlert): String = when (alert.alertType) {
    "APP_BLOCKED" -> "Se bloqueó ${alert.packageName}"
    "APP_LIMIT_REACHED" -> "Se alcanzó el límite de tiempo de ${alert.packageName}"
    "GEOFENCE_EXIT" -> "Salió de ${alert.geofenceName}"
    "GEOFENCE_ENTER" -> "Entró a ${alert.geofenceName}"
    // Sprint 20 — señales de manipulación: describen el estado del dispositivo, no una app ni
    // una zona, así que no usan packageName/geofenceName.
    "PERMISSION_REVOKED" -> "Sin permiso de acceso a uso: no puede aplicar reglas"
    "SERVICE_INACTIVE" -> "El servicio de control de apps no está en ejecución"
    "HEARTBEAT_SILENCE" -> "Dejó de reportarse durante un periodo anormalmente largo"
    "CLOCK_TAMPERING" -> "La hora del dispositivo no coincide con la del servidor"
    "UNINSTALL_ATTEMPT" -> "Se intentó desactivar la protección contra desinstalación"
    else -> alert.alertType
}

/** Sprint 17, read-only: shows the tutor inbox the backend already generates and deduplicates
 * from bloqueos de reglas y entradas/salidas de geocercas. Marcar leída/silenciar quedan sólo en
 * el panel web, mismo criterio de sólo-lectura ya usado para historial/estadísticas/geocercas.
 */
@Composable
private fun AlertsSection(state: AlertsState?) {
    when (state) {
        null, AlertsState.Loading -> Text("Cargando alertas…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is AlertsState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is AlertsState.Loaded -> {
            if (state.alerts.isEmpty()) {
                Text("Sin alertas para este dispositivo.", color = Color(0xFFABB5C4), fontSize = 13.sp)
            } else {
                Column {
                    state.alerts.forEach { alert ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val suffix = if (alert.occurrenceCount > 1) " (x${alert.occurrenceCount})" else ""
                            Text(
                                "[${alert.level}] ${alertLabel(alert)}$suffix",
                                color = Color.White,
                                fontSize = 13.sp,
                            )
                            Text(formatCapturedAt(alert.lastOccurredAt), color = Color(0xFF7D899A), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Sprint 22, read-only and account-level (not per-device): the tutor's own audited actions
 * (backend scopes GET /users/me/audit to actor_user_id == current_user.id). No filters or CSV
 * export here — those live only in the web panel, same split already used for historial/
 * alertas/geocercas.
 */
@Composable
private fun AuditSection(state: AuditState) {
    when (state) {
        AuditState.Loading -> Text("Cargando auditoría…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is AuditState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is AuditState.Loaded -> {
            if (state.logs.isEmpty()) {
                Text("Sin acciones registradas todavía.", color = Color(0xFFABB5C4), fontSize = 13.sp)
            } else {
                Column {
                    state.logs.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val resource = entry.resourceType?.let { " · $it" } ?: ""
                            Text("${entry.action}$resource", color = Color.White, fontSize = 13.sp)
                            Text(formatCapturedAt(entry.createdAt), color = Color(0xFF7D899A), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCapturedAt(isoInstant: String): String = runCatching {
    Instant.parse(isoInstant)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(isoInstant)


// ---------------------------------------------------------------------------------------------
// Sprint 44: loaders for the sections above. Each one is the exact load the old TutorScreen did
// when the section was expanded (same client call, same error mapping), now run when the route
// opens. Removed one by one as Sprints 45–47 redesign the sections.
// ---------------------------------------------------------------------------------------------

@Composable
private fun LegacyFrame(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/** One device section with its pre-redesign look (dark), until its own sprint replaces it. */
@Composable
fun LegacyDeviceSectionScreen(
    section: DeviceSection,
    deviceId: String,
    baseUrl: String,
    session: TokenSession,
) {
    when (section) {
        // Sprint 45: rediseñadas; TutorShell las enruta a sus pantallas.
        DeviceSection.Apps, DeviceSection.Location, DeviceSection.Geofences -> Unit
        DeviceSection.History -> {
            val client = remember { HistoryClient(baseUrl) }
            var state by remember(deviceId) { mutableStateOf<HistoryState>(HistoryState.Loading) }
            LaunchedEffect(deviceId) {
                state = try {
                    HistoryState.Loaded(session.authorized { token -> client.listHistory(token, deviceId) })
                } catch (exception: Exception) {
                    HistoryState.Error(exception.toUiError().message)
                }
            }
            LegacyFrame("Historial") { HistorySection(state) }
        }
        DeviceSection.Statistics -> {
            val client = remember { StatisticsClient(baseUrl) }
            var period by rememberSaveable(deviceId) { mutableStateOf("today") }
            var state by remember(deviceId) { mutableStateOf<StatisticsState>(StatisticsState.Loading) }
            LaunchedEffect(deviceId, period) {
                state = StatisticsState.Loading
                state = try {
                    StatisticsState.Loaded(period, session.authorized { token -> client.getStatistics(token, deviceId, period) })
                } catch (exception: Exception) {
                    StatisticsState.Error(exception.toUiError().message)
                }
            }
            LegacyFrame("Estadísticas") { StatisticsSection(state) { period = it } }
        }
        DeviceSection.Alerts -> {
            val client = remember { AlertsClient(baseUrl) }
            var state by remember(deviceId) { mutableStateOf<AlertsState>(AlertsState.Loading) }
            LaunchedEffect(deviceId) {
                state = try {
                    AlertsState.Loaded(session.authorized { token -> client.listAlerts(token, deviceId) })
                } catch (exception: Exception) {
                    AlertsState.Error(exception.toUiError().message)
                }
            }
            LegacyFrame("Alertas") { AlertsSection(state) }
        }
    }
}

/** The tutor's own audit log (tab "Actividad"), pre-redesign look until Sprint 47. */
@Composable
fun LegacyActivityScreen(baseUrl: String, session: TokenSession) {
    val client = remember { AuditClient(baseUrl) }
    var state by remember { mutableStateOf<AuditState>(AuditState.Loading) }
    LaunchedEffect(Unit) {
        state = try {
            AuditState.Loaded(session.authorized { token -> client.listMyAuditLog(token) })
        } catch (exception: Exception) {
            AuditState.Error(exception.toUiError().message)
        }
    }
    LegacyFrame("Mi actividad (auditoría)") { AuditSection(state) }
}
