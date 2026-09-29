package com.netprotect.app.feature.tutor

import android.os.SystemClock
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.GeofenceClient
import com.netprotect.app.core.network.LocationClient
import com.netprotect.app.feature.tutor.apps.AppsScreen
import com.netprotect.app.feature.tutor.geofences.GeofencesScreen
import com.netprotect.app.feature.tutor.location.LocationScreen
import com.netprotect.app.feature.tutor.sections.geofencesView
import com.netprotect.app.core.network.AlertsClient
import com.netprotect.app.core.network.HistoryClient
import com.netprotect.app.core.network.StatisticsClient
import com.netprotect.app.feature.tutor.alerts.AlertsScreen
import com.netprotect.app.feature.tutor.history.HistoryScreen
import com.netprotect.app.feature.tutor.sections.AlertsController
import com.netprotect.app.feature.tutor.sections.AlertsData
import com.netprotect.app.feature.tutor.sections.StatisticsController
import com.netprotect.app.feature.tutor.sections.alertItems
import com.netprotect.app.feature.tutor.sections.appLabels
import com.netprotect.app.feature.tutor.sections.historyDays
import com.netprotect.app.feature.tutor.sections.statisticsView
import com.netprotect.app.feature.tutor.statistics.StatisticsScreen
import com.netprotect.app.feature.tutor.sections.locationView
import com.netprotect.app.ui.state.Loader
import java.time.ZoneId
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.TokenSession
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.network.CurrentUser
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.PairingClient
import com.netprotect.app.feature.tutor.device.DeviceDetailController
import com.netprotect.app.feature.tutor.device.DeviceDetailScreen
import com.netprotect.app.feature.tutor.devices.DevicesScreen
import com.netprotect.app.feature.tutor.home.PairingUi
import com.netprotect.app.feature.tutor.home.TutorHomeController
import com.netprotect.app.feature.tutor.home.TutorHomeScreen
import com.netprotect.app.feature.tutor.legacy.LegacyActivityScreen
import com.netprotect.app.feature.tutor.legacy.LegacyDeviceSectionScreen
import com.netprotect.app.feature.tutor.more.MoreScreen
import com.netprotect.app.ui.components.NpBottomBar
import com.netprotect.app.ui.components.NpBottomItem
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.navigation.rememberNavStack
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TABS = listOf(
    TutorTab.Home to NpBottomItem("Inicio", NpIcons.Home),
    TutorTab.Devices to NpBottomItem("Dispositivos", NpIcons.Smartphone),
    TutorTab.Activity to NpBottomItem("Actividad", NpIcons.Clock),
    TutorTab.More to NpBottomItem("Más", NpIcons.MoreHorizontal),
)

/** Sprint 44: the tutor mode. Replaces the 963-line single-column TutorScreen with a stack of
 * routes (D-01) under a four-tab bottom bar (D-08). It owns the network clients and the state
 * holders (D-02: plain classes remembered here, no ViewModel); the screens only draw what they
 * are given. The six device sections and the audit log still use their pre-redesign look
 * (feature/tutor/legacy) until Sprints 45–47.
 *
 * What survives what: the route stack survives rotation (saved as strings); the loaded data and an
 * active pairing code do not — they are reloaded, which D-02 accepts. A code that disappears on
 * rotation is still valid on the backend until it expires or a new one replaces it.
 */
@Composable
fun TutorShell(
    baseUrl: String,
    session: TokenSession,
    user: CurrentUser,
    onSignOut: suspend () -> Unit,
    onSwitchMode: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val deviceClient = remember { DeviceClient(baseUrl) }
    val pairingClient = remember { PairingClient(baseUrl) }
    val home = remember {
        TutorHomeController(
            loadDevices = { session.authorized { token -> deviceClient.listDevices(token) } },
            createCode = { session.authorized { token -> pairingClient.generateCode(token) } },
            revokeCode = { session.authorized { token -> pairingClient.revokeCurrentCode(token) } },
            clock = SystemClock::elapsedRealtime,
        )
    }
    val stack = rememberNavStack(TutorRoute.Home, ::encodeTutorRoute, ::decodeTutorRoute)

    // "hace N min" needs a clock; a coarse one is enough and keeps recompositions rare.
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Instant.now()
        }
    }
    LaunchedEffect(Unit) { home.refreshDevices() }
    val codeShowing = home.pairing is PairingUi.Active
    LaunchedEffect(codeShowing) {
        while (codeShowing) {
            home.tick()
            delay(1_000)
        }
    }

    BackHandler(enabled = stack.value.back() != null) {
        stack.value.back()?.let { stack.value = it }
    }

    fun open(route: TutorRoute) {
        stack.value = stack.value.push(route)
    }

    Column(modifier = Modifier.fillMaxSize().background(NpColors.SkyGround)) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (val route = stack.value.current) {
                TutorRoute.Home -> TutorHomeScreen(
                    userName = user.displayName,
                    userEmail = user.email,
                    pairing = home.pairing,
                    devices = home.devices,
                    now = now,
                    onGenerateCode = { scope.launch { home.generateCode() } },
                    onRevokeCode = { scope.launch { home.revoke() } },
                    onDismissPairing = home::dismissPairing,
                    onRefreshDevices = { scope.launch { home.refreshDevices() } },
                    onOpenDevice = { open(TutorRoute.Detail(it)) },
                    onOpenActivity = { stack.value = stack.value.selectTab(TutorTab.Activity) },
                    onSwitchMode = onSwitchMode,
                    onSignOut = { scope.launch { onSignOut() } },
                )
                TutorRoute.Devices -> DevicesScreen(
                    devices = home.devices,
                    now = now,
                    onRefresh = { scope.launch { home.refreshDevices() } },
                    onOpenDevice = { open(TutorRoute.Detail(it)) },
                )
                TutorRoute.Activity -> LegacyActivityScreen(baseUrl = baseUrl, session = session)
                TutorRoute.More -> MoreScreen(
                    appVersion = BuildConfig.VERSION_NAME,
                    onSwitchMode = onSwitchMode,
                    onSignOut = { scope.launch { onSignOut() } },
                )
                is TutorRoute.Detail -> {
                    val deviceId = route.deviceId
                    val detail = remember(deviceId) {
                        DeviceDetailController(
                            loadDevice = { session.authorized { token -> deviceClient.getDevice(token, deviceId) } },
                            renameDevice = { name ->
                                session.authorized { token -> deviceClient.renameDevice(token, deviceId, name) }
                            },
                            unlinkDevice = {
                                session.authorized { token -> pairingClient.unlinkDevice(token, deviceId) }
                            },
                        )
                    }
                    LaunchedEffect(deviceId) { detail.refresh() }
                    // A device that turned out to be gone (404) must also drop off the lists.
                    val gone = (detail.device as? LoadState.Failed)?.notFound == true
                    LaunchedEffect(gone) { if (gone) home.refreshDevices() }
                    DeviceDetailScreen(
                        device = detail.device,
                        now = now,
                        rename = detail.rename,
                        unlinking = detail.unlinking,
                        unlinkError = detail.unlinkError,
                        onBack = { stack.value = stack.value.pop() },
                        onRefresh = { scope.launch { detail.refresh() } },
                        onStartRename = detail::startRename,
                        onEditRename = detail::editRename,
                        onSaveRename = {
                            scope.launch {
                                detail.saveRename()
                                home.refreshDevices()
                            }
                        },
                        onCancelRename = detail::cancelRename,
                        onConfirmUnlink = {
                            scope.launch {
                                if (detail.unlink()) {
                                    // Leave the detail of *this* device, even if the tutor already
                                    // navigated elsewhere while the request was in flight.
                                    stack.value = stack.value.leave(TutorRoute.Detail(deviceId))
                                    home.refreshDevices()
                                }
                            }
                        },
                        onOpenSection = { open(TutorRoute.Section(deviceId, it)) },
                    )
                }
                is TutorRoute.Section -> DeviceSectionRoute(
                    route = route,
                    device = (home.devices as? LoadState.Loaded)?.value?.firstOrNull { it.id == route.deviceId },
                    now = now,
                    baseUrl = baseUrl,
                    session = session,
                    onBack = { stack.value = stack.value.pop() },
                )
            }
        }
        NpBottomBar(
            items = TABS.map { it.second },
            selectedIndex = TABS.indexOfFirst { it.first == stack.value.currentTab },
            onSelect = { index -> stack.value = stack.value.selectTab(TABS[index].first) },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

/** Sprint 45: Apps, Ubicación and Geocercas have their redesigned screens; the other three device
 * sections still use the pre-redesign ones until Sprints 46–47. */
@Composable
private fun DeviceSectionRoute(
    route: TutorRoute.Section,
    device: DeviceSummary?,
    now: Instant,
    baseUrl: String,
    session: TokenSession,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val deviceId = route.deviceId
    val today = remember(now) { now.atZone(ZoneId.systemDefault()).toLocalDate() }
    when (route.section) {
        DeviceSection.Apps -> {
            val client = remember { ApplicationsClient(baseUrl) }
            val apps = remember(deviceId) {
                Loader { session.authorized { token -> client.getApplications(token, deviceId) } }
            }
            LaunchedEffect(deviceId) { apps.refresh() }
            AppsScreen(
                device = device,
                apps = apps.state,
                today = today,
                now = now,
                onRefresh = { scope.launch { apps.refresh() } },
                onBack = onBack,
            )
        }
        DeviceSection.Location -> {
            val locationClient = remember { LocationClient(baseUrl) }
            val geofenceClient = remember { GeofenceClient(baseUrl) }
            val location = remember(deviceId) {
                Loader {
                    val report = session.authorized { token -> locationClient.getLatestLocation(token, deviceId) }
                    // Only used to say "Dentro de «zona»"; if the zones can't be loaded the location
                    // is still shown, just without that line.
                    val zones = runCatching {
                        session.authorized { token -> geofenceClient.listGeofences(token, deviceId) }
                    }.getOrDefault(emptyList())
                    locationView(report, zones)
                }
            }
            LaunchedEffect(deviceId) { location.refresh() }
            LocationScreen(
                device = device,
                location = location.state,
                now = now,
                onRefresh = { scope.launch { location.refresh() } },
                onOpenMap = {
                    // D-05: the coordinates leave the app only when the tutor taps this, and only to
                    // the map app installed on this phone (a plain geo: intent, no API key, no tiles).
                    val report = (location.state as? LoadState.Loaded)?.value?.report
                    report != null && runCatching {
                        val uri = "geo:${report.latitude},${report.longitude}?q=${report.latitude},${report.longitude}".toUri()
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }.isSuccess
                },
                onBack = onBack,
            )
        }
        DeviceSection.Geofences -> {
            val client = remember { GeofenceClient(baseUrl) }
            val geofences = remember(deviceId) {
                Loader {
                    geofencesView(
                        geofences = session.authorized { token -> client.listGeofences(token, deviceId) },
                        events = session.authorized { token -> client.listGeofenceEvents(token, deviceId) },
                    )
                }
            }
            LaunchedEffect(deviceId) { geofences.refresh() }
            GeofencesScreen(
                device = device,
                geofences = geofences.state,
                today = today,
                now = now,
                onRefresh = { scope.launch { geofences.refresh() } },
                onBack = onBack,
            )
        }
        DeviceSection.History -> {
            val client = remember { HistoryClient(baseUrl) }
            val appsClient = remember { ApplicationsClient(baseUrl) }
            val history = remember(deviceId) {
                Loader {
                    val events = session.authorized { token -> client.listHistory(token, deviceId) }
                    historyDays(events, loadAppLabels(appsClient, session, deviceId), today, ZoneId.systemDefault())
                }
            }
            LaunchedEffect(deviceId) { history.refresh() }
            HistoryScreen(
                device = device,
                history = history.state,
                now = now,
                onRefresh = { scope.launch { history.refresh() } },
                onBack = onBack,
            )
        }
        DeviceSection.Statistics -> {
            val client = remember { StatisticsClient(baseUrl) }
            val appsClient = remember { ApplicationsClient(baseUrl) }
            val statistics = remember(deviceId) {
                StatisticsController { period ->
                    val stats = session.authorized { token -> client.getStatistics(token, deviceId, period.apiValue) }
                    statisticsView(stats, loadAppLabels(appsClient, session, deviceId))
                }
            }
            LaunchedEffect(deviceId) { statistics.refresh() }
            StatisticsScreen(
                device = device,
                period = statistics.period,
                statistics = statistics.state,
                now = now,
                onSelectPeriod = { scope.launch { statistics.select(it) } },
                onRefresh = { scope.launch { statistics.refresh() } },
                onBack = onBack,
            )
        }
        DeviceSection.Alerts -> {
            val client = remember { AlertsClient(baseUrl) }
            val appsClient = remember { ApplicationsClient(baseUrl) }
            val alerts = remember(deviceId) {
                AlertsController(
                    load = {
                        AlertsData(
                            alerts = session.authorized { token -> client.listAlerts(token, deviceId) },
                            // Only used to replace "Silenciar" with "Silenciada"; without it the
                            // button stays and silencing again is harmless (the backend upserts).
                            silences = runCatching {
                                session.authorized { token -> client.listSilences(token, deviceId) }
                            }.getOrDefault(emptyList()),
                            labels = loadAppLabels(appsClient, session, deviceId),
                        )
                    },
                    markReadRequest = { alertId -> session.authorized { token -> client.markRead(token, deviceId, alertId) } },
                    silenceRequest = { alertId -> session.authorized { token -> client.silence(token, deviceId, alertId, days = null) } },
                )
            }
            LaunchedEffect(deviceId) { alerts.refresh() }
            val items = when (val state = alerts.state) {
                is LoadState.Loaded -> LoadState.Loaded(
                    alertItems(state.value.alerts, state.value.silences, state.value.labels, now, ZoneId.systemDefault()),
                )
                is LoadState.Failed -> state
                LoadState.Loading -> LoadState.Loading
            }
            AlertsScreen(
                device = device,
                alerts = items,
                filter = alerts.filter,
                busyAlertId = alerts.busyId,
                actionError = alerts.actionError,
                silenceTarget = (items as? LoadState.Loaded)?.value?.firstOrNull { it.id == alerts.silenceTarget },
                now = now,
                onSelectFilter = { alerts.filter = it },
                onMarkRead = { alertId -> scope.launch { alerts.markRead(alertId) } },
                onAskSilence = { alertId -> alerts.askSilence(alertId) },
                onConfirmSilence = { scope.launch { alerts.confirmSilence() } },
                onDismissSilence = { alerts.dismissSilence() },
                onRefresh = { scope.launch { alerts.clearActionError(); alerts.refresh() } },
                onBack = onBack,
            )
        }
        else -> LegacyDeviceSectionScreen(
            section = route.section,
            deviceId = deviceId,
            baseUrl = baseUrl,
            session = session,
        )
    }
}

/** App names for history, statistics and alerts. Optional: if the list can't be loaded the
 * screens show package names instead of failing. */
private suspend fun loadAppLabels(client: ApplicationsClient, session: TokenSession, deviceId: String): Map<String, String> =
    runCatching { session.authorized { token -> client.getApplications(token, deviceId) } }
        .map(::appLabels)
        .getOrDefault(emptyMap())
