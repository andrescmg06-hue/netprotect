package com.netprotect.app.core.network

import org.json.JSONObject

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else getString(key)

data class DeviceAlert(
    val id: String,
    val level: String,
    val alertType: String,
    val packageName: String?,
    val geofenceName: String?,
    val occurrenceCount: Int,
    val lastOccurredAt: String,
    val readAt: String?,
)

/** Sprint 17, read-only: the tutor's alert inbox, generated server-side from signals that already
 * existed (bloqueos de reglas, entradas/salidas de geocercas). Marking read and silenciar son
 * acciones de tutor de sólo escritura desde el panel web — mismo criterio ya usado para
 * crear/editar reglas y geocercas — así que este cliente sólo lista.
 */
class AlertsClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listAlerts(accessToken: String, deviceId: String): List<DeviceAlert> {
        val payload = getJson("/api/v1/devices/$deviceId/alerts", accessToken)
        val alerts = payload.getJSONArray("alerts")
        return (0 until alerts.length()).map { index ->
            val alert = alerts.getJSONObject(index)
            DeviceAlert(
                id = alert.getString("id"),
                level = alert.getString("level"),
                alertType = alert.getString("alert_type"),
                packageName = alert.optNullableString("package_name"),
                geofenceName = alert.optNullableString("geofence_name"),
                occurrenceCount = alert.getInt("occurrence_count"),
                lastOccurredAt = alert.getString("last_occurred_at"),
                readAt = alert.optNullableString("read_at"),
            )
        }
    }
}
