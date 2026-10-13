package com.netprotect.app.feature.supervised

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.runtime.saveable.Saver
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
import com.netprotect.app.core.auth.WelcomeStore
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
import com.netprotect.app.feature.supervised.consent.ConsentEvent
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentScreen
import com.netprotect.app.feature.supervised.consent.consentRequestedAtFromMillis
import com.netprotect.app.feature.supervised.consent.consentRequestedAtToMillis
import com.netprotect.app.feature.supervised.consent.consentStep
import com.netprotect.app.feature.supervised.consent.shouldSendConsentStop
import com.netprotect.app.feature.supervised.services.ServicesStatusScreen
import com.netprotect.app.feature.supervised.settings.MoreSettingsScreen
import com.netprotect.app.feature.supervised.unlinked.UnlinkedScreen
import com.netprotect.app.ui.components.ActiveShareBanner
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.feature.supervised.permissions.PermissionDetailScreen
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission
import com.netprotect.app.feature.supervised.privacy.PrivacyScreen
import com.netprotect.app.feature.supervised.welcome.WelcomeScreen
import com.netprotect.app.feature.supervised.welcome.WelcomeStep
import com.netprotect.app.feature.supervised.welcome.shouldShowWelcome
import com.netprotect.app.feature.supervised.welcome.welcomeBack
import com.netprotect.app.feature.supervised.welcome.welcomeNext
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.theme.NpColors
import java.time.Instant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

private const val HEARTBEAT_INTERVAL_MS = 60_000L
private const val APP_SYNC_INTERVAL_MS = 5 * 60_000L

/** Sprint 64 (B1): how often `/devices/me` is asked while linked, besides every ON_RESUME. */
private const val LINK_CHECK_INTERVAL_MS = 5 * 60_000L

/** Sprint 65: saves the pending request's arrival time across activity recreation (see consentRequestedAt). */
private val ConsentRequestedAtSaver = Saver<Instant?, Long>(
    save = { consentRequestedAtToMillis(it) },
    restore = { consentRequestedAtFromMillis(it) },
)

private sealed interface SupervisedState {
    data object CheckingLink : SupervisedState
    data class EnteringCode(val error: String? = null) : SupervisedState
    /** Sprint 64 (G-19): only the device's identity, which every service is started with. The tutors
     * and everything else a refresh can change live in separate presentation state. */
    data class Linked(val deviceId: String) : SupervisedState
    /** Sprint 64 (B1): the server said this device is no longer linked. Not [EnteringCode]: that one
     * means it was never linked (or the person chose to link again). */
    data object Unlinked : SupervisedState
}

/**
 * Sprint 48: the supervised mode's container. Everything that keeps the device reporting and
 * enforcing lives here, moved **unchanged** from the old SupervisedScreen (Sprints 7–41): the
 * heartbeat loop, the app sync loop, RuleEnforcementService, LocationReportingService, the
 * realtime socket for screen-share requests, SyncWorker and the permission launchers.
 *
 * The one rule that keeps them alive: every effect is keyed on the linked device's identity (the
 * `deviceId` inside `SupervisedState.Linked`) and the permission flags, and **nothing else**.
 * Navigation ([SupervisedRoute]) and what the screens display ([MyDeviceInfo], the tutors, last
 * heartbeat) are separate state that no effect reads as a key, and
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
    var screenShareSignaling by remember { mutableStateOf<ReconnectingRealtimeChannel?>(null) }

    // Sprint 48 — display-only state. Never used as an effect key (see the class comment).
    var route by rememberSaveable { mutableStateOf(SupervisedRoute.Linked) }
    // Sprint 66: which permission PermissionDetail shows. Presentation only, never an effect key (G-19).
    var detailPermission by rememberSaveable { mutableStateOf(SupervisedPermission.UsageAccess) }
    // Sprint 67: the welcome after a new link and its step. Presentation only, never an effect key (G-19):
    // the services start as soon as the state is Linked, whether or not the welcome is on screen.
    var showWelcome by rememberSaveable { mutableStateOf(false) }
    var welcomeStep by rememberSaveable { mutableStateOf(WelcomeStep.Linked) }
    var deviceInfo by remember { mutableStateOf<MyDeviceInfo?>(null) }
    var lastHeartbeatOk by remember { mutableStateOf<Instant?>(null) }
    var heartbeatFailing by remember { mutableStateOf(false) }
    var linking by remember { mutableStateOf(false) }
    var rechecking by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(Instant.now()) }
    // Sprint 64 (G-19): the tutors' names, refreshed by B1 — presentation only, never an effect key.
    var tutorLabel by remember { mutableStateOf<String?>(null) }
    // Sprint 65: when the pending screen-share request reached the phone; null when there is none.
    // Presentation only (the consent countdown and its expiry), never a service effect's key (G-19).
    // Saved like `route`: a recreated activity must reopen the consent screen with its countdown and expiry.
    var consentRequestedAt by rememberSaveable(stateSaver = ConsentRequestedAtSaver) { mutableStateOf<Instant?>(null) }

    // The only key the service effects below use: null in every state but Linked, and stable while
    // linked — a B1 refresh that changes the tutors leaves it untouched.
    val linkedDeviceId = (state as? SupervisedState.Linked)?.deviceId

    fun leaveConsentRoute() {
        if (route == SupervisedRoute.Consent) route = SupervisedRoute.Linked
    }

    // Sprint 65: every change to the pending request goes through consentStep; this only applies it.
    // The stop is best-effort and never queued or retried (ReconnectingRealtimeChannel.send): with
    // the socket down the tutor isn't told, and the request still closes here.
    fun onConsentEvent(event: ConsentEvent) {
        // A request hopping to the main thread after the link ended has no one to answer it.
        if (event is ConsentEvent.Requested && state !is SupervisedState.Linked) return
        val step = consentStep(consentRequestedAt, event)
        consentRequestedAt = step.requestedAt
        // During a running capture a stop would end that live session for the tutor: only clear locally.
        val reason = step.sendStopReason
        if (reason != null && shouldSendConsentStop(reason, ServiceStatusRegistry.status.value.screenShareActive)) {
            screenShareSignaling?.send(JSONObject().put("type", "screen_share_stop").put("reason", reason))
        }
        when {
            step.requestedAt != null -> if (event is ConsentEvent.Requested) route = SupervisedRoute.Consent
            // «Continuar» stays on the consent screen behind Android's dialog; its result brings them back.
            event == ConsentEvent.Answered -> Unit
            else -> leaveConsentRoute()
        }
    }

    // Android's own screen-capture consent dialog. Deliberately launched only from the card
    // below, never automatically: the tutor's request is what makes the card appear, and the
    // supervised person is the one who decides. Each session needs this dialog again — the
    // resulting permission is good for exactly one capture (verified in
    // docs/android/capability-matrix.md, Sprint 23), so there is nothing here to reuse silently.
    val screenCaptureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // The answer was already given («Continuar»); a request that arrived meanwhile stays on screen.
        if (consentRequestedAt == null) leaveConsentRoute()
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
    LaunchedEffect(linkedDeviceId != null) {
        if (linkedDeviceId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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

    // Any failure (offline, timeout, 5xx, unreadable body) becomes a failed Result, which
    // classifyLinkCheck never reads as an unlink. Cancellation is not a failure: it propagates.
    suspend fun fetchMyDevice(): Result<MyDeviceInfo?> = try {
        // Sprint 48 (B-01): say which install this is — one account can own several rows.
        Result.success(session.authorized { token -> deviceClient.getMyDevice(token, deviceInstanceId) })
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        Result.failure(exception)
    }

    // Sprint 64 (B1): the only way out of Linked besides signing out. Clearing the cache and leaving
    // Linked lets the effects' onDispose stop the services, the socket, the heartbeat and SyncWorker.
    fun enterUnlinked() {
        LinkedDeviceStore.clear(context)
        // A new link shows the welcome again.
        WelcomeStore.clear(context)
        showWelcome = false
        deviceInfo = null
        tutorLabel = null
        onConsentEvent(ConsentEvent.Stopped)
        route = SupervisedRoute.Linked
        // Linking again starts from "no report yet", not from the old link's last heartbeat.
        lastHeartbeatOk = null
        heartbeatFailing = false
        // A share must not outlive the link it was authorised under.
        if (ServiceStatusRegistry.status.value.screenShareActive) ScreenShareService.requestStop(context)
        state = SupervisedState.Unlinked
    }

    // The local cache is only a UI shortcut. On every start we ask the server who this
    // account's device actually is: a different Google account signing into the same phone,
    // or a tutor unlinking it remotely, would otherwise leave a stale cache pointing at a
    // device_id that no longer belongs to this session.
    suspend fun checkLink() {
        val cached = LinkedDeviceStore.read(context)
        when (val check = classifyLinkCheck(fetchMyDevice())) {
            is LinkCheck.Linked -> {
                deviceInfo = check.info
                tutorLabel = check.tutorLabel
                LinkedDeviceStore.write(context, check.deviceId, check.tutorLabel)
                state = SupervisedState.Linked(check.deviceId)
            }
            // The device row can outlive every tutor link: /devices/me then answers 200 with an empty
            // tutor list, which is not "linked" either. With a cache this phone was linked, so the
            // tutor ended it (B1); without one it simply never was.
            LinkCheck.Unlinked -> if (cached != null) {
                enterUnlinked()
            } else {
                deviceInfo = null
                state = SupervisedState.EnteringCode()
            }
            // Couldn't reach the server (offline, timeout): trust the cache rather than
            // forcing this device to re-pair just because of a transient network hiccup.
            LinkCheck.Inconclusive -> {
                tutorLabel = cached?.tutorLabel
                state = cached?.let { SupervisedState.Linked(it.deviceId) } ?: SupervisedState.EnteringCode()
            }
        }
    }

    // Sprint 64 (B1): the periodic and ON_RESUME re-check while linked. A refresh with tutors only
    // updates presentation state; linkedDeviceId changes only if the identity really changed
    // (changesLinkIdentity), so it never restarts a service (G-19). A failure changes nothing.
    suspend fun refreshLink() {
        val linked = state as? SupervisedState.Linked ?: return
        val check = classifyLinkCheck(fetchMyDevice())
        // The state moved while the request was in flight: this answer no longer applies. By identity, not
        // equality: after Unlinked and a new pairing the state equals the old Linked but is a different link.
        if (state !== linked) return
        when (check) {
            is LinkCheck.Linked -> {
                deviceInfo = check.info
                tutorLabel = check.tutorLabel
                LinkedDeviceStore.write(context, check.deviceId, check.tutorLabel)
                if (changesLinkIdentity(linked.deviceId, check)) state = SupervisedState.Linked(check.deviceId)
            }
            LinkCheck.Unlinked -> enterUnlinked()
            LinkCheck.Inconclusive -> Unit
        }
    }
    LaunchedEffect(Unit) { checkLink() }

    // Sprint 64 (B1): coming back to the app re-asks the server whether the link still exists. Keyed
    // on the lifecycle owner only, like the permission observer above.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && state is SupervisedState.Linked) {
                scope.launch { refreshLink() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // The first re-check waits a full interval: checkLink or the pairing has just asked.
    LaunchedEffect(linkedDeviceId) {
        if (linkedDeviceId == null) return@LaunchedEffect
        while (true) {
            delay(LINK_CHECK_INTERVAL_MS)
            refreshLink()
        }
    }

    // Sends a heartbeat immediately once linked, then every HEARTBEAT_INTERVAL_MS while this
    // shell stays composed — a tighter cadence than SyncWorker's 15-minute background floor
    // (below) can offer, so this keeps running whenever the app happens to be in the foreground
    // instead of stepping aside for it.
    LaunchedEffect(linkedDeviceId) {
        val deviceId = linkedDeviceId ?: return@LaunchedEffect
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
    LaunchedEffect(linkedDeviceId, hasUsageAccess) {
        val deviceId = linkedDeviceId ?: return@LaunchedEffect
        if (!hasUsageAccess) return@LaunchedEffect
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
    DisposableEffect(linkedDeviceId, hasUsageAccess) {
        val deviceId = linkedDeviceId
        if (hasUsageAccess && deviceId != null) {
            RuleEnforcementService.start(context, BuildConfig.API_BASE_URL, session.currentAccessToken().orEmpty(), deviceId)
        }
        onDispose { RuleEnforcementService.stop(context) }
    }

    // Independent gate from the rule-enforcement service above: reporting location doesn't need
    // usage access, and enforcing rules doesn't need location. Same foreground-start requirement
    // (see LocationReportingService): this effect only fires while the shell is composed.
    DisposableEffect(linkedDeviceId, hasLocationPermission) {
        val deviceId = linkedDeviceId
        if (hasLocationPermission && deviceId != null) {
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
    DisposableEffect(linkedDeviceId) {
        val deviceId = linkedDeviceId ?: return@DisposableEffect onDispose { }

        val channel = ReconnectingRealtimeChannel(
            baseUrl = BuildConfig.API_BASE_URL,
            deviceId = deviceId,
            tokenProvider = { session.validAccessToken() },
            onConnectedChange = ServiceStatusRegistry::realtimeConnected,
        ) { event, _ ->
            // OkHttp's thread: the arrival time is taken here, the state changes on the main thread.
            when (event) {
                "screen_share_request" -> {
                    val at = Instant.now()
                    scope.launch { onConsentEvent(ConsentEvent.Requested(at)) }
                }
                "screen_share_stop" -> scope.launch { onConsentEvent(ConsentEvent.Stopped) }
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
    DisposableEffect(linkedDeviceId) {
        val deviceId = linkedDeviceId
        if (deviceId != null) {
            SyncWorker.schedule(context, BuildConfig.API_BASE_URL, deviceId)
        }
        onDispose { SyncWorker.cancel(context) }
    }

    // Sprint 50/65: a pending request opens the consent screen, wherever the person is (onConsentEvent).
    // This presentation clock runs only while a request is pending, keyed on its arrival time and on
    // nothing a service uses: it refreshes `now` for the countdown and closes the request once
    // CONSENT_WINDOW_SECONDS have passed, telling the tutor it expired.
    LaunchedEffect(consentRequestedAt) {
        val requestedAt = consentRequestedAt ?: return@LaunchedEffect
        while (consentRequestedAt == requestedAt) {
            now = Instant.now()
            onConsentEvent(ConsentEvent.Tick(now))
            delay(1_000L)
        }
    }
    BackHandler(enabled = state is SupervisedState.Linked && route != SupervisedRoute.Linked) {
        when (route) {
            // Back is not an answer («Ahora no» is): it closes the request and tells the tutor it was
            // dismissed, so their page stops waiting (B5).
            SupervisedRoute.Consent -> onConsentEvent(ConsentEvent.Back)
            // The detail is opened from Permisos, so Back returns there, not to Inicio.
            SupervisedRoute.PermissionDetail -> route = SupervisedRoute.Permissions
            else -> route = SupervisedRoute.Linked
        }
    }
    // Sprint 67: the welcome is drawn on the Linked route, so the handler above is off. Back goes from step 2
    // to step 1 and does nothing on step 1: the welcome is only left with its own buttons.
    BackHandler(enabled = state is SupervisedState.Linked && route == SupervisedRoute.Linked && showWelcome) {
        welcomeStep = welcomeBack(welcomeStep)
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
    // The permission actions, shared by Permisos and its detail so both fire exactly the same launchers.
    val openUsageAccessSettings = { UsageAccessPermission.openSettings(context) }
    val requestLocation = { locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
    val requestOverlay = { overlayPermissionLauncher.launch(OverlayPermission.requestIntent(context)) }
    val requestDeviceAdmin = { deviceAdminLauncher.launch(DeviceAdminPermission.requestIntent(context)) }

    Column(modifier = Modifier.fillMaxSize().background(NpColors.Cream)) {
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
                                tutorLabel = label
                                welcomeStep = WelcomeStep.Linked
                                showWelcome = shouldShowWelcome(
                                    justLinked = true,
                                    seenForDeviceId = WelcomeStore.seenDeviceId(context),
                                    deviceId = result.deviceId,
                                )
                                state = SupervisedState.Linked(result.deviceId)
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

            SupervisedState.Unlinked -> UnlinkedScreen(
                onRelink = {
                    codeInput = ""
                    state = SupervisedState.EnteringCode()
                },
                // No confirmation: nothing is protecting this phone any more.
                onSignOut = { scope.launch { onSignOut() } },
            )

            is SupervisedState.Linked -> when (route) {
                // Sprint 67: a screen-share request still takes the route (Consent) and the welcome comes back after it.
                SupervisedRoute.Linked -> if (showWelcome) {
                    WelcomeScreen(
                        step = welcomeStep,
                        pendingPermissions = permissions.pending,
                        onNext = { welcomeNext(welcomeStep)?.let { welcomeStep = it } },
                        onBack = { welcomeStep = welcomeBack(welcomeStep) },
                        onFinish = {
                            WelcomeStore.markSeen(context, current.deviceId)
                            showWelcome = false
                            // «Entendido, configurar permisos» opens Permisos; plain «Entendido» stays on Inicio.
                            if (permissions.pending > 0) route = SupervisedRoute.Permissions
                        },
                    )
                } else LinkedDeviceScreen(
                    deviceName = deviceInfo?.deviceName,
                    androidVersion = Build.VERSION.RELEASE,
                    tutors = deviceInfo?.tutors?.takeIf { it.isNotEmpty() } ?: listOfNotNull(tutorLabel),
                    lastContact = lastContact(lastHeartbeatOk, deviceInfo?.lastSeenAt),
                    now = now,
                    pendingPermissions = permissions.pending,
                    onOpenPermissions = { route = SupervisedRoute.Permissions },
                    onOpenServices = { route = SupervisedRoute.Services },
                    onOpenMoreSettings = { route = SupervisedRoute.MoreSettings },
                    onOpenPrivacy = { route = SupervisedRoute.Privacy },
                )

                SupervisedRoute.Permissions -> PermissionsScreen(
                    permissions = permissions,
                    onOpenUsageAccessSettings = openUsageAccessSettings,
                    onRecheckUsageAccess = { hasUsageAccess = UsageAccessPermission.isGranted(context) },
                    onRequestLocation = requestLocation,
                    onRecheckLocation = { hasLocationPermission = LocationPermission.isGranted(context) },
                    onRequestOverlay = requestOverlay,
                    onRecheckOverlay = { hasOverlayPermission = OverlayPermission.isGranted(context) },
                    onRequestDeviceAdmin = requestDeviceAdmin,
                    onOpenDetail = { permission ->
                        detailPermission = permission
                        route = SupervisedRoute.PermissionDetail
                    },
                    onBack = { route = SupervisedRoute.Linked },
                )

                // Live flags: the ON_RESUME observer above re-reads them when the person comes back.
                SupervisedRoute.PermissionDetail -> PermissionDetailScreen(
                    permission = detailPermission,
                    permissions = permissions,
                    onOpenUsageAccessSettings = openUsageAccessSettings,
                    onRequestLocation = requestLocation,
                    onRequestOverlay = requestOverlay,
                    onRequestDeviceAdmin = requestDeviceAdmin,
                    // Location denied for good no longer shows Android's dialog: this app's page in Settings.
                    onOpenAppSettings = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onBack = { route = SupervisedRoute.Permissions },
                )

                SupervisedRoute.Privacy -> PrivacyScreen(onBack = { route = SupervisedRoute.Linked })

                SupervisedRoute.Consent -> ScreenShareConsentScreen(
                    // Exactly what the Sprint 23 card sent, then Android's own capture dialog.
                    // A tap in the same frame the request expired or was dismissed answers nothing.
                    onContinue = {
                        if (consentRequestedAt == null) {
                            leaveConsentRoute()
                            return@ScreenShareConsentScreen
                        }
                        screenShareSignaling?.send(
                            JSONObject().put("type", "screen_share_consent").put("granted", true)
                        )
                        onConsentEvent(ConsentEvent.Answered)
                        screenCaptureLauncher.launch(ScreenCapture.consentIntent(context))
                    },
                    onDecline = {
                        if (consentRequestedAt == null) {
                            leaveConsentRoute()
                            return@ScreenShareConsentScreen
                        }
                        onConsentEvent(ConsentEvent.Answered)
                        leaveConsentRoute()
                        screenShareSignaling?.send(
                            JSONObject().put("type", "screen_share_consent").put("granted", false)
                        )
                    },
                    requestedAt = consentRequestedAt,
                    now = now,
                    // The arrow means the same as the system Back (BackHandler above): not an answer.
                    onBack = { onConsentEvent(ConsentEvent.Back) },
                )

                SupervisedRoute.Services -> ServicesStatusScreen(
                    view = servicesView(serviceStatus, EnforcementLiveness.isRecentlyActive(context), now),
                    now = now,
                    onStopScreenShare = { ScreenShareService.requestStop(context) },
                    onBack = { route = SupervisedRoute.Linked },
                )

                SupervisedRoute.MoreSettings -> MoreSettingsScreen(
                    onSignOut = { scope.launch { onSignOut() } },
                    onBack = { route = SupervisedRoute.Linked },
                )
            }
        }
    }
    }
}
