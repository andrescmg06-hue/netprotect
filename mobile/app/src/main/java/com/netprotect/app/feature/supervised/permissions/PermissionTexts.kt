package com.netprotect.app.feature.supervised.permissions

/** Texts of one permission, shared by Permisos (lámina 04) and its detail (lámina 11). */
internal class PermissionTexts(val title: String, val description: String, val actionLabel: String)

/**
 * [locationApproximateOnly]: the user allowed only approximate location (Android 12+ dialog). Location then still
 * counts as pending (PermissionsUi.location is precise access), and its description says why.
 * [locationBackgroundMissing] (Sprint 73, T6): precise location is granted and only "Permitir todo el tiempo" is left
 * (Android 11+): still pending, and the texts say that this is the last step and why.
 */
internal fun permissionTexts(
    permission: SupervisedPermission,
    locationApproximateOnly: Boolean = false,
    locationBackgroundMissing: Boolean = false,
): PermissionTexts = when (permission) {
    // ACTION_USAGE_ACCESS_SETTINGS really is a Settings screen (UsageAccessPermission.openSettings).
    SupervisedPermission.UsageAccess -> PermissionTexts(
        title = "Acceso al uso de apps",
        description = "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.",
        actionLabel = "Activar en Ajustes",
    )
    // Location is Android's own dialog, not a Settings screen: "Activar en Ajustes" would be false.
    // Precise since 2026-10-14 (owner's decision, LocationPermission). Same button for both pending states: it asks
    // for FINE + COARSE, and Android 12+ answers an approximate-only grant with its own "change to precise" dialog.
    // Sprint 73 (T6): with precise location granted, the last step is "Permitir todo el tiempo" (Android 11+ asks
    // for it on its own and sends the person to Settings, after BACKGROUND_LOCATION_EXPLANATION_*).
    SupervisedPermission.Location -> PermissionTexts(
        title = "Ubicación precisa",
        description = when {
            locationApproximateOnly ->
                "Ahora solo está permitida la ubicación aproximada, que no indica bien dónde está este teléfono. Cámbiala a precisa."
            locationBackgroundMissing ->
                "Falta un último paso: permitir la ubicación todo el tiempo, para que siga llegando a tu tutor aunque NetProtect esté cerrada."
            else -> "Permite reportar a tu tutor la ubicación precisa de este teléfono cada 15 minutos."
        },
        actionLabel = if (locationBackgroundMissing && !locationApproximateOnly) BACKGROUND_LOCATION_ACTION else "Permitir ubicación precisa",
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

/** Sprint 73 (T6): the button of location's last step, and the shown-before-Android explanation it opens. What the
 * text says matches the code: LocationReportingService sends only the location, every REPORT_INTERVAL_MS (15 min),
 * to the tutor linked to this phone (backend: devices.location). «Permitir todo el tiempo» is Android's own label
 * (it can change by version or language, so the steps describe it instead of promising its exact words).
 */
const val BACKGROUND_LOCATION_ACTION = "Permitir todo el tiempo"
const val BACKGROUND_LOCATION_EXPLANATION_TITLE = "Ubicación con la app cerrada"
const val BACKGROUND_LOCATION_EXPLANATION_BODY =
    "Para que tu tutor siga recibiendo la ubicación de este teléfono aunque NetProtect esté cerrada, Android pide " +
        "permitirla «todo el tiempo». Se comparte solo la ubicación, más o menos cada 15 minutos, y solo con el tutor " +
        "vinculado a este teléfono. Con este permiso no se envía nada más. Al continuar, Android abre los Ajustes de " +
        "NetProtect para que elijas «Permitir todo el tiempo». Puedes quitarlo cuando quieras en esos mismos Ajustes."
const val BACKGROUND_LOCATION_EXPLANATION_CONTINUE = "Continuar"
const val BACKGROUND_LOCATION_EXPLANATION_DISMISS = "Ahora no"

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
fun permissionDetailSteps(
    permission: SupervisedPermission,
    locationApproximateOnly: Boolean = false,
    locationBackgroundMissing: Boolean = false,
): List<String> = when (permission) {
    SupervisedPermission.UsageAccess -> listOf(
        "Toca «Activar en Ajustes». Android abre la lista de apps con acceso especial al uso, no la página de NetProtect.",
        "Busca NetProtect en esa lista, tócala y activa el permiso.",
        "Vuelve a esta pantalla. Lo comprobamos automáticamente al volver.",
    )
    // FINE + COARSE are requested together (LocationPermission.REQUESTED). The dialog's labels change by Android
    // version and language, so they are described instead of quoted.
    // Sprint 73 (T6): the background step, after precise location (a trip to Settings, unlike the dialog).
    SupervisedPermission.Location -> if (locationBackgroundMissing && !locationApproximateOnly) {
        listOf(
            "Toca «$BACKGROUND_LOCATION_ACTION». Antes te explicamos qué se comparte; después Android abre los Ajustes de NetProtect.",
            "En la ubicación de NetProtect, elige la opción que la permite todo el tiempo, no solo con la app abierta.",
            "Vuelve a esta pantalla. Lo comprobamos automáticamente al volver.",
        )
    } else if (locationApproximateOnly) {
        listOf(
            "Toca «Permitir ubicación precisa». Android muestra un cuadro para cambiar a la ubicación precisa; no se abren los Ajustes.",
            "En el cuadro, elige la opción que cambia a la ubicación precisa.",
            LOCATION_DENIED_HINT,
        )
    } else {
        listOf(
            "Toca «Permitir ubicación precisa». Android muestra un cuadro para permitirla; no se abren los Ajustes.",
            "En el cuadro, deja elegida la ubicación precisa, no la aproximada, y elige una opción que la permita.",
            LOCATION_DENIED_HINT,
        )
    }
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
fun permissionDetailTexts(
    permission: SupervisedPermission,
    locationApproximateOnly: Boolean = false,
    locationBackgroundMissing: Boolean = false,
): List<String> {
    val texts = permissionTexts(permission, locationApproximateOnly, locationBackgroundMissing)
    return listOf(
        permissionPositionLabel(permission),
        texts.title,
        texts.description,
        texts.actionLabel,
        PERMISSION_CONFIGURED_TEXT,
        PERMISSION_DISABLE_HINT,
        PERMISSION_APP_SETTINGS_LABEL,
        PERMISSION_BACK_LABEL,
    ) + permissionDetailSteps(permission, locationApproximateOnly, locationBackgroundMissing) + listOfNotNull(permissionDetailNote(permission))
}
