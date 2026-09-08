package com.netprotect.app.core.network

import org.json.JSONArray
import org.json.JSONObject

data class Geofence(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)

data class GeofenceEvent(
    val id: String,
    val geofenceName: String,
    val eventType: String,
    val occurredAt: String,
)

/** Read-only from Android, same as reglas/categorías (Sprint 8-10): creating and editing
 * geofences is a web-only tutor action (see DeviceRulesPanel/DeviceCategoriesPanel — no
 * RuleClient/CategoryClient exists on this side either). The tutor screen here only shows what
 * already exists and its ENTER/EXIT history — see docs/sprint-14.md.
 *
 * No Android permission or dependency of its own: transitions are detected server-side by
 * comparing consecutive location reports (already sent by LocationReportingService, Sprint 13)
 * against each geofence, so this client is pure read access, nothing more.
 */
class GeofenceClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listGeofences(accessToken: String, deviceId: String): List<Geofence> {
        val payload = getJson("/api/v1/devices/$deviceId/geofences", accessToken)
        return payload.getJSONArray("geofences").toGeofenceList()
    }

    suspend fun listGeofenceEvents(accessToken: String, deviceId: String): List<GeofenceEvent> {
        val payload = getJson("/api/v1/devices/$deviceId/geofences/events", accessToken)
        val events = payload.getJSONArray("events")
        return (0 until events.length()).map { index ->
            val event = events.getJSONObject(index)
            GeofenceEvent(
                id = event.getString("id"),
                geofenceName = event.getString("geofence_name"),
                eventType = event.getString("event_type"),
                occurredAt = event.getString("occurred_at"),
            )
        }
    }

    private fun JSONArray.toGeofenceList(): List<Geofence> =
        (0 until length()).map { index ->
            val geofence: JSONObject = getJSONObject(index)
            Geofence(
                id = geofence.getString("id"),
                name = geofence.getString("name"),
                latitude = geofence.getDouble("latitude"),
                longitude = geofence.getDouble("longitude"),
                radiusMeters = geofence.getDouble("radius_meters"),
            )
        }
}
