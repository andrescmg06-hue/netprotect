package com.netprotect.app.ui.format

import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpTone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsTest {

    @Test
    fun alertLevelLabels() {
        val expected = mapOf(
            "INFO" to "Info",
            "WARNING" to "Advertencia",
            "HIGH" to "Alta",
            "CRITICAL" to "Crítica",
        )
        for ((level, label) in expected) {
            assertEquals(label, AlertLabels.levelLabel(level))
            assertTrue(AlertLabels.levelLabel(level).isNotEmpty())
            assertNotEquals(level, AlertLabels.levelLabel(level))
        }
    }

    @Test
    fun alertLevelTones() {
        assertEquals(NpTone.Info, AlertLabels.levelTone("INFO"))
        assertEquals(NpTone.Warning, AlertLabels.levelTone("WARNING"))
        assertEquals(NpTone.Danger, AlertLabels.levelTone("HIGH"))
        assertEquals(NpTone.Purple, AlertLabels.levelTone("CRITICAL"))
    }

    @Test
    fun alertLevelIcons() {
        assertEquals(NpIcons.Info, AlertLabels.levelIcon("INFO"))
        assertEquals(NpIcons.TriangleAlert, AlertLabels.levelIcon("WARNING"))
        assertEquals(NpIcons.OctagonAlert, AlertLabels.levelIcon("HIGH"))
        assertEquals(NpIcons.Siren, AlertLabels.levelIcon("CRITICAL"))
    }

    @Test
    fun alertMessages() {
        assertEquals(
            "Se bloqueó com.instagram.android",
            AlertLabels.alertMessage("APP_BLOCKED", "com.instagram.android", null),
        )
        assertEquals(
            "Se alcanzó el límite de tiempo de com.instagram.android",
            AlertLabels.alertMessage("APP_LIMIT_REACHED", "com.instagram.android", null),
        )
        assertEquals("Salió de Zona escolar", AlertLabels.alertMessage("GEOFENCE_EXIT", null, "Zona escolar"))
        assertEquals("Entró a Zona escolar", AlertLabels.alertMessage("GEOFENCE_ENTER", null, "Zona escolar"))
        assertEquals(
            "El permiso de acceso a uso de apps no está activo: el dispositivo no puede aplicar reglas",
            AlertLabels.alertMessage("PERMISSION_REVOKED", null, null),
        )
        assertEquals(
            "El servicio de control de apps no está en ejecución en el dispositivo",
            AlertLabels.alertMessage("SERVICE_INACTIVE", null, null),
        )
        assertEquals(
            "El dispositivo dejó de reportarse durante un periodo anormalmente largo",
            AlertLabels.alertMessage("HEARTBEAT_SILENCE", null, null),
        )
        assertEquals(
            "La hora del dispositivo no coincide con la del servidor",
            AlertLabels.alertMessage("CLOCK_TAMPERING", null, null),
        )
        assertEquals(
            "Se intentó desactivar la protección contra desinstalación",
            AlertLabels.alertMessage("UNINSTALL_ATTEMPT", null, null),
        )
    }

    @Test
    fun tamperSignalLabels() {
        val expected = mapOf(
            "PERMISSION_REVOKED" to "Permiso de acceso a uso revocado",
            "SERVICE_INACTIVE" to "Servicio de control de apps detenido",
            "HEARTBEAT_SILENCE" to "Silencio anómalo del dispositivo",
            "CLOCK_TAMPERING" to "Hora del dispositivo desfasada",
            "UNINSTALL_ATTEMPT" to "Intento de desinstalación",
        )
        for ((signal, label) in expected) {
            assertEquals(label, AlertLabels.tamperSignalLabel(signal))
            assertNotEquals(signal, AlertLabels.tamperSignalLabel(signal))
        }
    }

    @Test
    fun ruleTypeLabels() {
        val expected = mapOf(
            "ALLOW" to "Permitir",
            "BLOCK" to "Bloquear",
            "DAILY_LIMIT" to "Límite diario",
            "WEEKLY_LIMIT" to "Límite semanal",
            "SCHEDULE" to "Horario",
            "CATEGORY" to "Por categoría",
            "SCHOOL_MODE" to "Horario escolar",
            "DEFAULT_POLICY" to "Sin aprobar",
        )
        for ((type, label) in expected) {
            assertEquals(label, RuleLabels.ruleTypeLabel(type))
            assertTrue(RuleLabels.ruleTypeLabel(type).isNotEmpty())
            assertNotEquals(type, RuleLabels.ruleTypeLabel(type))
        }
    }

    @Test
    fun categoryLabels() {
        val expected = mapOf(
            "SOCIAL_MEDIA" to "Redes sociales",
            "GAMES" to "Juegos",
            "STREAMING" to "Streaming",
            "EDUCATION" to "Educación",
            "PRODUCTIVITY" to "Productividad",
            "COMMUNICATION" to "Comunicación",
            "NEWS" to "Noticias",
            "SHOPPING" to "Compras",
            "FINANCE" to "Finanzas",
            "UTILITIES" to "Utilidades",
            "ADULT_CONTENT" to "Contenido para adultos",
        )
        for ((category, label) in expected) {
            assertEquals(label, CategoryLabels.categoryLabel(category))
            assertTrue(CategoryLabels.categoryLabel(category).isNotEmpty())
            assertNotEquals(category, CategoryLabels.categoryLabel(category))
        }
    }

    @Test
    fun deviceStatusLabels() {
        val expected = mapOf(
            "ONLINE" to ("En línea" to NpTone.Success),
            "OFFLINE" to ("Desconectado" to NpTone.Neutral),
            "ALERT" to ("Alerta" to NpTone.Danger),
            "SYNCING" to ("Sincronizando" to NpTone.Info),
            "RESTRICTED" to ("Restringido" to NpTone.Warning),
            "UNLINKED" to ("Desvinculado" to NpTone.Neutral),
        )
        for ((status, pair) in expected) {
            assertEquals(pair, DeviceStatusLabels.label(status))
            assertTrue(DeviceStatusLabels.label(status).first.isNotEmpty())
            assertNotEquals(status, DeviceStatusLabels.label(status).first)
        }
    }

    @Test
    fun auditActionLabels() {
        val expected = mapOf(
            "LOGIN" to "Inicio de sesión",
            "LOGOUT" to "Cierre de sesión",
            "TOKEN_REFRESH" to "Renovación de sesión",
            "ROLE_GRANTED" to "Modo asignado",
            "PAIRING_CODE_GENERATED" to "Código de vinculación generado",
            "PAIRING_CODE_REVOKED" to "Código de vinculación revocado",
            "DEVICE_LINKED" to "Dispositivo vinculado",
            "DEVICE_UNLINKED" to "Dispositivo desvinculado",
            "DEVICE_RENAMED" to "Dispositivo renombrado",
            "DEVICE_POLICY_CHANGED" to "Política del dispositivo cambiada",
            "SCHOOL_MODE_CHANGED" to "Horario escolar cambiado",
            "APP_RULE_DELETED" to "Regla de app eliminada",
            "CATEGORY_RULE_DELETED" to "Regla de categoría eliminada",
            "APP_CATEGORY_UNASSIGNED" to "App quitada de su categoría",
            "GEOFENCE_CREATED" to "Geocerca creada",
            "GEOFENCE_UPDATED" to "Geocerca actualizada",
            "GEOFENCE_DELETED" to "Geocerca eliminada",
            "ALERT_READ" to "Alerta marcada como leída",
            "ALERT_SILENCED" to "Alerta silenciada",
            "ALERT_SILENCE_REMOVED" to "Silencio de alerta quitado",
            "SCREEN_SHARE_REQUESTED" to "Vista remota solicitada",
            "SCREEN_SHARE_CONSENT_GRANTED" to "Vista remota autorizada",
            "SCREEN_SHARE_CONSENT_DENIED" to "Vista remota rechazada",
            "SCREEN_SHARE_STARTED" to "Vista remota iniciada",
            "SCREEN_SHARE_STOPPED" to "Vista remota terminada",
        )
        for ((action, label) in expected) {
            assertEquals(label, AuditLabels.auditActionLabel(action))
            assertTrue(AuditLabels.auditActionLabel(action).isNotEmpty())
            assertNotEquals(action, AuditLabels.auditActionLabel(action))
        }
    }

    @Test
    fun unknownValuesFallBackToTheRawValue() {
        assertEquals("NUEVO_VALOR", AlertLabels.levelLabel("NUEVO_VALOR"))
        assertEquals("NUEVO_VALOR", AlertLabels.tamperSignalLabel("NUEVO_VALOR"))
        assertEquals("NUEVO_VALOR", AlertLabels.alertMessage("NUEVO_VALOR", null, null))
        assertEquals("NUEVO_VALOR", RuleLabels.ruleTypeLabel("NUEVO_VALOR"))
        assertEquals("NUEVO_VALOR", CategoryLabels.categoryLabel("NUEVO_VALOR"))
        assertEquals("NUEVO_VALOR", DeviceStatusLabels.label("NUEVO_VALOR").first)
        assertEquals("NUEVO_VALOR", AuditLabels.auditActionLabel("NUEVO_VALOR"))
    }
}
