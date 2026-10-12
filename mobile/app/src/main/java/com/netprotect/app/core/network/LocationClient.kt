package com.netprotect.app.core.network

import org.json.JSONObject

/** Location reporting (Sprint 13): the supervised device sends its fixes. */
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

}
