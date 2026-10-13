package com.netprotect.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.statusBars
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
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.feature.home.LoadingScreen
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.feature.home.RoleCheckFailedScreen
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.feature.home.TutorAccountScreen
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.SignOutConfirmContent
import com.netprotect.app.feature.supervised.block.BlockScreenContent
import com.netprotect.app.feature.supervised.consent.ScreenShareConsentContent
import com.netprotect.app.feature.supervised.link.LinkDeviceScreen
import com.netprotect.app.feature.supervised.linked.LinkedDeviceScreen
import com.netprotect.app.feature.supervised.permissions.PermissionsScreen
import com.netprotect.app.feature.supervised.services.ServicesStatusScreen
import com.netprotect.app.feature.supervised.settings.MoreSettingsScreen
import com.netprotect.app.feature.supervised.unlinked.UnlinkedScreen
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
        // ---- Sprint 66 (al principio para las capturas). Sin Dialog. Bloqueo restilado (lámina 07). ----
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
        // ---- Sprint 65 (al principio para las capturas). Sin Dialog. ----
        ScreenFrame("Consentimiento · sin autorizar (2 min)") {
            GalleryConsent(authorized = false, requestedAt = galleryNowS65)
        }
        ScreenFrame("Consentimiento · autorizado (1 min 48 s)") {
            GalleryConsent(authorized = true, requestedAt = galleryNowS65.minusSeconds(12))
        }
        ScreenFrame("Consentimiento · sin solicitud (sin cuenta atrás)") {
            GalleryConsent(authorized = true, requestedAt = null)
        }
        ScreenFrame("Estado · todo activo") {
            GalleryServices(location = ServiceState.Active, screenShareActive = false)
        }
        ScreenFrame("Estado · vista remota en curso") {
            // As in the shell: the banner above the screen, which then must not pad for the status bar again.
            Column(Modifier.fillMaxSize()) {
                ActiveShareBanner(onStop = {})
                Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) {
                    GalleryServices(location = ServiceState.Active, screenShareActive = true)
                }
            }
        }
        ScreenFrame("Estado · ubicación sin confirmar") {
            GalleryServices(location = ServiceState.Unconfirmed, screenShareActive = false)
        }
        ScreenFrame("ActiveShareBanner") {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                ActiveShareBanner(onStop = {})
            }
        }
        // ---- Sprint 64 (al principio para las capturas). Sin Dialog. ----
        ScreenFrame("Inicio · todo funciona") {
            GalleryHome(pendingPermissions = 0, lastContact = galleryNowS64.minusSeconds(30))
        }
        ScreenFrame("Inicio · falta un paso (2 pendientes)") {
            GalleryHome(pendingPermissions = 2, lastContact = galleryNowS64.minusSeconds(30))
        }
        ScreenFrame("Inicio · falta 1 permiso") {
            GalleryHome(pendingPermissions = 1, lastContact = galleryNowS64.minusSeconds(30))
        }
        ScreenFrame("Inicio · sin reporte reciente") {
            GalleryHome(pendingPermissions = 0, lastContact = galleryNowS64.minusSeconds(12 * 60))
        }
        ScreenFrame("Inicio · vista remota en curso") {
            // As in the shell: the banner above the screen, which then must not pad for the status bar again.
            Column(Modifier.fillMaxSize()) {
                ActiveShareBanner(onStop = {})
                Box(Modifier.weight(1f).consumeWindowInsets(WindowInsets.statusBars)) {
                    GalleryHome(pendingPermissions = 0, lastContact = galleryNowS64.minusSeconds(30))
                }
            }
        }
        ScreenFrame("Permisos · dos pendientes") {
            PermissionsScreen(
                permissions = PermissionsUi(usageAccess = true, location = true, overlay = false, deviceAdmin = false),
                onOpenUsageAccessSettings = {}, onRecheckUsageAccess = {}, onRequestLocation = {}, onRecheckLocation = {},
                onRequestOverlay = {}, onRecheckOverlay = {}, onRequestDeviceAdmin = {}, onBack = {},
            )
        }
        ScreenFrame("Permisos · todos configurados") {
            PermissionsScreen(
                permissions = PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true),
                onOpenUsageAccessSettings = {}, onRecheckUsageAccess = {}, onRequestLocation = {}, onRecheckLocation = {},
                onRequestOverlay = {}, onRecheckOverlay = {}, onRequestDeviceAdmin = {}, onBack = {},
            )
        }
        ScreenFrame("UnlinkedScreen") {
            UnlinkedScreen(onRelink = {}, onSignOut = {})
        }
        // ---- Sprint 63 (al principio para las capturas). Sin Dialog: el diálogo se muestra como tarjeta. ----
        ScreenFrame("LoadingScreen") {
            LoadingScreen()
        }
        ScreenFrame("LoginScreen · servicio listo") {
            LoginScreen(error = null, service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · servicio no disponible") {
            LoginScreen(error = UiError.ServiceUnavailable.message, service = ServiceStatus.Unavailable, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · sin Internet") {
            LoginScreen(error = UiError.NoInternet.message, service = ServiceStatus.Unavailable, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · tu sesión caducó") {
            LoginScreen(error = UiError.SessionExpired.message, service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · comprobando") {
            LoginScreen(error = null, service = ServiceStatus.Checking, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · error genérico") {
            LoginScreen(error = "No se pudo iniciar sesión", service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LinkDeviceScreen · esperando el código") {
            LinkDeviceScreen(code = "", onCodeChange = {}, linking = false, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · 6 dígitos") {
            LinkDeviceScreen(code = "482915", onCodeChange = {}, linking = false, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · vinculando") {
            LinkDeviceScreen(code = "482915", onCodeChange = {}, linking = true, error = null, rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("LinkDeviceScreen · inválido o vencido") {
            LinkDeviceScreen(
                code = "482915", onCodeChange = {}, linking = false,
                error = "El código no es válido o ya venció. Pídele uno nuevo a tu tutor.",
                rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {},
            )
        }
        ScreenFrame("LinkDeviceScreen · demasiados intentos") {
            LinkDeviceScreen(
                code = "482915", onCodeChange = {}, linking = false, error = UiError.RateLimited.message,
                rechecking = false, onLink = {}, onCheckLink = {}, onSignOut = {},
            )
        }
        ScreenFrame("LinkDeviceScreen · comprobando vínculo") {
            LinkDeviceScreen(code = "", onCodeChange = {}, linking = false, error = null, rechecking = true, onLink = {}, onCheckLink = {}, onSignOut = {})
        }
        ScreenFrame("MoreSettingsScreen") {
            MoreSettingsScreen(onSignOut = {}, onBack = {})
        }
        ScreenFrame("¿Cerrar sesión? (contenido del diálogo)") {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NpColors.Navy.copy(alpha = 0.55f))
                    .padding(16.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                SignOutConfirmContent(onConfirm = {}, onCancel = {}, modifier = Modifier.fillMaxWidth())
            }
        }
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
        // ---- Sprint 50 (sus frames de consentimiento, Estado todo activo / vista remota y banner los sustituyen los de S65) ----
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
        // Sprint 49: its block frames were replaced by the Sprint 66 ones at the top.
        // Sprint 48: its Inicio and Permisos frames were replaced by the Sprint 64 ones at the top.
    }
}
private val galleryNowS50: Instant = Instant.parse("2026-10-01T15:00:00Z")
private val galleryNowS64: Instant = Instant.parse("2026-10-12T15:00:00Z")
private val galleryNowS65: Instant = Instant.parse("2026-10-13T15:00:00Z")

@Composable
private fun GalleryConsent(authorized: Boolean, requestedAt: Instant?) {
    ScreenShareConsentContent(
        authorized = authorized,
        onAuthorizedChange = {},
        onContinue = {},
        onDecline = {},
        requestedAt = requestedAt,
        now = galleryNowS65,
        onBack = {},
    )
}

@Composable
private fun GalleryServices(location: ServiceState, screenShareActive: Boolean) {
    ServicesStatusScreen(
        view = ServicesView(
            report = ServiceState.Active,
            lastReport = galleryNowS65.minusSeconds(60),
            appControl = ServiceState.Active,
            location = location,
            screenShareActive = screenShareActive,
            screenShareSince = if (screenShareActive) galleryNowS65.minusSeconds(30) else null,
        ),
        now = galleryNowS65,
        onStopScreenShare = {},
        onBack = {},
    )
}

@Composable
private fun GalleryHome(pendingPermissions: Int, lastContact: Instant?) {
    LinkedDeviceScreen(
        deviceName = "Tablet de Sofía",
        androidVersion = "13",
        tutors = listOf("Andrés Mosquera"),
        lastContact = lastContact,
        now = galleryNowS64,
        pendingPermissions = pendingPermissions,
        onOpenPermissions = {}, onOpenServices = {}, onOpenMoreSettings = {},
    )
}

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
