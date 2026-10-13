package com.netprotect.app.feature.supervised.permissions

/** Texts of one permission, shared by Permisos (lámina 04) and its detail (lámina 11). */
internal class PermissionTexts(val title: String, val description: String, val actionLabel: String)

internal fun permissionTexts(permission: SupervisedPermission): PermissionTexts = when (permission) {
    // ACTION_USAGE_ACCESS_SETTINGS really is a Settings screen (UsageAccessPermission.openSettings).
    SupervisedPermission.UsageAccess -> PermissionTexts(
        title = "Acceso al uso de apps",
        description = "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.",
        actionLabel = "Activar en Ajustes",
    )
    // Location is Android's own dialog, not a Settings screen: "Activar en Ajustes" would be false.
    SupervisedPermission.Location -> PermissionTexts(
        title = "Ubicación aproximada",
        description = "Permite reportar la última ubicación conocida. No se usa la ubicación precisa.",
        actionLabel = "Permitir ubicación aproximada",
    )
    // ACTION_MANAGE_OVERLAY_PERMISSION with this package: Settings' page for NetProtect (OverlayPermission).
    SupervisedPermission.Overlay -> PermissionTexts(
        title = "Mostrar sobre otras apps",
        description = "Necesario para mostrar la pantalla de bloqueo cuando corresponda.",
        actionLabel = "Activar en Ajustes",
    )
    // Pending question 7: the text says it warns the tutor, never that it prevents uninstalling.
    // ACTION_ADD_DEVICE_ADMIN is Android's activation screen, not Settings (PLAN §2.1: «Activar protección»).
    SupervisedPermission.DeviceAdmin -> PermissionTexts(
        title = "Protección contra desinstalación",
        description = "Avisa a tu tutor si alguien intenta desinstalar NetProtect.",
        actionLabel = "Activar protección",
    )
}

/** "PERMISO 3 DE 4": the position in the canonical order of [SupervisedPermission], not in the rail. */
fun permissionPositionLabel(permission: SupervisedPermission): String =
    "PERMISO ${permission.ordinal + 1} DE ${SupervisedPermission.entries.size}"

/** Last step of location: the way out when it was denied for good and the dialog no longer appears. */
const val LOCATION_DENIED_HINT = "Si el cuadro de Android no aparece, actívala desde Ajustes de la app."

const val PERMISSION_CONFIGURED_TEXT = "Android confirma que este permiso está activo."
const val PERMISSION_DISABLE_HINT = "Para desactivarlo, ve a los ajustes de Android."
const val PERMISSION_APP_SETTINGS_LABEL = "Abrir Ajustes de la app"
const val PERMISSION_BACK_LABEL = "Volver a permisos"

/**
 * The real steps of each permission (lámina 11, corrected by PLAN §2.1): they differ because each one
 * opens something different. Sources: UsageAccessPermission.openSettings (ACTION_USAGE_ACCESS_SETTINGS,
 * the system-wide list), SupervisedShell's locationPermissionLauncher (RequestPermission, a dialog),
 * OverlayPermission.requestIntent (ACTION_MANAGE_OVERLAY_PERMISSION + "package:"), and
 * DeviceAdminPermission.requestIntent (ACTION_ADD_DEVICE_ADMIN). "Lo comprobamos al volver" is the
 * shell's ON_RESUME re-read plus the launchers' result callbacks.
 */
fun permissionDetailSteps(permission: SupervisedPermission): List<String> = when (permission) {
    SupervisedPermission.UsageAccess -> listOf(
        "Toca «Activar en Ajustes». Android abre la lista de apps con acceso especial al uso, no la página de NetProtect.",
        "Busca NetProtect en esa lista, tócala y activa el permiso.",
        "Vuelve a esta pantalla. Lo comprobamos automáticamente al volver.",
    )
    SupervisedPermission.Location -> listOf(
        "Toca «Permitir ubicación aproximada». Android muestra un cuadro para permitirla; no se abren los Ajustes.",
        // Only ACCESS_COARSE_LOCATION is in the manifest and requested (LocationPermission).
        "En el cuadro, elige una opción que permita la ubicación. NetProtect solo pide la aproximada.",
        LOCATION_DENIED_HINT,
    )
    SupervisedPermission.Overlay -> listOf(
        // Android 11+ ignores the package in ACTION_MANAGE_OVERLAY_PERMISSION and opens the general list
        // (seen on the API 36 emulator: "Display over other apps"), so the page of NetProtect is not promised.
        "Toca «Activar en Ajustes». Android abre una lista de apps con este permiso, no la página de NetProtect.",
        // The switch's label changes by manufacturer, so it is described instead of quoted.
        "Busca NetProtect en la lista, tócala y activa la opción que permite mostrarla sobre otras apps.",
        "Vuelve a esta pantalla. Lo comprobamos automáticamente al volver.",
    )
    SupervisedPermission.DeviceAdmin -> listOf(
        "Toca «Activar protección». Android abre su pantalla para activar el administrador del dispositivo; no se abren los Ajustes.",
        "Lee el aviso de Android y confirma la activación.",
        "Al volver a esta pantalla, lo comprobamos automáticamente.",
    )
}

/**
 * An extra line under «Qué habilita», or null. Device admin only reports the attempt
 * (NetProtectDeviceAdminReceiver.onDisableRequested → UNINSTALL_ATTEMPT, backend services/tamper.py);
 * the deactivation and the uninstall still go through.
 */
fun permissionDetailNote(permission: SupervisedPermission): String? = when (permission) {
    SupervisedPermission.DeviceAdmin -> "Solo avisa a tu tutor: no bloquea la desinstalación."
    else -> null
}

/** Every text the detail of [permission] can show, pending or configured; for the wording tests. */
fun permissionDetailTexts(permission: SupervisedPermission): List<String> {
    val texts = permissionTexts(permission)
    return listOf(
        permissionPositionLabel(permission),
        texts.title,
        texts.description,
        texts.actionLabel,
        PERMISSION_CONFIGURED_TEXT,
        PERMISSION_DISABLE_HINT,
        PERMISSION_APP_SETTINGS_LABEL,
        PERMISSION_BACK_LABEL,
    ) + permissionDetailSteps(permission) + listOfNotNull(permissionDetailNote(permission))
}
