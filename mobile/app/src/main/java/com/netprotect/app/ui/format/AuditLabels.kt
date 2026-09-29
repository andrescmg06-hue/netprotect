package com.netprotect.app.ui.format

/** Etiquetas de acciones de auditoría (25). El panel web no las traduce; **estas** son las
 * etiquetas, verificadas contra `record_audit_event` del backend (sin acciones extra). */
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
        "APP_RULE_DELETED" -> "Regla de app eliminada"
        "CATEGORY_RULE_DELETED" -> "Regla de categoría eliminada"
        "APP_CATEGORY_UNASSIGNED" -> "App quitada de su categoría"
        "GEOFENCE_CREATED" -> "Geocerca creada"
        "GEOFENCE_UPDATED" -> "Geocerca actualizada"
        "GEOFENCE_DELETED" -> "Geocerca eliminada"
        "ALERT_READ" -> "Alerta marcada como leída"
        "ALERT_SILENCED" -> "Alerta silenciada"
        "ALERT_SILENCE_REMOVED" -> "Silencio de alerta quitado"
        "SCREEN_SHARE_REQUESTED" -> "Vista remota solicitada"
        "SCREEN_SHARE_CONSENT_GRANTED" -> "Vista remota autorizada"
        "SCREEN_SHARE_CONSENT_DENIED" -> "Vista remota rechazada"
        "SCREEN_SHARE_STARTED" -> "Vista remota iniciada"
        "SCREEN_SHARE_STOPPED" -> "Vista remota terminada"
        else -> action
    }
}
