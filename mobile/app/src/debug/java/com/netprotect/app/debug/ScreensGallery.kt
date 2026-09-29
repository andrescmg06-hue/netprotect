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
import com.netprotect.app.feature.tutor.sections.LocationView
import com.netprotect.app.feature.tutor.sections.geofencesView
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
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
