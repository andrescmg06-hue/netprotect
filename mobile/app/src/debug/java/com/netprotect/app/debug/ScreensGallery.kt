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
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.feature.home.LoadingScreen
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.feature.home.RoleCheckFailedScreen
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.feature.home.TutorAccountScreen
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.block.BlockScreenContent
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentScreen
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.feature.supervised.services.ServicesStatusScreen
import com.netprotect.app.ui.components.ActiveShareBanner
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
        // ---- Sprint 61 (al principio para las capturas) ----
        ScreenFrame("TutorAccountScreen") {
            TutorAccountScreen(loading = false, error = null, onUseAsSupervised = {}, onSignOut = {})
        }
        ScreenFrame("TutorAccountScreen · concediendo") {
            TutorAccountScreen(loading = true, error = null, onUseAsSupervised = {}, onSignOut = {})
        }
        ScreenFrame("TutorAccountScreen · error") {
            TutorAccountScreen(
                loading = false,
                error = "No hay conexión a Internet. Comprueba tu conexión e inténtalo de nuevo.",
                onUseAsSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("RoleCheckFailedScreen") {
            RoleCheckFailedScreen(
                message = "NetProtect no puede conectarse al servicio ahora. Inténtalo de nuevo en unos momentos.",
                onRetry = {},
                onSignOut = {},
            )
        }
        // ---- Sprint 50 ----
        ScreenFrame("ScreenShareConsentScreen") {
            ScreenShareConsentScreen(onContinue = {}, onDecline = {})
        }
        ScreenFrame("ServicesStatusScreen · todo activo") {
            ServicesStatusScreen(
                view = ServicesView(
                    report = ServiceState.Active,
                    lastReport = galleryNowS50.minusSeconds(60),
                    appControl = ServiceState.Active,
                    location = ServiceState.Active,
                    screenShareActive = false,
                    screenShareSince = null,
                ),
                now = galleryNowS50,
                onStopScreenShare = {},
                onBack = {},
            )
        }
        ScreenFrame("ServicesStatusScreen · control sin confirmar") {
            ServicesStatusScreen(
                view = ServicesView(
                    report = ServiceState.Active,
                    lastReport = galleryNowS50.minusSeconds(60),
                    appControl = ServiceState.Unconfirmed,
                    location = ServiceState.Inactive,
                    screenShareActive = false,
                    screenShareSince = null,
                ),
                now = galleryNowS50,
                onStopScreenShare = {},
                onBack = {},
            )
        }
        ScreenFrame("ServicesStatusScreen · vista remota") {
            ServicesStatusScreen(
                view = ServicesView(
                    report = ServiceState.Active,
                    lastReport = galleryNowS50.minusSeconds(60),
                    appControl = ServiceState.Active,
                    location = ServiceState.Active,
                    screenShareActive = true,
                    screenShareSince = galleryNowS50.minusSeconds(30),
                ),
                now = galleryNowS50,
                onStopScreenShare = {},
                onBack = {},
            )
        }
        ScreenFrame("ServicesStatusScreen · reporte sin confirmar") {
            ServicesStatusScreen(
                view = ServicesView(
                    report = ServiceState.Unconfirmed,
                    lastReport = null,
                    appControl = ServiceState.Active,
                    location = ServiceState.Active,
                    screenShareActive = false,
                    screenShareSince = null,
                ),
                now = galleryNowS50,
                onStopScreenShare = {},
                onBack = {},
            )
        }
        ScreenFrame("ActiveShareBanner") {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                ActiveShareBanner(onStop = {})
            }
        }
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
                onOpenPermissions = {}, onOpenServices = {}, onSignOut = {},
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
                onOpenPermissions = {}, onOpenServices = {}, onSignOut = {},
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
                onOpenPermissions = {}, onOpenServices = {}, onSignOut = {},
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
    }
}
private val galleryNowS48: Instant = Instant.parse("2026-09-30T15:00:00Z")
private val galleryNowS50: Instant = Instant.parse("2026-10-01T15:00:00Z")

@Composable
private fun ScreenFrame(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = NpText.Title, color = NpColors.Ink)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(760.dp)
                .background(NpColors.Cream)
                .border(1.dp, NpColors.Hairline, NpShapes.Card),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}
