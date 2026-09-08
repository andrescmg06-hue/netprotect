package com.netprotect.app.feature.supervised

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.DeviceIdentity
import com.netprotect.app.core.auth.LinkedDeviceStore
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.location.LocationReportingService
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.PairingClient
import com.netprotect.app.core.permissions.DeviceAdminPermission
import com.netprotect.app.core.permissions.LocationPermission
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.rules.EnforcementLiveness
import com.netprotect.app.core.rules.RuleEnforcementService
import com.netprotect.app.core.sync.SyncWorker
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HEARTBEAT_INTERVAL_MS = 60_000L
private const val APP_SYNC_INTERVAL_MS = 5 * 60_000L

private sealed interface SupervisedState {
    data object CheckingLink : SupervisedState
    data class EnteringCode(val error: String? = null) : SupervisedState
    data class Linked(val tutorLabel: String) : SupervisedState
}

@Composable
fun SupervisedScreen(
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
    val deviceInstanceId = remember { DeviceIdentity.getOrCreate(context) }

    var state by remember { mutableStateOf<SupervisedState>(SupervisedState.CheckingLink) }
    var codeInput by remember { mutableStateOf("") }
    var hasUsageAccess by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var hasLocationPermission by remember { mutableStateOf(LocationPermission.isGranted(context)) }
    var hasDeviceAdmin by remember { mutableStateOf(DeviceAdminPermission.isActive(context)) }

    // Sprint 20: la administración del dispositivo se concede en una pantalla del sistema, igual
    // que el acceso a uso — pero aquí sí existe un intent con resultado, así que no hace falta el
    // botón de "ya lo activé, verificar de nuevo" que necesita UsageAccessPermission.
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasDeviceAdmin = DeviceAdminPermission.isActive(context) }

    // ACCESS_COARSE_LOCATION is an ordinary runtime permission (unlike PACKAGE_USAGE_STATS
    // above): the system dialog itself grants or denies it, no trip to Settings needed. Shown
    // only after the rationale card below — never fired automatically — following the
    // educational-UI pattern from docs/android/capability-matrix.md (Sprint 13).
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    // Not required for the enforcement service to run (see RuleEnforcementService/
    // capability-matrix.md) — asked anyway because the notification is deliberately visible,
    // not something to hide. No-op callback: whether the user grants it or not, the flow
    // continues the same way.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(state) {
        if (state is SupervisedState.Linked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // The local cache is only a UI shortcut. On every start we ask the server who this
    // account's device actually is: a different Google account signing into the same phone,
    // or a tutor unlinking it remotely, would otherwise leave a stale cache pointing at a
    // device_id that no longer belongs to this session.
    LaunchedEffect(Unit) {
        val cached = LinkedDeviceStore.read(context)
        state = try {
            val mine = deviceClient.getMyDevice(accessToken)
            val tutorLabel = mine?.tutorLabel
            // The device row can outlive every tutor link (all of them unlinked): /devices/me
            // still answers 200 in that case, with an empty tutor list. That is not "linked" —
            // treat it the same as no device at all, so the user can redeem a new code instead
            // of getting stuck on a "linked, but to no one" screen with no way out.
            if (mine != null && tutorLabel != null) {
                LinkedDeviceStore.write(context, mine.deviceId, tutorLabel)
                SupervisedState.Linked(tutorLabel)
            } else {
                LinkedDeviceStore.clear(context)
                SupervisedState.EnteringCode()
            }
        } catch (exception: Exception) {
            // Couldn't reach the server (offline, timeout): trust the cache rather than
            // forcing this device to re-pair just because of a transient network hiccup.
            cached?.let { SupervisedState.Linked(it.tutorLabel) } ?: SupervisedState.EnteringCode()
        }
    }

    // Sends a heartbeat immediately once linked, then every HEARTBEAT_INTERVAL_MS while this
    // screen stays composed — a tighter cadence than SyncWorker's 15-minute background floor
    // (below) can offer, so this keeps running whenever the app happens to be in the foreground
    // instead of stepping aside for it.
    LaunchedEffect(state) {
        val linked = state as? SupervisedState.Linked ?: return@LaunchedEffect
        val deviceId = LinkedDeviceStore.read(context)?.deviceId ?: return@LaunchedEffect
        while (true) {
            runCatching {
                deviceClient.sendHeartbeat(
                    accessToken,
                    deviceId,
                    Build.VERSION.RELEASE,
                    BuildConfig.VERSION_NAME,
                    java.util.TimeZone.getDefault().id,
                    // Sprint 20: read fresh on every beat, never cached — the whole point is
                    // noticing the moment one of them changes (see app/services/tamper.py).
                    usageAccessGranted = UsageAccessPermission.isGranted(context),
                    serviceActive = EnforcementLiveness.isRecentlyActive(context),
                    deviceTime = Instant.now(),
                )
            }
            delay(HEARTBEAT_INTERVAL_MS)
        }
    }

    // Same foreground-only approach as the heartbeat above, on a longer interval since
    // reading the full app list and today's usage is heavier than a plain ping. Gated on
    // hasUsageAccess: the app list alone needs no permission, but sending it without usage
    // numbers would be a half-finished sync, so both wait for the same signal.
    LaunchedEffect(state, hasUsageAccess) {
        state as? SupervisedState.Linked ?: return@LaunchedEffect
        if (!hasUsageAccess) return@LaunchedEffect
        val deviceId = LinkedDeviceStore.read(context)?.deviceId ?: return@LaunchedEffect
        while (true) {
            runCatching {
                applicationsClient.syncApplications(
                    accessToken = accessToken,
                    deviceId = deviceId,
                    usageDate = AppInventoryCollector.todayDateString(),
                    installedApps = AppInventoryCollector.collectInstalledApps(context),
                    dailyUsage = AppInventoryCollector.collectTodayUsage(context),
                )
            }
            delay(APP_SYNC_INTERVAL_MS)
        }
    }

    // Unlike the two loops above, this one must keep running while this screen is NOT in the
    // foreground — the whole point is catching when the supervised user opens some other app.
    // A foreground service, not a LaunchedEffect, is what makes that possible; see
    // RuleEnforcementService. Same hasUsageAccess gate as the sync above: foreground detection
    // uses the same PACKAGE_USAGE_STATS-backed API.
    DisposableEffect(state, hasUsageAccess) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && hasUsageAccess && deviceId != null) {
            RuleEnforcementService.start(context, BuildConfig.API_BASE_URL, accessToken, deviceId)
        }
        onDispose { RuleEnforcementService.stop(context) }
    }

    // Independent gate from the rule-enforcement service above: reporting location doesn't need
    // usage access, and enforcing rules doesn't need location — each foreground service is
    // started/stopped only by the permission it actually depends on. Same foreground-start
    // requirement applies here (see LocationReportingService's own doc comment): this effect
    // only ever fires while SupervisedScreen itself is composed, i.e. the app in the foreground.
    DisposableEffect(state, hasLocationPermission) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && hasLocationPermission && deviceId != null) {
            LocationReportingService.start(context, BuildConfig.API_BASE_URL, accessToken, deviceId)
        }
        onDispose { LocationReportingService.stop(context) }
    }

    // Sprint 19: the background counterpart to the heartbeat/app-sync loops above — same data,
    // sent by SyncWorker instead, on WorkManager's own schedule (15-minute floor, requires
    // connectivity) so it keeps happening while this screen isn't composed. No permission gate:
    // SyncWorker checks usage-access itself before syncing applications, and a heartbeat needs
    // none at all.
    DisposableEffect(state) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && deviceId != null) {
            SyncWorker.schedule(context, BuildConfig.API_BASE_URL, deviceId)
        }
        onDispose { SyncWorker.cancel(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Text(
            text = "Modo Supervisado",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121722),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                when (val current = state) {
                    SupervisedState.CheckingLink -> {
                        Text("Comprobando vínculo…", color = Color.White)
                    }

                    is SupervisedState.EnteringCode -> {
                        Text(
                            "Introduce el código de 6 dígitos que te dio tu tutor.",
                            color = Color(0xFFABB5C4),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = codeInput,
                            onValueChange = { if (it.length <= 6) codeInput = it.filter(Char::isDigit) },
                            label = { Text("Código") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        current.error?.let {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(it, color = Color(0xFFFFB4AB), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                scope.launch {
                                    try {
                                        val result = pairingClient.redeem(
                                            accessToken = accessToken,
                                            code = codeInput,
                                            deviceInstanceId = deviceInstanceId,
                                            deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                                            osVersion = Build.VERSION.RELEASE,
                                            appVersion = BuildConfig.VERSION_NAME,
                                        )
                                        val label = result.tutor.displayName ?: result.tutor.email
                                        LinkedDeviceStore.write(context, result.deviceId, label)
                                        state = SupervisedState.Linked(label)
                                    } catch (exception: Exception) {
                                        state = SupervisedState.EnteringCode(
                                            exception.message ?: "No se pudo vincular"
                                        )
                                    }
                                }
                            },
                            enabled = codeInput.length == 6,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                        ) {
                            Text("Vincular dispositivo")
                        }
                    }

                    is SupervisedState.Linked -> {
                        Text("Dispositivo vinculado", color = Color(0xFF6BE3BF), fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Supervisado por ${current.tutorLabel}", color = Color(0xFFABB5C4))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Este dispositivo reporta su estado cada minuto mientras la app esté abierta.",
                            color = Color(0xFF7D899A),
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        if (state is SupervisedState.Linked && !hasUsageAccess) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121722),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Acceso a uso de apps",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Para que tu tutor vea qué apps usas y cuánto tiempo, actívalo en " +
                            "Ajustes → Acceso a datos de uso. Android exige que este permiso se " +
                            "conceda ahí, no aquí.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { UsageAccessPermission.openSettings(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                    ) {
                        Text("Abrir Ajustes")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { hasUsageAccess = UsageAccessPermission.isGranted(context) }) {
                        Text("Ya lo activé, verificar de nuevo", color = Color(0xFFABB5C4))
                    }
                }
            }
        }

        if (state is SupervisedState.Linked && !hasLocationPermission) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121722),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Ubicación aproximada",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Para que tu tutor vea en qué zona está este dispositivo, permite el " +
                            "acceso a la ubicación aproximada. No se usa la ubicación precisa, y " +
                            "sólo se comparte mientras esta app siga activa en segundo plano.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                    ) {
                        Text("Permitir ubicación aproximada")
                    }
                }
            }
        }

        if (state is SupervisedState.Linked && !hasDeviceAdmin) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF121722),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Protección contra desinstalación",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Si lo activas, Android pedirá desactivar esta protección antes de " +
                            "desinstalar NetProtect, y tu tutor recibirá un aviso cuando eso " +
                            "ocurra. No permite borrar el dispositivo ni cambiar tu contraseña, " +
                            "y puedes quitarlo cuando quieras.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            deviceAdminLauncher.launch(DeviceAdminPermission.requestIntent(context))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                    ) {
                        Text("Activar protección")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onSwitchMode) { Text("Cambiar de modo", color = Color(0xFFABB5C4)) }
        TextButton(onClick = { scope.launch { onSignOut() } }) {
            Text("Cerrar sesión", color = Color(0xFFABB5C4))
        }
    }
}
