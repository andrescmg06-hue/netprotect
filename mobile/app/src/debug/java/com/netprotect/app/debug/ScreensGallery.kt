package com.netprotect.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.Geofence
import com.netprotect.app.core.network.GeofenceEvent
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.feature.home.LoadingScreen
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.feature.home.RoleSelectionScreen
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.feature.tutor.apps.AppsScreen
import com.netprotect.app.feature.tutor.device.DeviceDetailScreen
import com.netprotect.app.feature.tutor.device.RenameUi
import com.netprotect.app.feature.tutor.devices.DevicesScreen
import com.netprotect.app.feature.tutor.geofences.GeofencesScreen
import com.netprotect.app.feature.tutor.home.PairingUi
import com.netprotect.app.feature.tutor.home.TutorHomeScreen
import com.netprotect.app.feature.tutor.location.LocationScreen
import com.netprotect.app.feature.tutor.more.MoreScreen
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.block.BlockScreenContent
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.feature.tutor.activity.ActivityDay
import com.netprotect.app.feature.tutor.activity.ActivityRow
import com.netprotect.app.feature.tutor.activity.MyActivityScreen
import com.netprotect.app.feature.tutor.sections.LocationView
import com.netprotect.app.feature.tutor.sections.geofencesView
import com.netprotect.app.feature.tutor.alerts.AlertsScreen
import com.netprotect.app.feature.tutor.history.HistoryScreen
import com.netprotect.app.feature.tutor.sections.AlertFilter
import com.netprotect.app.feature.tutor.sections.AlertItem
import com.netprotect.app.feature.tutor.sections.BlockReasonItem
import com.netprotect.app.feature.tutor.sections.ComplianceRow
import com.netprotect.app.feature.tutor.sections.HistoryDay
import com.netprotect.app.feature.tutor.sections.HistoryKind
import com.netprotect.app.feature.tutor.sections.HistoryRow
import com.netprotect.app.feature.tutor.sections.NO_USAGE_IN_PERIOD
import com.netprotect.app.feature.tutor.sections.StatsPeriod
import com.netprotect.app.feature.tutor.sections.StatisticsView
import com.netprotect.app.feature.tutor.sections.TopAppRow
import com.netprotect.app.feature.tutor.statistics.StatisticsScreen
import com.netprotect.app.ui.format.AuditLabels
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.LocalDate

/** Galería de pantallas (solo debug): cada pantalla en cada estado, dentro de un marco de 760 dp
 * de alto, con su título encima. Datos ilustrativos; nada de esto se usa fuera de `src/debug`. */
@Composable
fun ScreensGallery() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // ---- Sprint 49 (al principio para las capturas) ----
        ScreenFrame("BlockScreenContent · BLOCK") {
            BlockScreenContent(packageName = "com.instagram.android", appLabel = "Instagram", categoryLabel = "Redes sociales", reason = BlockReason.BLOCK, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · DAILY_LIMIT") {
            BlockScreenContent(packageName = "com.google.android.youtube", appLabel = "YouTube", categoryLabel = "Streaming", reason = BlockReason.DAILY_LIMIT, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · WEEKLY_LIMIT") {
            BlockScreenContent(packageName = "com.ejemplo.tiktok", appLabel = "TikTok", categoryLabel = "Redes sociales", reason = BlockReason.WEEKLY_LIMIT, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · SCHEDULE") {
            BlockScreenContent(packageName = "com.discord", appLabel = "Discord", categoryLabel = "Comunicación", reason = BlockReason.SCHEDULE, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · CATEGORY") {
            BlockScreenContent(packageName = "com.netflix.mediaclient", appLabel = "Netflix", categoryLabel = "Streaming", reason = BlockReason.CATEGORY, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · SCHOOL_MODE") {
            BlockScreenContent(packageName = "com.android.chrome", appLabel = "Chrome", categoryLabel = null, reason = BlockReason.SCHOOL_MODE, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · DEFAULT_POLICY") {
            BlockScreenContent(packageName = "com.android.chrome", appLabel = "Chrome", categoryLabel = null, reason = BlockReason.DEFAULT_POLICY, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · CATEGORY sin categoría") {
            BlockScreenContent(packageName = "com.ejemplo.tiktok", appLabel = "TikTok", categoryLabel = null, reason = BlockReason.CATEGORY, onGoHome = {})
        }
        ScreenFrame("BlockScreenContent · nombre largo") {
            BlockScreenContent(packageName = "com.ejemplo.largo", appLabel = "Una aplicación con un nombre muy largo para probar el recorte", categoryLabel = "Streaming", reason = BlockReason.DAILY_LIMIT, onGoHome = {})
        }
        // ---- Sprint 48 (al principio para las capturas) ----
        ScreenFrame("LinkDeviceScreen · vacío") {
            LinkDeviceScreen(code = "", onCodeChange = {}, linking = false, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · 123") {
            LinkDeviceScreen(code = "123", onCodeChange = {}, linking = false, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · 6 dígitos") {
            LinkDeviceScreen(code = "123456", onCodeChange = {}, linking = false, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · vinculando") {
            LinkDeviceScreen(code = "123456", onCodeChange = {}, linking = true, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · error") {
            LinkDeviceScreen(code = "123456", onCodeChange = {}, linking = false, error = "El código no es válido o ya venció.", rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · comprobando") {
            LinkDeviceScreen(code = "", onCodeChange = {}, linking = false, error = null, rechecking = true, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkedDeviceScreen · 2 pendientes") {
            LinkedDeviceScreen(
                deviceName = "Tablet de Sofía",
                androidVersion = "13",
                tutors = listOf("Andrés Mosquera", "María Pérez"),
                lastContact = galleryNowS48.minusSeconds(120),
                reachable = true,
                now = galleryNowS48,
                pendingPermissions = 2,
                onOpenPermissions = {}, onOpenServices = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("LinkedDeviceScreen · 0 pendientes sin conexión") {
            LinkedDeviceScreen(
                deviceName = "Tablet de Sofía",
                androidVersion = "13",
                tutors = listOf("Andrés Mosquera"),
                lastContact = galleryNowS48.minusSeconds(120),
                reachable = false,
                now = galleryNowS48,
                pendingPermissions = 0,
                onOpenPermissions = {}, onOpenServices = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("LinkedDeviceScreen · sin nombre ni contacto") {
            LinkedDeviceScreen(
                deviceName = null,
                androidVersion = "13",
                tutors = listOf("Andrés Mosquera"),
                lastContact = null,
                reachable = false,
                now = galleryNowS48,
                pendingPermissions = 2,
                onOpenPermissions = {}, onOpenServices = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("PermissionsScreen · 2 pendientes") {
            PermissionsScreen(
                permissions = PermissionsUi(usageAccess = false, location = false, overlay = true, deviceAdmin = true),
                onOpenUsageAccessSettings = {}, onRecheckUsageAccess = {}, onRequestLocation = {}, onRecheckLocation = {},
                onRequestOverlay = {}, onRecheckOverlay = {}, onRequestDeviceAdmin = {}, onBack = {},
            )
        }
        ScreenFrame("PermissionsScreen · todos configurados") {
            PermissionsScreen(
                permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true),
                onOpenUsageAccessSettings = {}, onRecheckUsageAccess = {}, onRequestLocation = {}, onRecheckLocation = {},
                onRequestOverlay = {}, onRecheckOverlay = {}, onRequestDeviceAdmin = {}, onBack = {},
            )
        }
        ScreenFrame("PermissionsScreen · todos pendientes") {
            PermissionsScreen(
                permissions = PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false),
                onOpenUsageAccessSettings = {}, onRecheckUsageAccess = {}, onRequestLocation = {}, onRecheckLocation = {},
                onRequestOverlay = {}, onRecheckOverlay = {}, onRequestDeviceAdmin = {}, onBack = {},
            )
        }
        // ---- Sprint 47 (al principio para las capturas) ----
        ScreenFrame("MyActivityScreen · dos días") {
            MyActivityScreen(
                activity = LoadState.Loaded(galleryActivityDays),
                hasMore = true,
                loadingMore = false,
                moreError = null,
                onLoadMore = {},
                onRefresh = {},
            )
        }
        ScreenFrame("MyActivityScreen · loadingMore") {
            MyActivityScreen(
                activity = LoadState.Loaded(galleryActivityDays),
                hasMore = true,
                loadingMore = true,
                moreError = null,
                onLoadMore = {},
                onRefresh = {},
            )
        }
        ScreenFrame("MyActivityScreen · moreError") {
            MyActivityScreen(
                activity = LoadState.Loaded(galleryActivityDays),
                hasMore = true,
                loadingMore = false,
                moreError = "Sin conexión con el servidor.",
                onLoadMore = {},
                onRefresh = {},
            )
        }
        ScreenFrame("MyActivityScreen · vacío") {
            MyActivityScreen(
                activity = LoadState.Loaded(emptyList()),
                hasMore = false,
                loadingMore = false,
                moreError = null,
                onLoadMore = {},
                onRefresh = {},
            )
        }
        ScreenFrame("MyActivityScreen · error") {
            MyActivityScreen(
                activity = LoadState.Failed("Sin conexión con el servidor."),
                hasMore = false,
                loadingMore = false,
                moreError = null,
                onLoadMore = {},
                onRefresh = {},
            )
        }
        // ---- Sprint 46 (al principio para las capturas) ----
        ScreenFrame("HistoryScreen · dos días") {
            HistoryScreen(device = galleryTablet, history = LoadState.Loaded(galleryHistory), now = galleryNowS46, onRefresh = {}, onBack = {})
        }
        ScreenFrame("HistoryScreen · vacío") {
            HistoryScreen(device = galleryTablet, history = LoadState.Loaded(emptyList()), now = galleryNowS46, onRefresh = {}, onBack = {})
        }
        ScreenFrame("HistoryScreen · error") {
            HistoryScreen(device = galleryTablet, history = LoadState.Failed("Sin conexión con el servidor."), now = galleryNowS46, onRefresh = {}, onBack = {})
        }
        ScreenFrame("StatisticsScreen · 7 días") {
            StatisticsScreen(
                device = galleryTablet,
                period = StatsPeriod.Week,
                statistics = LoadState.Loaded(galleryStatistics),
                now = galleryNowS46,
                onSelectPeriod = {},
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("StatisticsScreen · Hoy vacío") {
            StatisticsScreen(
                device = galleryTablet,
                period = StatsPeriod.Today,
                statistics = LoadState.Loaded(StatisticsView(emptyList(), emptyList(), emptyList())),
                now = galleryNowS46,
                onSelectPeriod = {},
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("StatisticsScreen · error") {
            StatisticsScreen(
                device = galleryTablet,
                period = StatsPeriod.Week,
                statistics = LoadState.Failed("Sin conexión con el servidor."),
                now = galleryNowS46,
                onSelectPeriod = {},
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("AlertsScreen · 5 alertas") {
            AlertsScreen(
                device = galleryTablet,
                alerts = LoadState.Loaded(galleryAlerts),
                filter = AlertFilter.All,
                busyAlertId = null,
                actionError = null,
                silenceTarget = null,
                now = galleryNowS46,
                onSelectFilter = {}, onMarkRead = {}, onAskSilence = {}, onConfirmSilence = {}, onDismissSilence = {},
                onRefresh = {}, onBack = {},
            )
        }
        ScreenFrame("AlertsScreen · busyAlertId") {
            AlertsScreen(
                device = galleryTablet,
                alerts = LoadState.Loaded(galleryAlerts),
                filter = AlertFilter.All,
                busyAlertId = galleryAlerts.first().id,
                actionError = null,
                silenceTarget = null,
                now = galleryNowS46,
                onSelectFilter = {}, onMarkRead = {}, onAskSilence = {}, onConfirmSilence = {}, onDismissSilence = {},
                onRefresh = {}, onBack = {},
            )
        }
        // No "silenceTarget" frame: ConfirmDialog is a modal window and would cover the whole
        // gallery. The dialog is covered by AlertsScreenTest.
        ScreenFrame("AlertsScreen · actionError") {
            AlertsScreen(
                device = galleryTablet,
                alerts = LoadState.Loaded(galleryAlerts),
                filter = AlertFilter.All,
                busyAlertId = null,
                actionError = "Sin conexión con el servidor.",
                silenceTarget = null,
                now = galleryNowS46,
                onSelectFilter = {}, onMarkRead = {}, onAskSilence = {}, onConfirmSilence = {}, onDismissSilence = {},
                onRefresh = {}, onBack = {},
            )
        }
        ScreenFrame("AlertsScreen · vacía") {
            AlertsScreen(
                device = galleryTablet,
                alerts = LoadState.Loaded(emptyList()),
                filter = AlertFilter.All,
                busyAlertId = null,
                actionError = null,
                silenceTarget = null,
                now = galleryNowS46,
                onSelectFilter = {}, onMarkRead = {}, onAskSilence = {}, onConfirmSilence = {}, onDismissSilence = {},
                onRefresh = {}, onBack = {},
            )
        }
        ScreenFrame("AlertsScreen · error") {
            AlertsScreen(
                device = galleryTablet,
                alerts = LoadState.Failed("Sin conexión con el servidor."),
                filter = AlertFilter.All,
                busyAlertId = null,
                actionError = null,
                silenceTarget = null,
                now = galleryNowS46,
                onSelectFilter = {}, onMarkRead = {}, onAskSilence = {}, onConfirmSilence = {}, onDismissSilence = {},
                onRefresh = {}, onBack = {},
            )
        }
        ScreenFrame("LoadingScreen") {
            LoadingScreen()
        }
        ScreenFrame("LoginScreen · Checking") {
            LoginScreen(error = null, service = ServiceStatus.Checking, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Ready") {
            LoginScreen(error = null, service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Unavailable") {
            LoginScreen(error = null, service = ServiceStatus.Unavailable, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Ready + error") {
            LoginScreen(error = "No se pudo iniciar sesión", service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("RoleSelectionScreen · con nombre") {
            RoleSelectionScreen(
                displayName = "Andrés Mosquera",
                email = "tutor@example.com",
                error = null,
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("RoleSelectionScreen · sin nombre") {
            RoleSelectionScreen(
                displayName = null,
                email = "tutor@example.com",
                error = null,
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("RoleSelectionScreen · con error") {
            RoleSelectionScreen(
                displayName = "Andrés Mosquera",
                email = "tutor@example.com",
                error = "No se pudo guardar el modo",
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("TutorHomeScreen · Idle (2 dispositivos)") {
            TutorHomeScreen(
                userName = "Andrés Mosquera",
                userEmail = "tutor@example.com",
                pairing = PairingUi.Idle,
                devices = LoadState.Loaded(listOf(galleryTablet, galleryOffline)),
                now = galleryNow,
                onGenerateCode = {}, onRevokeCode = {}, onDismissPairing = {}, onRefreshDevices = {},
                onOpenDevice = {}, onOpenActivity = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("TutorHomeScreen · Active") {
            TutorHomeScreen(
                userName = "Andrés Mosquera",
                userEmail = "tutor@example.com",
                pairing = PairingUi.Active(code = "482917", remainingSeconds = 143, totalSeconds = 180),
                devices = LoadState.Loaded(listOf(galleryTablet)),
                now = galleryNow,
                onGenerateCode = {}, onRevokeCode = {}, onDismissPairing = {}, onRefreshDevices = {},
                onOpenDevice = {}, onOpenActivity = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("TutorHomeScreen · Expired (lista vacía)") {
            TutorHomeScreen(
                userName = "Andrés Mosquera",
                userEmail = "tutor@example.com",
                pairing = PairingUi.Expired,
                devices = LoadState.Loaded(emptyList()),
                now = galleryNow,
                onGenerateCode = {}, onRevokeCode = {}, onDismissPairing = {}, onRefreshDevices = {},
                onOpenDevice = {}, onOpenActivity = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("TutorHomeScreen · Failed (lista en error)") {
            TutorHomeScreen(
                userName = "Andrés Mosquera",
                userEmail = "tutor@example.com",
                pairing = PairingUi.Failed("No fue posible contactar la API"),
                devices = LoadState.Failed("Sin conexión con el servidor."),
                now = galleryNow,
                onGenerateCode = {}, onRevokeCode = {}, onDismissPairing = {}, onRefreshDevices = {},
                onOpenDevice = {}, onOpenActivity = {}, onSwitchMode = {}, onSignOut = {},
            )
        }
        ScreenFrame("DevicesScreen · Loading") {
            DevicesScreen(
                devices = LoadState.Loading,
                now = galleryNow,
                onRefresh = {},
                onOpenDevice = {},
            )
        }
        ScreenFrame("DevicesScreen · Loaded (2)") {
            DevicesScreen(
                devices = LoadState.Loaded(listOf(galleryTablet, galleryOffline)),
                now = galleryNow,
                onRefresh = {},
                onOpenDevice = {},
            )
        }
        ScreenFrame("DeviceDetailScreen · Loaded") {
            DeviceDetailScreen(
                device = LoadState.Loaded(galleryTablet),
                now = galleryNow,
                rename = null,
                unlinking = false,
                unlinkError = null,
                onBack = {}, onRefresh = {}, onStartRename = {}, onEditRename = {}, onSaveRename = {},
                onCancelRename = {}, onConfirmUnlink = {}, onOpenSection = {},
            )
        }
        ScreenFrame("DeviceDetailScreen · Renombrando con error") {
            DeviceDetailScreen(
                device = LoadState.Loaded(galleryTablet),
                now = galleryNow,
                rename = RenameUi(text = "Tablet de Sofía", error = "Sin conexión con el servidor. Revisa tu conexión e intenta de nuevo."),
                unlinking = false,
                unlinkError = null,
                onBack = {}, onRefresh = {}, onStartRename = {}, onEditRename = {}, onSaveRename = {},
                onCancelRename = {}, onConfirmUnlink = {}, onOpenSection = {},
            )
        }
        ScreenFrame("DeviceDetailScreen · Failed notFound") {
            DeviceDetailScreen(
                device = LoadState.Failed("Este dispositivo ya no existe o no tienes acceso.", notFound = true),
                now = galleryNow,
                rename = null,
                unlinking = false,
                unlinkError = null,
                onBack = {}, onRefresh = {}, onStartRename = {}, onEditRename = {}, onSaveRename = {},
                onCancelRename = {}, onConfirmUnlink = {}, onOpenSection = {},
            )
        }
        ScreenFrame("MoreScreen") {
            MoreScreen(appVersion = "0.1.0", onSwitchMode = {}, onSignOut = {})
        }
        ScreenFrame("AppsScreen · 6 apps") {
            AppsScreen(
                device = galleryTablet,
                apps = LoadState.Loaded(galleryApps),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("AppsScreen · vacío") {
            AppsScreen(
                device = galleryTablet,
                apps = LoadState.Loaded(emptyList()),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("AppsScreen · error") {
            AppsScreen(
                device = galleryTablet,
                apps = LoadState.Failed("Sin conexión con el servidor."),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("LocationScreen · dentro de Casa") {
            LocationScreen(
                device = galleryTablet,
                location = LoadState.Loaded(LocationView(galleryReport, "Casa")),
                now = galleryNow,
                onRefresh = {},
                onOpenMap = { true },
                onBack = {},
            )
        }
        ScreenFrame("LocationScreen · sin zona") {
            LocationScreen(
                device = galleryTablet,
                location = LoadState.Loaded(LocationView(galleryReport, null)),
                now = galleryNow,
                onRefresh = {},
                onOpenMap = { true },
                onBack = {},
            )
        }
        ScreenFrame("LocationScreen · sin reporte") {
            LocationScreen(
                device = galleryTablet,
                location = LoadState.Loaded(LocationView(null, null)),
                now = galleryNow,
                onRefresh = {},
                onOpenMap = { true },
                onBack = {},
            )
        }
        ScreenFrame("LocationScreen · error") {
            LocationScreen(
                device = galleryTablet,
                location = LoadState.Failed("Sin conexión con el servidor."),
                now = galleryNow,
                onRefresh = {},
                onOpenMap = { true },
                onBack = {},
            )
        }
        ScreenFrame("GeofencesScreen · 3 zonas") {
            GeofencesScreen(
                device = galleryTablet,
                geofences = LoadState.Loaded(geofencesView(galleryGeofences, galleryGeofenceEvents)),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("GeofencesScreen · vacío") {
            GeofencesScreen(
                device = galleryTablet,
                geofences = LoadState.Loaded(geofencesView(emptyList(), emptyList())),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
        ScreenFrame("GeofencesScreen · error") {
            GeofencesScreen(
                device = galleryTablet,
                geofences = LoadState.Failed("Sin conexión con el servidor."),
                today = galleryToday,
                now = galleryNow,
                onRefresh = {},
                onBack = {},
            )
        }
    }
}

private val galleryNow: Instant = Instant.parse("2026-09-29T15:00:00Z")
private val galleryToday: LocalDate = LocalDate.of(2026, 9, 29)

private val galleryTablet = DeviceSummary(
    id = "dev-tablet",
    name = "Tablet de Sofía",
    platform = "ANDROID",
    status = "ONLINE",
    lastSeenAt = galleryNow.minusSeconds(12 * 60).toString(),
    timezone = "America/Bogota",
    osVersion = "13",
    appVersion = "0.1.0",
)

private val galleryOffline = DeviceSummary(
    id = "dev-phone",
    name = "Teléfono de Sofía",
    platform = "ANDROID",
    status = "OFFLINE",
    lastSeenAt = null,
    timezone = null,
    osVersion = null,
    appVersion = null,
)

private val galleryApps = listOf(
    DeviceApplicationSummary("com.android.chrome", "Chrome", false, null, "2026-09-29", 5400),
    DeviceApplicationSummary("com.google.android.youtube", "YouTube", false, null, "2026-09-29", 3600),
    DeviceApplicationSummary("com.instagram.android", "Instagram", false, null, "2026-09-28", 1800),
    DeviceApplicationSummary("com.spotify.music", "Spotify", false, "2026-09-20T10:00:00Z", "2026-09-19", 1200),
    DeviceApplicationSummary("com.example.notes", "Notas", false, null, null, null),
    DeviceApplicationSummary("com.whatsapp", "WhatsApp", false, null, "2026-09-29", 900),
)

private val galleryReport = LocationReport(
    latitude = 4.15123,
    longitude = -73.63456,
    accuracyMeters = 200.0,
    capturedAt = galleryNow.minusSeconds(12 * 60).toString(),
    receivedAt = galleryNow.minusSeconds(10 * 60).toString(),
)

private val galleryGeofences = listOf(
    Geofence("g1", "Casa", 4.0, -73.0, 150.0),
    Geofence("g2", "Colegio", 4.5, -73.5, 300.0),
    Geofence("g3", "Parque", 4.2, -73.2, 500.0),
)

private val galleryGeofenceEvents = listOf(
    GeofenceEvent("e1", "Casa", "ENTER", galleryNow.minusSeconds(3600).toString(), "g1"),
    GeofenceEvent("e2", "Casa", "EXIT", galleryNow.minusSeconds(7200).toString(), "g1"),
    GeofenceEvent("e3", "Colegio", "EXIT", galleryNow.minusSeconds(10800).toString(), "g2"),
    GeofenceEvent("e4", "Casa", "ENTER", galleryNow.minusSeconds(14400).toString(), "g1"),
    GeofenceEvent("e5", "Colegio", "ENTER", galleryNow.minusSeconds(18000).toString(), "g2"),
)

private val galleryNowS46: Instant = Instant.parse("2026-09-29T20:00:00Z")
private val galleryNowS48: Instant = Instant.parse("2026-09-30T15:00:00Z")

private val galleryHistory = listOf(
    HistoryDay(
        date = LocalDate.of(2026, 9, 29),
        title = "Hoy",
        subtitle = "29 de septiembre de 2026",
        rows = listOf(
            HistoryRow("h1", HistoryKind.Block, "Bloqueo de app · YouTube", "Motivo: Límite diario", "DAILY_LIMIT", "3:42 p. m."),
            HistoryRow("h2", HistoryKind.Enter, "Entró · Casa", null, null, "2:10 p. m."),
            HistoryRow("h3", HistoryKind.Block, "Bloqueo de app · Instagram", "Motivo: Bloquear", "BLOCK", "11:05 a. m."),
            HistoryRow("h4", HistoryKind.Exit, "Salió · Colegio", null, null, "8:30 a. m."),
        ),
    ),
    HistoryDay(
        date = LocalDate.of(2026, 9, 28),
        title = "Ayer",
        subtitle = "28 de septiembre de 2026",
        rows = listOf(
            HistoryRow("h5", HistoryKind.Block, "Bloqueo de app · YouTube", "Motivo: Horario", "SCHEDULE", "9:15 p. m."),
            HistoryRow("h6", HistoryKind.Enter, "Entró · Parque", null, null, "4:00 p. m."),
            HistoryRow("h7", HistoryKind.Exit, "Salió · Parque", null, null, "3:30 p. m."),
        ),
    ),
)

private val galleryStatistics = StatisticsView(
    topApps = listOf(
        TopAppRow("com.google.android.youtube", "YouTube", "3 h 24 min", 1f),
        TopAppRow("com.android.chrome", "Chrome", "1 h 12 min", 0.35f),
        TopAppRow("com.instagram.android", "Instagram", "48 min", 0.23f),
        TopAppRow("com.whatsapp", "WhatsApp", "30 min", 0.15f),
    ),
    blocks = listOf(
        BlockReasonItem("BLOCK", "Bloquear", NpIcons.Ban, NpTone.Danger, 12),
        BlockReasonItem("DAILY_LIMIT", "Límite diario", NpIcons.Calendar, NpTone.Danger, 7),
        BlockReasonItem("SCHEDULE", "Horario", NpIcons.Clock, NpTone.Warning, 3),
    ),
    compliance = listOf(
        ComplianceRow("YouTube", "Límite: 60 min/día", 0.71f, "5 de 7 días"),
        ComplianceRow("Instagram", "Límite: 30 min/día", null, NO_USAGE_IN_PERIOD),
    ),
)

private val galleryAlerts = listOf(
    AlertItem("a1", "CRITICAL", "Se intentó desactivar la protección contra desinstalación", "Hoy, 3:42 p. m.", "Repetido 3 veces", unread = true, silenced = false),
    AlertItem("a2", "HIGH", "Se bloqueó YouTube", "Hoy, 2:10 p. m.", null, unread = true, silenced = false),
    AlertItem("a3", "WARNING", "Se alcanzó el límite de tiempo de Instagram", "Ayer, 6:40 p. m.", null, unread = false, silenced = true),
    AlertItem("a4", "INFO", "Salió de Casa", "Ayer, 9:15 a. m.", null, unread = false, silenced = false),
    AlertItem("a5", "INFO", "Entró a Casa", "24 de septiembre de 2026, 8:12 a. m.", null, unread = true, silenced = false),
)

private fun galleryActivityRow(id: String, action: String, subtitle: String?, time: String): ActivityRow {
    val family = AuditLabels.family(action)
    return ActivityRow(
        id = id,
        icon = family.icon,
        tone = family.tone,
        title = AuditLabels.auditActionLabel(action),
        subtitle = subtitle,
        time = time,
    )
}

private val galleryActivityDays = listOf(
    ActivityDay(
        date = LocalDate.of(2026, 9, 29),
        title = "Hoy",
        subtitle = "29 de septiembre de 2026",
        rows = listOf(
            galleryActivityRow("a1", "LOCATION_VIEWED", "Tablet de Sofía", "3:42 p. m."),
            galleryActivityRow("a2", "ALERT_SILENCED", "Alerta", "3:10 p. m."),
            galleryActivityRow("a3", "APP_RULE_UPDATED", "Regla de app", "2:05 p. m."),
            galleryActivityRow("a4", "TOKEN_REFRESH", null, "11:30 a. m."),
            galleryActivityRow("a5", "DEVICE_RENAMED", "Tablet de Sofía", "9:15 a. m."),
        ),
    ),
    ActivityDay(
        date = LocalDate.of(2026, 9, 28),
        title = "Ayer",
        subtitle = "28 de septiembre de 2026",
        rows = listOf(
            galleryActivityRow("a6", "GEOFENCE_CREATED", "Geocerca", "6:40 p. m."),
            galleryActivityRow("a7", "SCREEN_SHARE_REQUESTED", null, "4:22 p. m."),
            galleryActivityRow("a8", "LOGIN", null, "8:05 a. m."),
            galleryActivityRow("a9", "ALERT_READ", "Alerta", "7:50 a. m."),
        ),
    ),
)

@Composable
private fun ScreenFrame(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = NpText.Headline, color = NpColors.ShieldNavy)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(760.dp)
                .background(NpColors.SkyGround)
                .border(1.dp, NpColors.Hairline, NpShapes.Lg),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}
