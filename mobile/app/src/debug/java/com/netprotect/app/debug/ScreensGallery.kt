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
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.home.LoadingScreen
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.feature.home.RoleSelectionScreen
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.feature.tutor.device.DeviceDetailScreen
import com.netprotect.app.feature.tutor.device.RenameUi
import com.netprotect.app.feature.tutor.devices.DevicesScreen
import com.netprotect.app.feature.tutor.home.PairingUi
import com.netprotect.app.feature.tutor.home.TutorHomeScreen
import com.netprotect.app.feature.tutor.more.MoreScreen
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

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
    }
}

private val galleryNow: Instant = Instant.parse("2026-09-29T15:00:00Z")

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
