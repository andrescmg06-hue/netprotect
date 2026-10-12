package com.netprotect.app.feature.supervised

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.DeviceIdentity
import com.netprotect.app.core.auth.LinkedDeviceStore
import com.netprotect.app.core.auth.TokenSession
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.location.LocationReportingService
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.MyDeviceInfo
import com.netprotect.app.core.network.PairingClient
import com.netprotect.app.core.network.ReconnectingRealtimeChannel
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.core.permissions.DeviceAdminPermission
import com.netprotect.app.core.permissions.LocationPermission
import com.netprotect.app.core.permissions.OverlayPermission
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.rules.EnforcementLiveness
import com.netprotect.app.core.rules.RuleEnforcementService
import com.netprotect.app.core.screenshare.ScreenCapture
import com.netprotect.app.core.screenshare.ScreenShareService
import com.netprotect.app.core.status.ServiceStatusRegistry
import com.netprotect.app.core.status.servicesView
import com.netprotect.app.core.sync.SyncWorker
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentScreen
import com.netprotect.app.feature.supervised.services.ServicesStatusScreen
import com.netprotect.app.ui.components.ActiveShareBanner
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.theme.NpColors
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val HEARTBEAT_INTERVAL_MS = 60_000L
private const val APP_SYNC_INTERVAL_MS = 5 * 60_000L

/** Sprint 50: a request nobody answers closes by itself — the backend doesn't tell the device when
 * the tutor just closes the tab. Nothing is sent: a "no" is never sent on the child's behalf. */
private const val CONSENT_TIMEOUT_MS = 2 * 60_000L

private sealed interface SupervisedState {
    data object CheckingLink : SupervisedState
    data class EnteringCode(val error: String? = null) : SupervisedState
    data class Linked(val tutorLabel: String) : SupervisedState
}

/**
 * Sprint 48: the supervised mode's container. Everything that keeps the device reporting and
 * enforcing lives here, moved **unchanged** from the old SupervisedScreen (Sprints 7–41): the
 * heartbeat loop, the app sync loop, RuleEnforcementService, LocationReportingService, the
 * realtime socket for screen-share requests, SyncWorker and the permission launchers.
 *
 * The one rule that keeps them alive: every effect is keyed on [SupervisedState] and the
 * permission flags, and **nothing else**. Navigation ([SupervisedRoute]) and what the screens
 * display ([MyDeviceInfo], last heartbeat) are separate state that no effect reads as a key, and
 * this composable never leaves the composition while the mode is on — so going from "Dispositivo
 * vinculado" to "Permisos" and back never disposes a service. Do not put anything that changes
 * over time inside `SupervisedState.Linked`: every change would restart (stop + start) all of them.
 *
 * The screens only receive values and callbacks; none of them starts or stops anything.
 */
@Composable
fun SupervisedShell(
    baseUrl: String,
    session: TokenSession,
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
    var codeInput by rememberSaveable { mutableStateOf("") }
    var hasUsageAccess by remember { mutableStateOf(UsageAccessPermission.isGranted(context)) }
    var hasLocationPermission by remember { mutableStateOf(LocationPermission.isGranted(context)) }
    var hasDeviceAdmin by remember { mutableStateOf(DeviceAdminPermission.isActive(context)) }
    var hasOverlayPermission by remember { mutableStateOf(OverlayPermission.isGranted(context)) }
    // Sprint 23: set from the signalling socket's callback (OkHttp's thread) and read by the
    // composition — a Compose state, so the recomposition happens on its own.
    var screenShareRequested by remember { mutableStateOf(false) }
    var screenShareSignaling by remember { mutableStateOf<ReconnectingRealtimeChannel?>(null) }

    // Sprint 48 — display-only state. Never used as an effect key (see the class comment).
    var route by rememberSaveable { mutableStateOf(SupervisedRoute.Linked) }
    var deviceInfo by remember { mutableStateOf<MyDeviceInfo?>(null) }
    var lastHeartbeatOk by remember { mutableStateOf<Instant?>(null) }
    var heartbeatFailing by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var rechecking by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }

    // Android's own screen-capture consent dialog. Deliberately launched only from the card
    // below, never automatically: the tutor's request is what makes the card appear, and the
    // supervised person is the one who decides. Each session needs this dialog again — the
    // resulting permission is good for exactly one capture (verified in
    // docs/android/capability-matrix.md, Sprint 23), so there is nothing here to reuse silently.
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        screenShareRequested = false
        val data = result.data
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (result.resultCode == android.app.Activity.RESULT_OK && data != null && deviceId != null) {
            ScreenShareService.start(context, BuildConfig.API_BASE_URL, session.currentAccessToken().orEmpty(), deviceId, data)
        } else {
            // The system dialog was dismissed. The tutor is still waiting on a "yes" that will
            // now never produce a stream, so say so rather than leaving them watching a spinner.
            screenShareSignaling?.send(
                JSONObject().put("type", "screen_share_stop").put("reason", "projection_cancelled")
            )
        }
    }

    // Sprint 20: la administración del dispositivo se concede en una pantalla del sistema, igual
    // que el acceso a uso — pero aquí sí existe un intent con resultado.
    val deviceAdminLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasDeviceAdmin = DeviceAdminPermission.isActive(context) }

    // Verified live (17/09/2026): this is what actually shows the block screen while the device
    // is unlocked and in active use — a plain startActivity()/full-screen-intent from
    // RuleEnforcementService gets rejected or degraded to a heads-up banner in that exact
    // situation. Same result-launcher shape as device admin above.
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlayPermission = OverlayPermission.isGranted(context) }

    // ACCESS_COARSE_LOCATION is an ordinary runtime permission: the system dialog itself grants
    // or denies it. Shown only from its card — never fired automatically — following the
    // educational-UI pattern from docs/android/capability-matrix.md (Sprint 13).
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasLocationPermission = granted }

    // Not required for the enforcement service to run — asked anyway because the notification is
    // deliberately visible, not something to hide. No-op callback.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(state) {
        if (state is SupervisedState.Linked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Sprint 48: permissions are re-read whenever the app comes back to the front (e.g. from
    // Settings). A flag only changes if the permission really changed, so an effect keyed on it
    // restarts only then — revoking usage access stops RuleEnforcementService, which cannot work
    // without it anyway; the heartbeat keeps reporting the revocation to the tutor (Sprint 20).
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = UsageAccessPermission.isGranted(context)
                hasLocationPermission = LocationPermission.isGranted(context)
                hasDeviceAdmin = DeviceAdminPermission.isActive(context)
                hasOverlayPermission = OverlayPermission.isGranted(context)
                now = Instant.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The local cache is only a UI shortcut. On every start we ask the server who this
    // account's device actually is: a different Google account signing into the same phone,
    // or a tutor unlinking it remotely, would otherwise leave a stale cache pointing at a
    // device_id that no longer belongs to this session.
    suspend fun checkLink() {
        val cached = LinkedDeviceStore.read(context)
        state = try {
            // Sprint 48 (B-01): say which install this is — one account can own several rows.
            val mine = session.authorized { token -> deviceClient.getMyDevice(token, deviceInstanceId) }
            deviceInfo = mine
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
    LaunchedEffect(Unit) { checkLink() }

    // Sends a heartbeat immediately once linked, then every HEARTBEAT_INTERVAL_MS while this
    // shell stays composed — a tighter cadence than SyncWorker's 15-minute background floor
    // (below) can offer, so this keeps running whenever the app happens to be in the foreground
    // instead of stepping aside for it.
    LaunchedEffect(state) {
        val linked = state as? SupervisedState.Linked ?: return@LaunchedEffect
        val deviceId = LinkedDeviceStore.read(context)?.deviceId ?: return@LaunchedEffect
        while (true) {
            val sent = runCatching {
                session.authorized { token ->
                    deviceClient.sendHeartbeat(
                        token,
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
            }.isSuccess
            // Sprint 48: only what "Última comunicación" shows — not a key of any effect.
            now = Instant.now()
            if (sent) {
                lastHeartbeatOk = now
                ServiceStatusRegistry.heartbeatSucceeded(now)
            }
            heartbeatFailing = !sent
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
                session.authorized { token ->
                    applicationsClient.syncApplications(
                        accessToken = token,
                        deviceId = deviceId,
                        usageDate = AppInventoryCollector.todayDateString(),
                        installedApps = AppInventoryCollector.collectInstalledApps(context),
                        dailyUsage = AppInventoryCollector.collectTodayUsage(context),
                    )
                }
            }
            delay(APP_SYNC_INTERVAL_MS)
        }
    }

    // Unlike the two loops above, this one must keep running while the app is NOT in the
    // foreground — the whole point is catching when the supervised user opens some other app.
    // A foreground service, not a LaunchedEffect, is what makes that possible; see
    // RuleEnforcementService. Same hasUsageAccess gate as the sync above.
    DisposableEffect(state, hasUsageAccess) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && hasUsageAccess && deviceId != null) {
            RuleEnforcementService.start(context, BuildConfig.API_BASE_URL, session.currentAccessToken().orEmpty(), deviceId)
        }
        onDispose { RuleEnforcementService.stop(context) }
    }

    // Independent gate from the rule-enforcement service above: reporting location doesn't need
    // usage access, and enforcing rules doesn't need location. Same foreground-start requirement
    // (see LocationReportingService): this effect only fires while the shell is composed.
    DisposableEffect(state, hasLocationPermission) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && hasLocationPermission && deviceId != null) {
            LocationReportingService.start(context, BuildConfig.API_BASE_URL, session.currentAccessToken().orEmpty(), deviceId)
        }
        onDispose { LocationReportingService.stop(context) }
    }

    // Sprint 23: a second WebSocket, open only while the shell is composed, whose single job is
    // hearing the tutor ask to see the screen. Separate from RuleEnforcementService's connection on
    // purpose (that one is gated on usage access). Once the supervised person accepts,
    // ScreenShareService opens its own connection for the session itself.
    // Sprint 50 (B-02): it now reconnects by itself (growing waits up to 30 s, a fresh valid token
    // as the first frame each time), so a request sent after a network drop is no longer lost.
    DisposableEffect(state) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked == null || deviceId == null) return@DisposableEffect onDispose { }

        val channel = ReconnectingRealtimeChannel(
            baseUrl = BuildConfig.API_BASE_URL,
            deviceId = deviceId,
            tokenProvider = { session.validAccessToken() },
            onConnectedChange = ServiceStatusRegistry::realtimeConnected,
        ) { event, _ ->
            when (event) {
                "screen_share_request" -> screenShareRequested = true
                "screen_share_stop" -> screenShareRequested = false
            }
        }
        screenShareSignaling = channel
        channel.start(scope)
        onDispose {
            channel.stop()
            screenShareSignaling = null
        }
    }

    // Sprint 19: the background counterpart to the heartbeat/app-sync loops above — same data,
    // sent by SyncWorker on WorkManager's own schedule so it keeps happening while the app isn't
    // in the foreground. No permission gate: SyncWorker checks usage access itself.
    DisposableEffect(state) {
        val linked = state as? SupervisedState.Linked
        val deviceId = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && deviceId != null) {
            SyncWorker.schedule(context, BuildConfig.API_BASE_URL, deviceId)
        }
        onDispose { SyncWorker.cancel(context) }
    }

    // Sprint 50: a pending request opens the consent screen, wherever the person is; answering it,
    // the tutor cancelling (screen_share_stop) or CONSENT_TIMEOUT_MS of silence brings them back.
    LaunchedEffect(screenShareRequested) {
        if (screenShareRequested) {
            route = SupervisedRoute.Consent
            delay(CONSENT_TIMEOUT_MS)
            screenShareRequested = false
        } else if (route == SupervisedRoute.Consent) {
            route = SupervisedRoute.Linked
        }
    }
    BackHandler(enabled = state is SupervisedState.Linked && route != SupervisedRoute.Linked) {
        if (route == SupervisedRoute.Consent) {
            // Back is not an answer: only the buttons send one. Leaving just hides the request.
            screenShareRequested = false
        } else {
            route = SupervisedRoute.Linked
        }
    }
    // Sprint 50: "Estado de NetProtect" shows minutes since the last report; refresh while it's open.
    LaunchedEffect(route) {
        while (route == SupervisedRoute.Services) {
            now = Instant.now()
            delay(15_000L)
        }
    }
    val serviceStatus by ServiceStatusRegistry.status.collectAsState()

    val permissions = PermissionsUi(
        usageAccess = hasUsageAccess,
        location = hasLocationPermission,
        overlay = hasOverlayPermission,
        deviceAdmin = hasDeviceAdmin,
    )

    Column(modifier = Modifier.fillMaxSize().background(NpColors.SkyGround)) {
    // Sprint 50: visible on every supervised screen while a share really runs, besides Android's
    // own notification. It takes the status bar inset, so the screen below must not add it again.
    val sharing = serviceStatus.screenShareActive
    if (sharing) ActiveShareBanner(onStop = { ScreenShareService.requestStop(context) })
    Box(
        modifier = Modifier
            .weight(1f)
            .then(if (sharing) Modifier.consumeWindowInsets(WindowInsets.statusBars) else Modifier),
    ) {
        when (val current = state) {
            SupervisedState.CheckingLink ->
                LoadingState(modifier = Modifier.statusBarsPadding(), label = "Comprobando vínculo…")

            is SupervisedState.EnteringCode -> LinkDeviceScreen(
                code = codeInput,
                onCodeChange = { codeInput = sanitizePairingCode(it) },
                linking = linking,
                error = current.error,
                rechecking = rechecking,
                onLink = {
                    if (!linking && isCompletePairingCode(codeInput)) {
                        linking = true
                        scope.launch {
                            try {
                                val result = session.authorized { token ->
                                    pairingClient.redeem(
                                        accessToken = token,
                                        code = codeInput,
                                        deviceInstanceId = deviceInstanceId,
                                        deviceName = "${Build.MANUFACTURER} ${Build.MODEL}",
                                        osVersion = Build.VERSION.RELEASE,
                                        appVersion = BuildConfig.VERSION_NAME,
                                    )
                                }
                                val label = result.tutor.displayName ?: result.tutor.email
                                LinkedDeviceStore.write(context, result.deviceId, label)
                                codeInput = ""
                                route = SupervisedRoute.Linked
                                state = SupervisedState.Linked(label)
                                // Names and last contact for the linked screen; failures just leave
                                // the pairing answer's data on screen.
                                runCatching {
                                    deviceInfo = session.authorized { token -> deviceClient.getMyDevice(token, deviceInstanceId) }
                                }
                            } catch (exception: Exception) {
                                state = SupervisedState.EnteringCode(exception.toUiError().message)
                            } finally {
                                linking = false
                            }
                        }
                    }
                },
                onCheckLink = {
                    if (!rechecking) {
                        rechecking = true
                        scope.launch {
                            try {
                                checkLink()
                            } finally {
                                rechecking = false
                            }
                        }
                    }
                },
                onSignOut = { scope.launch { onSignOut() } },
            )

            is SupervisedState.Linked -> when (route) {
                SupervisedRoute.Linked -> LinkedDeviceScreen(
                    deviceName = deviceInfo?.deviceName,
                    androidVersion = Build.VERSION.RELEASE,
                    tutors = deviceInfo?.tutors?.takeIf { it.isNotEmpty() } ?: listOf(current.tutorLabel),
                    lastContact = lastContact(lastHeartbeatOk, deviceInfo?.lastSeenAt),
                    reachable = !heartbeatFailing,
                    now = now,
                    pendingPermissions = permissions.pending,
                    onOpenPermissions = { route = SupervisedRoute.Permissions },
                    onOpenServices = { route = SupervisedRoute.Services },
                    onSwitchMode = onSwitchMode,
                    onSignOut = { scope.launch { onSignOut() } },
                )

                SupervisedRoute.Permissions -> PermissionsScreen(
                    permissions = permissions,
                    onOpenUsageAccessSettings = { UsageAccessPermission.openSettings(context) },
                    onRecheckUsageAccess = { hasUsageAccess = UsageAccessPermission.isGranted(context) },
                    onRequestLocation = {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    },
                    onRecheckLocation = { hasLocationPermission = LocationPermission.isGranted(context) },
                    onRequestOverlay = { overlayPermissionLauncher.launch(OverlayPermission.requestIntent(context)) },
                    onRecheckOverlay = { hasOverlayPermission = OverlayPermission.isGranted(context) },
                    onRequestDeviceAdmin = { deviceAdminLauncher.launch(DeviceAdminPermission.requestIntent(context)) },
                    onBack = { route = SupervisedRoute.Linked },
                )

                SupervisedRoute.Consent -> ScreenShareConsentScreen(
                    // Exactly what the Sprint 23 card sent, then Android's own capture dialog.
                    onContinue = {
                        screenShareSignaling?.send(
                            JSONObject().put("type", "screen_share_consent").put("granted", true)
                        )
                        screenCaptureLauncher.launch(ScreenCapture.consentIntent(context))
                    },
                    onDecline = {
                        screenShareRequested = false
                        screenShareSignaling?.send(
                            JSONObject().put("type", "screen_share_consent").put("granted", false)
                        )
                    },
                )

                SupervisedRoute.Services -> ServicesStatusScreen(
                    view = servicesView(serviceStatus, EnforcementLiveness.isRecentlyActive(context), now),
                    now = now,
                    onStopScreenShare = { ScreenShareService.requestStop(context) },
                    onBack = { route = SupervisedRoute.Linked },
                )
            }
        }
    }
    }
}
