package com.netprotect.app.feature.supervised.privacy

/** One line of Privacidad: a short title and what it means, in plain words. */
class PrivacyItem(val title: String, val body: String)

/** A group of [PrivacyItem]s under an eyebrow heading («Qué hace», «Vista de pantalla»). */
class PrivacySection(val heading: String, val items: List<PrivacyItem>)

const val PRIVACY_TITLE = "Privacidad"
const val PRIVACY_SUBTITLE = "Qué hace NetProtect en este teléfono."

// Every claim below was checked against the code (sprint 66, PLAN_IMPLEMENTACION §1). None says how long
// anything is kept: only location's retention is documented, and security-baseline.md defers the rest.
val privacySections: List<PrivacySection> = listOf(
    PrivacySection(
        heading = "Qué hace",
        items = listOf(
            // SupervisedShell: HEARTBEAT_INTERVAL_MS (60 s) loop. It is a LaunchedEffect, so it keeps running while
            // the shell is composed even with another app on screen (checked on the emulator: one heartbeat per
            // minute with the launcher in front). Nothing is claimed about what happens once NetProtect is closed.
            // Payload: DeviceClient.sendHeartbeat (os_version, app_version, timezone, device_time,
            // usage_access_granted, service_active = EnforcementLiveness).
            PrivacyItem(
                title = "Informa el estado del dispositivo",
                body = "Cada minuto mientras NetProtect esté en marcha, aunque uses otras apps. Incluye la versión " +
                    "de Android y de NetProtect, la zona horaria y la hora del teléfono, si el acceso al uso está " +
                    "concedido y si el control de apps está funcionando.",
            ),
            // SupervisedShell: APP_SYNC_INTERVAL_MS (5 min) loop, gated on hasUsageAccess; same lifetime as the
            // heartbeat loop (backend log: syncs at 18:15:10 and 18:20:10 with NetProtect behind another screen). Payload: ApplicationsClient.syncApplications (installed_apps with
            // name and package from AppInventoryCollector.collectInstalledApps, daily_usage = today's
            // foreground seconds per app).
            PrivacyItem(
                title = "Envía la lista de tus apps y cuánto las usas",
                body = "Las apps de tu teléfono y cuánto tiempo usaste cada una hoy, cada 5 minutos mientras " +
                    "NetProtect esté en marcha, aunque uses otras apps. Solo con el acceso al uso activado.",
            ),
            // RuleEnforcementService: BlockOverlayController / BlockScreenActivity show the reason; each block
            // is reported with RuleEnforcementClient.reportRuleEvent (package_name, rule_type_applied, occurred_at).
            PrivacyItem(
                title = "Aplica las reglas de apps de tu tutor",
                body = "Puede bloquear apps y mostrar el motivo. Cuando bloquea una, le dice a tu tutor qué app fue y cuándo.",
            ),
            // LocationReportingService: REPORT_INTERVAL_MS (15 min); GPS_PROVIDER with ACCESS_FINE_LOCATION, and only
            // NETWORK_PROVIDER when the user allowed just the approximate one (precise since 2026-10-14).
            PrivacyItem(
                title = "Comparte tu ubicación precisa",
                body = "Llega a tu tutor aproximadamente cada 15 minutos, no en tiempo real. " +
                    "Es la ubicación precisa si la permites; si solo permites la aproximada, se envía esa.",
            ),
            // NetProtectDeviceAdminReceiver.onDisableRequested → TamperReportWorker (UNINSTALL_ATTEMPT); it
            // cannot veto the deactivation, so this only warns.
            PrivacyItem(
                title = "Avisa si intentan desinstalarla",
                body = "Con la protección contra desinstalación activada, avisa a tu tutor si alguien intenta desinstalar NetProtect.",
            ),
        ),
    ),
    PrivacySection(
        heading = "Vista de pantalla",
        items = listOf(
            // Only the web panel starts a request; ScreenShareConsentScreen + Android's capture dialog decide;
            // «Detener» in ActiveShareBanner and in ScreenShareService's notification stop it.
            PrivacyItem(
                title = "Solo con tu permiso",
                body = "Tu tutor puede pedírtela. Tú decides si aceptas y puedes detenerla en cualquier momento.",
            ),
        ),
    ),
)

// PLAN §1 question 12: no ImageReader/MediaRecorder/MediaMuxer or file writes in mobile/app/src/main, no
// MediaRecorder/captureStream/toDataURL in frontend/src; the backend only relays signalling.
const val PRIVACY_NO_RECORDING = "NetProtect no graba ni guarda lo que se ve en tu pantalla."

/** Every text Privacidad shows, in reading order; for the wording tests. */
fun privacyTexts(): List<String> =
    listOf(PRIVACY_TITLE, PRIVACY_SUBTITLE) +
        privacySections.flatMap { section -> listOf(section.heading) + section.items.flatMap { listOf(it.title, it.body) } } +
        PRIVACY_NO_RECORDING
