package com.netprotect.app.ui.format

import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpTone

/** Etiquetas de alerta — textos **literales** del panel web (`frontend/src/lib/alertFormatting.ts`). */
object AlertLabels {
    fun levelLabel(level: String): String = when (level) {
        "INFO" -> "Info"
        "WARNING" -> "Advertencia"
        "HIGH" -> "Alta"
        "CRITICAL" -> "Crítica"
        else -> level
    }

    fun levelTone(level: String): NpTone = when (level) {
        "INFO" -> NpTone.Info
        "WARNING" -> NpTone.Warning
        "HIGH" -> NpTone.Danger
        "CRITICAL" -> NpTone.Purple
        else -> NpTone.Neutral
    }

    fun levelIcon(level: String): Int = when (level) {
        "INFO" -> NpIcons.Info
        "WARNING" -> NpIcons.TriangleAlert
        "HIGH" -> NpIcons.OctagonAlert
        "CRITICAL" -> NpIcons.Siren
        else -> NpIcons.Info
    }

    fun alertMessage(type: String, packageName: String?, geofenceName: String?): String = when (type) {
        "APP_BLOCKED" -> "Se bloqueó $packageName"
        "APP_LIMIT_REACHED" -> "Se alcanzó el límite de tiempo de $packageName"
        "GEOFENCE_EXIT" -> "Salió de $geofenceName"
        "GEOFENCE_ENTER" -> "Entró a $geofenceName"
        "PERMISSION_REVOKED" -> "El permiso de acceso a uso de apps no está activo: el dispositivo no puede aplicar reglas"
        "SERVICE_INACTIVE" -> "El servicio de control de apps no está en ejecución en el dispositivo"
        "HEARTBEAT_SILENCE" -> "El dispositivo dejó de reportarse durante un periodo anormalmente largo"
        "CLOCK_TAMPERING" -> "La hora del dispositivo no coincide con la del servidor"
        "UNINSTALL_ATTEMPT" -> "Se intentó desactivar la protección contra desinstalación"
        else -> type
    }

    fun tamperSignalLabel(value: String): String = when (value) {
        "PERMISSION_REVOKED" -> "Permiso de acceso a uso revocado"
        "SERVICE_INACTIVE" -> "Servicio de control de apps detenido"
        "HEARTBEAT_SILENCE" -> "Silencio anómalo del dispositivo"
        "CLOCK_TAMPERING" -> "Hora del dispositivo desfasada"
        "UNINSTALL_ATTEMPT" -> "Intento de desinstalación"
        else -> value
    }
}
