package com.netprotect.app.feature.supervised

import java.time.Instant

/** Sprint 48: what the supervised screens receive, already computed. */

/** The four permissions the supervised mode asks for, read with the existing `core/permissions`
 * checks (never re-implemented). Notifications are not here: they are optional and asked once. */
data class PermissionsUi(
    val usageAccess: Boolean,
    // Precise location (ACCESS_FINE_LOCATION). Approximate only counts as pending: owner's decision of 2026-10-14.
    val location: Boolean,
    val overlay: Boolean,
    val deviceAdmin: Boolean,
    // Only the approximate one is allowed: still pending, but its texts say what to change.
    val locationApproximateOnly: Boolean = false,
) {
    val pending: Int
        get() = listOf(usageAccess, location, overlay, deviceAdmin).count { !it }
}

/** "2 permisos pendientes", "1 permiso pendiente" or "Todos los permisos están configurados". */
fun pendingPermissionsLabel(pending: Int): String = when (pending) {
    0 -> "Todos los permisos están configurados"
    1 -> "1 permiso pendiente"
    else -> "$pending permisos pendientes"
}

/** The pairing code the backend generates: exactly 6 digits. Pasting "123 456" or "12-34-56"
 * keeps the digits; anything longer is cut, never rejected. */
fun sanitizePairingCode(input: String): String = input.filter(Char::isDigit).take(6)

fun isCompletePairingCode(code: String): Boolean = code.length == 6 && code.all(Char::isDigit)

/** "Última comunicación": the newer of this phone's last successful heartbeat (known locally, no
 * extra request) and the server's `last_seen_at`. Null if neither exists yet. */
fun lastContact(lastHeartbeatOk: Instant?, serverLastSeenAt: String?): Instant? {
    val server = serverLastSeenAt?.let { runCatching { Instant.parse(it) }.getOrNull() }
    return listOfNotNull(lastHeartbeatOk, server).maxOrNull()
}

/** Routes inside the linked supervised mode. Only the visible screen changes — never the shell's
 * state, so no service is stopped by navigating (see SupervisedShell). Consent and Services: Sprint 50;
 * MoreSettings ("Más ajustes"): Sprint 63; Privacy and PermissionDetail (which permission is a separate
 * presentation value of the shell): Sprint 66; Battery (lámina 13.3) and PermissionRevoked (lámina 13.4, which
 * permission is again a separate presentation value): Sprint 67. */
enum class SupervisedRoute { Linked, Permissions, Consent, Services, MoreSettings, Privacy, PermissionDetail, Battery, PermissionRevoked }
