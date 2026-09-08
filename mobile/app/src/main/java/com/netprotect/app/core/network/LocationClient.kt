package com.netprotect.app.core.network

import org.json.JSONObject

data class LocationReport(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Double,
    val capturedAt: String,
    val receivedAt: String,
)

/** Everything a device or a tutor needs for location (Sprint 13): reporting a fix (the
 * supervised device) and reading the latest one (the tutor). History (the full list) has no
 * caller yet — the tutor screen only shows the latest known point — so it isn't wired here; add
 * it if a future sprint needs a location trail rather than a single dot.
 */
class LocationClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun reportLocation(
        accessToken: String,
        deviceId: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        capturedAt: String,
    ) {
        val body = JSONObject()
            .put("latitude", latitude)
            .put("longitude", longitude)
            .put("accuracy_meters", accuracyMeters)
            .put("captured_at", capturedAt)
        sendJson("/api/v1/devices/$deviceId/location", "POST", body, accessToken)
    }

    /** Null when the device has never reported, or every report has aged out of the retention
     * window (app/core/config.py: location_retention_days) — both look identical to a tutor and
     * are correctly described the same way, "no ubicación reciente disponible".
     */
    suspend fun getLatestLocation(accessToken: String, deviceId: String): LocationReport? {
        val payload = getJson("/api/v1/devices/$deviceId/location/latest", accessToken)
        val report = payload.optJSONObject("report") ?: return null
        return LocationReport(
            latitude = report.getDouble("latitude"),
            longitude = report.getDouble("longitude"),
            accuracyMeters = report.getDouble("accuracy_meters"),
            capturedAt = report.getString("captured_at"),
            receivedAt = report.getString("received_at"),
        )
    }
}
