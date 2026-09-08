package com.netprotect.app.core.network

import org.json.JSONObject

private fun JSONObject.optNullableString(key: String): String? =
    if (isNull(key)) null else getString(key)

data class HistoryEvent(
    val id: String,
    val eventType: String,
    val occurredAt: String,
    val packageName: String?,
    val ruleTypeApplied: String?,
    val geofenceName: String?,
    val geofenceEventType: String?,
)

/** Sprint 15, read-only same as GeofenceClient: a single chronological timeline merging what
 * were already two separate event logs (bloqueos de reglas, Sprint 8; entradas/salidas de
 * geocercas, Sprint 14) via the backend's GET /devices/{id}/history — no local merging needed,
 * the server already returns one sorted list.
 */
class HistoryClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listHistory(accessToken: String, deviceId: String): List<HistoryEvent> {
        val payload = getJson("/api/v1/devices/$deviceId/history", accessToken)
        val events = payload.getJSONArray("events")
        return (0 until events.length()).map { index ->
            val event = events.getJSONObject(index)
            HistoryEvent(
                id = event.getString("id"),
                eventType = event.getString("event_type"),
                occurredAt = event.getString("occurred_at"),
                packageName = event.optNullableString("package_name"),
                ruleTypeApplied = event.optNullableString("rule_type_applied"),
                geofenceName = event.optNullableString("geofence_name"),
                geofenceEventType = event.optNullableString("geofence_event_type"),
            )
        }
    }
}
