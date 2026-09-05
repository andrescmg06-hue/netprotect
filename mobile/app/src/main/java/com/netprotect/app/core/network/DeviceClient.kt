package com.netprotect.app.core.network

import org.json.JSONObject

data class DeviceSummary(
    val id: String,
    val name: String,
    val platform: String,
    val status: String,
    val lastSeenAt: String?,
)

data class MyDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val status: String,
    val tutorLabel: String,
)

class DeviceClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listDevices(accessToken: String): List<DeviceSummary> {
        val payload = getJson("/api/v1/devices", accessToken)
        val devices = payload.getJSONArray("devices")
        return (0 until devices.length()).map { index -> devices.getJSONObject(index).toSummary() }
    }

    /** What the server currently believes about this signed-in account's own device, or null
     * if it isn't linked (server-side truth, not the local pairing cache — see
     * [com.netprotect.app.core.auth.LinkedDeviceStore]).
     */
    suspend fun getMyDevice(accessToken: String): MyDeviceInfo? = try {
        val payload = getJson("/api/v1/devices/me", accessToken)
        val tutors = payload.getJSONArray("tutors")
        val tutorLabel = if (tutors.length() == 0) {
            "Sin tutor activo"
        } else {
            (0 until tutors.length()).joinToString(", ") { index ->
                val tutor = tutors.getJSONObject(index)
                tutor.optString("display_name").takeIf { it.isNotBlank() } ?: tutor.getString("email")
            }
        }
        MyDeviceInfo(
            deviceId = payload.getString("device_id"),
            deviceName = payload.getString("device_name"),
            status = payload.getJSONObject("status").getString("status"),
            tutorLabel = tutorLabel,
        )
    } catch (exception: ApiException) {
        if (exception.statusCode == 404) null else throw exception
    }

    suspend fun renameDevice(accessToken: String, deviceId: String, name: String): DeviceSummary {
        val payload = sendJson(
            "/api/v1/devices/$deviceId",
            "PATCH",
            JSONObject().put("name", name),
            accessToken,
        )
        return payload.toSummary()
    }

    suspend fun sendHeartbeat(
        accessToken: String,
        deviceId: String,
        osVersion: String?,
        appVersion: String?,
    ) {
        val body = JSONObject()
            .put("os_version", osVersion)
            .put("app_version", appVersion)
        sendJson("/api/v1/devices/$deviceId/heartbeat", "POST", body, accessToken)
    }

    private fun JSONObject.toSummary(): DeviceSummary {
        val status = getJSONObject("status")
        return DeviceSummary(
            id = getString("id"),
            name = getString("name"),
            platform = getString("platform"),
            status = status.getString("status"),
            lastSeenAt = status.optString("last_seen_at").takeIf { it.isNotBlank() },
        )
    }
}
