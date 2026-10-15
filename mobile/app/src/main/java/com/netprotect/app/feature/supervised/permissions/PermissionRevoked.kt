package com.netprotect.app.feature.supervised.permissions

import com.netprotect.app.feature.supervised.PermissionsUi

/** The permissions granted in [permissions], in the canonical order of [SupervisedPermission]. */
fun grantedPermissions(permissions: PermissionsUi): Set<SupervisedPermission> =
    SupervisedPermission.entries.filter { permissions.isGranted(it) }.toSet()

/**
 * Sprint 67 (lámina 13.4): the permissions that were granted at the previous read and are not any more, in the
 * canonical order of [SupervisedPermission]. Never granted is not revoked, so with no previous read
 * ([previouslyGranted] null: the first run, or right after a new link) there is nothing to report.
 */
fun revokedPermissions(previouslyGranted: Set<SupervisedPermission>?, now: PermissionsUi): List<SupervisedPermission> {
    if (previouslyGranted == null) return emptyList()
    return SupervisedPermission.entries.filter { it in previouslyGranted && !now.isGranted(it) }
}

const val PERMISSION_REVOKED_EYEBROW = "Permiso desactivado"
const val PERMISSION_REVOKED_TITLE = "Se desactivó un permiso"
const val PERMISSION_REVOKED_ACTIVATE = "Activar de nuevo"
const val PERMISSION_REVOKED_HOME = "Volver al inicio"

/** What the notice for [permission] says: the body (what was removed and what stops meanwhile) and the note. */
internal class PermissionRevokedTexts(val permissionTitle: String, val body: String, val note: String)

/**
 * The texts of lámina 13.4, corrected so each one is true of the code. What stops: usage access gates
 * RuleEnforcementService and the app sync, location gates LocationReportingService (both keyed in SupervisedShell);
 * overlay is what shows the block screen while the phone is in use (SupervisedShell's overlayPermissionLauncher
 * comment), so only "es posible que"; device admin is the uninstall-attempt receiver (NetProtectDeviceAdminReceiver).
 * Two permissions reach the tutor: the heartbeat's usage_access_granted becomes a PERMISSION_REVOKED alert (backend
 * services/tamper.py), and turning the device admin off fires NetProtectDeviceAdminReceiver.onDisableRequested, which
 * reports an UNINSTALL_ATTEMPT (Android's own dialog already says the tutor gets an alert). Nothing reports location or
 * overlay as removed, so their note says nothing of the tutor.
 */
internal fun permissionRevokedTexts(permission: SupervisedPermission): PermissionRevokedTexts {
    val title = permissionTexts(permission).title
    val meanwhile = when (permission) {
        SupervisedPermission.UsageAccess ->
            "Mientras tanto, NetProtect no puede aplicar las reglas de tu tutor ni saber cuánto tiempo se usan las apps."
        SupervisedPermission.Location ->
            "Mientras tanto, NetProtect no puede reportar la ubicación de este teléfono."
        SupervisedPermission.Overlay ->
            "Mientras tanto, es posible que NetProtect no pueda mostrar la pantalla de bloqueo."
        SupervisedPermission.DeviceAdmin ->
            "Mientras tanto, la protección contra desinstalación no está activa."
    }
    val note = when (permission) {
        SupervisedPermission.UsageAccess -> "Tu tutor puede ver que este permiso está desactivado."
        SupervisedPermission.DeviceAdmin -> "Al desactivarla, NetProtect avisa a tu tutor. Actívala de nuevo para volver a estar protegido."
        else -> "Activa el permiso para que NetProtect vuelva a funcionar."
    }
    return PermissionRevokedTexts(
        permissionTitle = title,
        body = "«$title» se quitó en Ajustes de Android. $meanwhile",
        note = note,
    )
}

/** Every text the notice for [permission] can show; for the wording tests. */
fun permissionRevokedAllTexts(permission: SupervisedPermission): List<String> {
    val texts = permissionRevokedTexts(permission)
    return listOf(
        PERMISSION_REVOKED_EYEBROW,
        PERMISSION_REVOKED_TITLE,
        texts.body,
        texts.permissionTitle,
        texts.note,
        PERMISSION_REVOKED_ACTIVATE,
        PERMISSION_REVOKED_HOME,
    )
}
