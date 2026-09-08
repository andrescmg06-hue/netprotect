package com.netprotect.app.feature.tutor

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.AlertsClient
import com.netprotect.app.core.network.DeviceAlert
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.Geofence
import com.netprotect.app.core.network.GeofenceClient
import com.netprotect.app.core.network.GeofenceEvent
import com.netprotect.app.core.network.HistoryClient
import com.netprotect.app.core.network.HistoryEvent
import com.netprotect.app.core.network.LocationClient
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.core.network.PairingClient
import com.netprotect.app.core.network.PairingCode
import com.netprotect.app.core.network.DeviceStatistics
import com.netprotect.app.core.network.StatisticsClient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

private sealed interface DevicesState {
    data object Loading : DevicesState
    data class Loaded(val devices: List<DeviceSummary>) : DevicesState
    data class Error(val message: String) : DevicesState
}

private sealed interface AppsState {
    data object Loading : AppsState
    data class Loaded(val apps: List<DeviceApplicationSummary>) : AppsState
    data class Error(val message: String) : AppsState
}

private sealed interface LocationState {
    data object Loading : LocationState
    // report == null means the device has never reported a location, or every report has aged
    // out of the backend's retention window (backend/app/schemas/location.py) — both look the
    // same to a tutor and are shown with the same "sin ubicación reciente" message.
    data class Loaded(val report: LocationReport?) : LocationState
    data class Error(val message: String) : LocationState
}

private sealed interface GeofenceState {
    data object Loading : GeofenceState
    // Read-only here — creating/editing a geofence is web-only (Sprint 14), same split already
    // established for reglas/categorías (Sprint 8-10): this screen only shows what the tutor
    // already configured on the web panel, plus the ENTER/EXIT history detected server-side.
    data class Loaded(val geofences: List<Geofence>, val events: List<GeofenceEvent>) : GeofenceState
    data class Error(val message: String) : GeofenceState
}

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

@Composable
fun TutorScreen(
    baseUrl: String,
    accessToken: String,
    onSignOut: suspend () -> Unit,
    onSwitchMode: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pairingClient = remember { PairingClient(baseUrl) }
    val deviceClient = remember { DeviceClient(baseUrl) }
    val applicationsClient = remember { ApplicationsClient(baseUrl) }
    val locationClient = remember { LocationClient(baseUrl) }
    val geofenceClient = remember { GeofenceClient(baseUrl) }
    val historyClient = remember { HistoryClient(baseUrl) }
    val statisticsClient = remember { StatisticsClient(baseUrl) }
    val alertsClient = remember { AlertsClient(baseUrl) }

    var devicesState by remember { mutableStateOf<DevicesState>(DevicesState.Loading) }
    var activeCode by remember { mutableStateOf<PairingCode?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var renamingDeviceId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var expandedAppsDeviceId by remember { mutableStateOf<String?>(null) }
    val appsStateByDevice = remember { mutableStateMapOf<String, AppsState>() }
    var expandedLocationDeviceId by remember { mutableStateOf<String?>(null) }
    val locationStateByDevice = remember { mutableStateMapOf<String, LocationState>() }
    var expandedGeofenceDeviceId by remember { mutableStateOf<String?>(null) }
    val geofenceStateByDevice = remember { mutableStateMapOf<String, GeofenceState>() }
    var expandedHistoryDeviceId by remember { mutableStateOf<String?>(null) }
    val historyStateByDevice = remember { mutableStateMapOf<String, HistoryState>() }
    var expandedStatisticsDeviceId by remember { mutableStateOf<String?>(null) }
    val statisticsStateByDevice = remember { mutableStateMapOf<String, StatisticsState>() }
    val statisticsPeriodByDevice = remember { mutableStateMapOf<String, String>() }
    var expandedAlertsDeviceId by remember { mutableStateOf<String?>(null) }
    val alertsStateByDevice = remember { mutableStateMapOf<String, AlertsState>() }

    suspend fun reloadDevices() {
        devicesState = try {
            DevicesState.Loaded(deviceClient.listDevices(accessToken))
        } catch (exception: Exception) {
            DevicesState.Error(exception.message ?: "No se pudo cargar la lista")
        }
    }

    fun toggleApps(deviceId: String) {
        if (expandedAppsDeviceId == deviceId) {
            expandedAppsDeviceId = null
            return
        }
        expandedAppsDeviceId = deviceId
        appsStateByDevice[deviceId] = AppsState.Loading
        scope.launch {
            appsStateByDevice[deviceId] = try {
                AppsState.Loaded(applicationsClient.getApplications(accessToken, deviceId))
            } catch (exception: Exception) {
                AppsState.Error(exception.message ?: "No se pudo cargar la lista de apps")
            }
        }
    }

    fun toggleLocation(deviceId: String) {
        if (expandedLocationDeviceId == deviceId) {
            expandedLocationDeviceId = null
            return
        }
        expandedLocationDeviceId = deviceId
        locationStateByDevice[deviceId] = LocationState.Loading
        scope.launch {
            locationStateByDevice[deviceId] = try {
                LocationState.Loaded(locationClient.getLatestLocation(accessToken, deviceId))
            } catch (exception: Exception) {
                LocationState.Error(exception.message ?: "No se pudo cargar la ubicación")
            }
        }
    }

    fun toggleGeofences(deviceId: String) {
        if (expandedGeofenceDeviceId == deviceId) {
            expandedGeofenceDeviceId = null
            return
        }
        expandedGeofenceDeviceId = deviceId
        geofenceStateByDevice[deviceId] = GeofenceState.Loading
        scope.launch {
            geofenceStateByDevice[deviceId] = try {
                GeofenceState.Loaded(
                    geofences = geofenceClient.listGeofences(accessToken, deviceId),
                    events = geofenceClient.listGeofenceEvents(accessToken, deviceId),
                )
            } catch (exception: Exception) {
                GeofenceState.Error(exception.message ?: "No se pudieron cargar las geocercas")
            }
        }
    }

    fun toggleHistory(deviceId: String) {
        if (expandedHistoryDeviceId == deviceId) {
            expandedHistoryDeviceId = null
            return
        }
        expandedHistoryDeviceId = deviceId
        historyStateByDevice[deviceId] = HistoryState.Loading
        scope.launch {
            historyStateByDevice[deviceId] = try {
                HistoryState.Loaded(historyClient.listHistory(accessToken, deviceId))
            } catch (exception: Exception) {
                HistoryState.Error(exception.message ?: "No se pudo cargar el historial")
            }
        }
    }

    fun loadStatistics(deviceId: String, period: String) {
        statisticsPeriodByDevice[deviceId] = period
        statisticsStateByDevice[deviceId] = StatisticsState.Loading
        scope.launch {
            statisticsStateByDevice[deviceId] = try {
                StatisticsState.Loaded(period, statisticsClient.getStatistics(accessToken, deviceId, period))
            } catch (exception: Exception) {
                StatisticsState.Error(exception.message ?: "No se pudieron cargar las estadísticas")
            }
        }
    }

    fun toggleStatistics(deviceId: String) {
        if (expandedStatisticsDeviceId == deviceId) {
            expandedStatisticsDeviceId = null
            return
        }
        expandedStatisticsDeviceId = deviceId
        loadStatistics(deviceId, statisticsPeriodByDevice[deviceId] ?: "today")
    }

    fun toggleAlerts(deviceId: String) {
        if (expandedAlertsDeviceId == deviceId) {
            expandedAlertsDeviceId = null
            return
        }
        expandedAlertsDeviceId = deviceId
        alertsStateByDevice[deviceId] = AlertsState.Loading
        scope.launch {
            alertsStateByDevice[deviceId] = try {
                AlertsState.Loaded(alertsClient.listAlerts(accessToken, deviceId))
            } catch (exception: Exception) {
                AlertsState.Error(exception.message ?: "No se pudieron cargar las alertas")
            }
        }
    }

    LaunchedEffect(Unit) { reloadDevices() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Modo Tutor",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onSwitchMode) { Text("Cambiar de modo") }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121722),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val code = activeCode
                if (code == null) {
                    Text("Vincula un dispositivo nuevo generando un código temporal.", color = Color(0xFFABB5C4))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                codeError = null
                                try {
                                    activeCode = pairingClient.generateCode(accessToken)
                                } catch (exception: Exception) {
                                    codeError = exception.message ?: "No se pudo generar el código"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                    ) {
                        Text("Generar código de vinculación")
                    }
                } else {
                    Text("Código de vinculación", color = Color(0xFF6BE3BF), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(code.code, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Válido por ${code.expiresInSeconds / 60} minutos. Uso único.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { pairingClient.revokeCurrentCode(accessToken) }
                                activeCode = null
                            }
                        }) { Text("Revocar", color = Color(0xFFFFB4AB)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { scope.launch { reloadDevices() } }) {
                            Text("¿Ya se vinculó? Actualizar lista")
                        }
                    }
                }
                codeError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color(0xFFFFB4AB), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Dispositivos vinculados", color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = { scope.launch { reloadDevices() } }) { Text("Actualizar") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (val state = devicesState) {
            DevicesState.Loading -> Text("Cargando…", color = Color(0xFFABB5C4))
            is DevicesState.Error -> Text(state.message, color = Color(0xFFFFB4AB))
            is DevicesState.Loaded -> {
                if (state.devices.isEmpty()) {
                    Text("Todavía no hay dispositivos vinculados.", color = Color(0xFFABB5C4))
                } else {
                    LazyColumn {
                        items(state.devices, key = { it.id }) { device ->
                            DeviceRow(
                                device = device,
                                isRenaming = renamingDeviceId == device.id,
                                renameText = renameText,
                                onRenameTextChange = { renameText = it },
                                onStartRename = {
                                    renamingDeviceId = device.id
                                    renameText = device.name
                                },
                                onConfirmRename = {
                                    scope.launch {
                                        runCatching {
                                            deviceClient.renameDevice(accessToken, device.id, renameText)
                                        }
                                        renamingDeviceId = null
                                        reloadDevices()
                                    }
                                },
                                onCancelRename = { renamingDeviceId = null },
                                onUnlink = {
                                    scope.launch {
                                        runCatching { pairingClient.unlinkDevice(accessToken, device.id) }
                                        reloadDevices()
                                    }
                                },
                                isAppsExpanded = expandedAppsDeviceId == device.id,
                                appsState = appsStateByDevice[device.id],
                                onToggleApps = { toggleApps(device.id) },
                                isLocationExpanded = expandedLocationDeviceId == device.id,
                                locationState = locationStateByDevice[device.id],
                                onToggleLocation = { toggleLocation(device.id) },
                                isGeofenceExpanded = expandedGeofenceDeviceId == device.id,
                                geofenceState = geofenceStateByDevice[device.id],
                                onToggleGeofences = { toggleGeofences(device.id) },
                                isHistoryExpanded = expandedHistoryDeviceId == device.id,
                                historyState = historyStateByDevice[device.id],
                                onToggleHistory = { toggleHistory(device.id) },
                                isStatisticsExpanded = expandedStatisticsDeviceId == device.id,
                                statisticsState = statisticsStateByDevice[device.id],
                                onToggleStatistics = { toggleStatistics(device.id) },
                                onChangeStatisticsPeriod = { period ->
                                    loadStatistics(device.id, period)
                                },
                                isAlertsExpanded = expandedAlertsDeviceId == device.id,
                                alertsState = alertsStateByDevice[device.id],
                                onToggleAlerts = { toggleAlerts(device.id) },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { scope.launch { onSignOut() } }) {
            Text("Cerrar sesión", color = Color(0xFFABB5C4))
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceSummary,
    isRenaming: Boolean,
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    onStartRename: () -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onUnlink: () -> Unit,
    isAppsExpanded: Boolean,
    appsState: AppsState?,
    onToggleApps: () -> Unit,
    isLocationExpanded: Boolean,
    locationState: LocationState?,
    onToggleLocation: () -> Unit,
    isGeofenceExpanded: Boolean,
    geofenceState: GeofenceState?,
    onToggleGeofences: () -> Unit,
    isHistoryExpanded: Boolean,
    historyState: HistoryState?,
    onToggleHistory: () -> Unit,
    isStatisticsExpanded: Boolean,
    statisticsState: StatisticsState?,
    onToggleStatistics: () -> Unit,
    onChangeStatisticsPeriod: (String) -> Unit,
    isAlertsExpanded: Boolean,
    alertsState: AlertsState?,
    onToggleAlerts: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121722),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRenaming) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = onRenameTextChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onConfirmRename) { Text("Guardar") }
                    TextButton(onClick = onCancelRename) { Text("Cancelar") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            device.timezone?.let { "${device.platform} · $it" } ?: device.platform,
                            color = Color(0xFF7D899A),
                            fontSize = 12.sp,
                        )
                    }
                    StatusPill(status = device.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    TextButton(onClick = onStartRename) { Text("Renombrar") }
                    TextButton(onClick = onUnlink) { Text("Desvincular", color = Color(0xFFFFB4AB)) }
                    TextButton(onClick = onToggleApps) {
                        Text(if (isAppsExpanded) "Ocultar apps" else "Ver apps")
                    }
                    TextButton(onClick = onToggleLocation) {
                        Text(if (isLocationExpanded) "Ocultar ubicación" else "Ver ubicación")
                    }
                    TextButton(onClick = onToggleGeofences) {
                        Text(if (isGeofenceExpanded) "Ocultar geocercas" else "Ver geocercas")
                    }
                    TextButton(onClick = onToggleHistory) {
                        Text(if (isHistoryExpanded) "Ocultar historial" else "Ver historial")
                    }
                    TextButton(onClick = onToggleStatistics) {
                        Text(if (isStatisticsExpanded) "Ocultar estadísticas" else "Ver estadísticas")
                    }
                    TextButton(onClick = onToggleAlerts) {
                        Text(if (isAlertsExpanded) "Ocultar alertas" else "Ver alertas")
                    }
                }
                if (isAppsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AppsList(appsState)
                }
                if (isLocationExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LocationSection(locationState)
                }
                if (isGeofenceExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    GeofenceSection(geofenceState)
                }
                if (isHistoryExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HistorySection(historyState)
                }
                if (isStatisticsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StatisticsSection(statisticsState, onChangeStatisticsPeriod)
                }
                if (isAlertsExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    AlertsSection(alertsState)
                }
            }
        }
    }
}

/** Text-only by design: this project's Android client never embeds a map view (no Maps SDK
 * dependency, no GOOGLE_MAPS_ANDROID_API_KEY) — see docs/sprint-13.md. "Abrir en mapa" hands the
 * coordinates to whatever map app is already installed via a plain geo: intent, which needs no
 * API key of its own. The web panel is the one that renders an embedded map, since a browser has
 * no equivalent app to delegate to.
 */
@Composable
private fun LocationSection(state: LocationState?) {
    val context = LocalContext.current
    when (state) {
        null, LocationState.Loading -> Text("Cargando ubicación…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is LocationState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is LocationState.Loaded -> {
            val report = state.report
            if (report == null) {
                Text(
                    "Todavía no hay ubicación reciente de este dispositivo.",
                    color = Color(0xFFABB5C4),
                    fontSize = 13.sp,
                )
            } else {
                Column {
                    Text(
                        "Lat ${"%.5f".format(report.latitude)}, Lng ${"%.5f".format(report.longitude)}" +
                            " (±${report.accuracyMeters.toInt()} m)",
                        color = Color.White,
                        fontSize = 13.sp,
                    )
                    Text(
                        "Capturada: ${formatCapturedAt(report.capturedAt)}",
                        color = Color(0xFF7D899A),
                        fontSize = 11.sp,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val uri = Uri.parse("geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}")
                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
                        },
                    ) {
                        Text("Abrir en mapa")
                    }
                }
            }
        }
    }
}

/** Read-only, same reasoning as the file-level GeofenceState docstring: creating/editing a
 * geofence happens on the web panel only. Shows the zones the tutor already configured there,
 * plus the ENTER/EXIT history the backend detected from consecutive location reports.
 */
@Composable
private fun GeofenceSection(state: GeofenceState?) {
    when (state) {
        null, GeofenceState.Loading -> Text("Cargando geocercas…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is GeofenceState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is GeofenceState.Loaded -> {
            Column {
                if (state.geofences.isEmpty()) {
                    Text(
                        "Todavía no hay geocercas. Créalas desde el panel web.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                    )
                } else {
                    state.geofences.forEach { geofence ->
                        Text(
                            "${geofence.name} · radio ${geofence.radiusMeters.toInt()} m",
                            color = Color.White,
                            fontSize = 13.sp,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Historial de entradas/salidas", color = Color(0xFF7D899A), fontSize = 11.sp)
                if (state.events.isEmpty()) {
                    Text(
                        "Todavía no se detectó ninguna entrada o salida.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                    )
                } else {
                    state.events.forEach { event -> GeofenceEventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun GeofenceEventRow(event: GeofenceEvent) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        val verb = if (event.eventType == "ENTER") "Entró a" else "Salió de"
        Text("$verb ${event.geofenceName}", color = Color.White, fontSize = 13.sp)
        Text(formatCapturedAt(event.occurredAt), color = Color(0xFF7D899A), fontSize = 11.sp)
    }
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

private fun formatCapturedAt(isoInstant: String): String = runCatching {
    Instant.parse(isoInstant)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(isoInstant)

@Composable
private fun AppsList(state: AppsState?) {
    when (state) {
        null, AppsState.Loading -> Text("Cargando apps…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is AppsState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is AppsState.Loaded -> {
            if (state.apps.isEmpty()) {
                Text(
                    "Todavía no se sincronizó ninguna app desde este dispositivo.",
                    color = Color(0xFFABB5C4),
                    fontSize = 13.sp,
                )
            } else {
                val sorted = state.apps.sortedByDescending { it.latestUsageSeconds ?: -1 }
                Column {
                    sorted.forEach { app -> AppUsageRow(app) }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(app: DeviceApplicationSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(app.appLabel, color = Color.White, fontSize = 14.sp)
            if (app.uninstalledAt != null) {
                Text("Desinstalada", color = Color(0xFF7D899A), fontSize = 11.sp)
            }
        }
        Text(
            text = app.latestUsageSeconds?.let(::formatUsageDuration) ?: "Sin datos de uso",
            color = Color(0xFFABB5C4),
            fontSize = 12.sp,
        )
    }
}

private fun formatUsageDuration(totalSeconds: Int): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "${hours} h ${minutes} min"
        minutes > 0 -> "$minutes min"
        else -> "< 1 min"
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "ONLINE" -> Color(0xFF6BE3BF)
        "OFFLINE" -> Color(0xFF7D899A)
        "ALERT" -> Color(0xFFFFB4AB)
        else -> Color(0xFFABB5C4)
    }
    Text(status, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
