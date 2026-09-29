package com.netprotect.app.feature.tutor

import android.os.SystemClock
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
                                    stack.value = stack.value.pop()
                                    home.refreshDevices()
                                }
                            }
                        },
                        onOpenSection = { open(TutorRoute.Section(deviceId, it)) },
                    )
                }
                is TutorRoute.Section -> LegacyDeviceSectionScreen(
                    section = route.section,
                    deviceId = route.deviceId,
                    baseUrl = baseUrl,
                    session = session,
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
