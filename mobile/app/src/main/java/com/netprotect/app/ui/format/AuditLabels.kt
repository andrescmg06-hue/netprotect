package com.netprotect.app.ui.format

import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpTone

/** Etiquetas de acciones de auditoría (33). Sprint 47: comprobadas contra **todas** las llamadas a
 * `record_audit_event` del backend, también las que pasan la acción en una variable (reglas y
 * categorías: antes faltaban 6). El panel web usa los mismos textos (`frontend/src/lib/auditFormatting.ts`).
 * Un código desconocido se muestra tal cual, nunca se oculta. */
object AuditLabels {
    fun auditActionLabel(action: String): String = when (action) {
        "LOGIN" -> "Inicio de sesión"
        "LOGOUT" -> "Cierre de sesión"
        "TOKEN_REFRESH" -> "Renovación de sesión"
        "ROLE_GRANTED" -> "Modo asignado"
        "PAIRING_CODE_GENERATED" -> "Código de vinculación generado"
        "PAIRING_CODE_REVOKED" -> "Código de vinculación revocado"
        "DEVICE_LINKED" -> "Dispositivo vinculado"
        "DEVICE_UNLINKED" -> "Dispositivo desvinculado"
        "DEVICE_RENAMED" -> "Dispositivo renombrado"
        "DEVICE_POLICY_CHANGED" -> "Política del dispositivo cambiada"
        "SCHOOL_MODE_CHANGED" -> "Horario escolar cambiado"
        "APP_RULE_CREATED" -> "Regla de app creada"
        "APP_RULE_UPDATED" -> "Regla de app actualizada"
        "APP_RULE_DELETED" -> "Regla de app eliminada"
        "CATEGORY_RULE_CREATED" -> "Regla de categoría creada"
        "CATEGORY_RULE_UPDATED" -> "Regla de categoría actualizada"
        "CATEGORY_RULE_DELETED" -> "Regla de categoría eliminada"
        "APP_CATEGORY_ASSIGNED" -> "App asignada a una categoría"
        "APP_CATEGORY_REASSIGNED" -> "App cambiada de categoría"
        "APP_CATEGORY_UNASSIGNED" -> "App quitada de su categoría"
        "GEOFENCE_CREATED" -> "Geocerca creada"
        "GEOFENCE_UPDATED" -> "Geocerca actualizada"
        "GEOFENCE_DELETED" -> "Geocerca eliminada"
        "ALERT_READ" -> "Alerta marcada como leída"
        "ALERT_SILENCED" -> "Alerta silenciada"
        "ALERT_SILENCE_REMOVED" -> "Silencio de alerta quitado"
        "LOCATION_VIEWED" -> "Ubicación consultada"
        "LOCATION_HISTORY_VIEWED" -> "Historial de ubicación consultado"
        "SCREEN_SHARE_REQUESTED" -> "Vista remota solicitada"
        "SCREEN_SHARE_CONSENT_GRANTED" -> "Vista remota autorizada"
        "SCREEN_SHARE_CONSENT_DENIED" -> "Vista remota rechazada"
        "SCREEN_SHARE_STARTED" -> "Vista remota iniciada"
        "SCREEN_SHARE_STOPPED" -> "Vista remota terminada"
        else -> action
    }

    /** Sprint 47: the family an action belongs to picks its icon and colour in Mi actividad.
     * Privacy-sensitive reads (location, remote view) in red so they stand out when reviewing. */
    enum class Family(val icon: Int, val tone: NpTone) {
        Session(NpIcons.KeyRound, NpTone.Neutral),
        Pairing(NpIcons.Link, NpTone.Info),
        Device(NpIcons.Smartphone, NpTone.Info),
        Rules(NpIcons.Shield, NpTone.Purple),
        Geofences(NpIcons.Map, NpTone.Success),
        Alerts(NpIcons.Bell, NpTone.Warning),
        Location(NpIcons.MapPin, NpTone.Danger),
        ScreenShare(NpIcons.Monitor, NpTone.Danger),
        Other(NpIcons.Info, NpTone.Neutral),
    }

    fun family(action: String): Family = when {
        action in setOf("LOGIN", "LOGOUT", "TOKEN_REFRESH", "ROLE_GRANTED") -> Family.Session
        action.startsWith("PAIRING_CODE_") || action == "DEVICE_LINKED" || action == "DEVICE_UNLINKED" -> Family.Pairing
        action in setOf("DEVICE_RENAMED", "DEVICE_POLICY_CHANGED", "SCHOOL_MODE_CHANGED") -> Family.Device
        action.startsWith("APP_RULE_") || action.startsWith("CATEGORY_RULE_") || action.startsWith("APP_CATEGORY_") -> Family.Rules
        action.startsWith("GEOFENCE_") -> Family.Geofences
        action.startsWith("ALERT_") -> Family.Alerts
        action.startsWith("LOCATION_") -> Family.Location
        action.startsWith("SCREEN_SHARE_") -> Family.ScreenShare
        else -> Family.Other
    }

    /** What the row says under the action: the resource, never details the audit doesn't store. A
     * device is named if it is in the tutor's current list ([deviceNames]); otherwise, or for other
     * resources, only its kind. */
    fun resourceLabel(resourceType: String?, resourceId: String?, deviceNames: Map<String, String>): String? =
        when (resourceType) {
            null, "" -> null
            "device" -> resourceId?.let { deviceNames[it] } ?: "Dispositivo"
            "alert", "alert_silence" -> "Alerta"
            "geofence" -> "Geocerca"
            "app_rule" -> "Regla de app"
            "category_rule" -> "Regla de categoría"
            "app_category_assignment" -> "Categoría de app"
            "pairing_code" -> "Código de vinculación"
            "role" -> when (resourceId) {
                "TUTOR" -> "Modo tutor"
                "SUPERVISADO" -> "Modo supervisado"
                else -> "Modo"
            }
            else -> null
        }
}
