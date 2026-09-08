package com.netprotect.app.core.network

import java.time.Instant
import org.json.JSONObject

data class DeviceSummary(
    val id: String,
    val name: String,
    val platform: String,
    val status: String,
    val lastSeenAt: String?,
    val timezone: String?,
)

data class MyDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val status: String,
    // Null means the device row exists but every tutor has since unlinked from it — that's
    // not the same as being genuinely linked, even though /devices/me still returns 200.
    val tutorLabel: String?,
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
            null
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

    /** Sprint 20: the last three arguments are the device's self-reported manipulation signals.
     * `serviceActive` is nullable on purpose — null means "no information yet" (nothing has ever
     * stamped [com.netprotect.app.core.rules.EnforcementLiveness]), and org.json drops a null
     * value from the object entirely, so the backend sees an absent field rather than `false` and
     * doesn't raise a SERVICE_INACTIVE alert for a device that simply hasn't started enforcing.
     */
    suspend fun sendHeartbeat(
        accessToken: String,
        deviceId: String,
        osVersion: String?,
        appVersion: String?,
        timezone: String?,
        usageAccessGranted: Boolean,
        serviceActive: Boolean?,
        deviceTime: Instant,
    ) {
        val body = JSONObject()
            .put("os_version", osVersion)
            .put("app_version", appVersion)
            .put("timezone", timezone)
            .put("usage_access_granted", usageAccessGranted)
            .put("service_active", serviceActive)
            .put("device_time", deviceTime.toString())
        sendJson("/api/v1/devices/$deviceId/heartbeat", "POST", body, accessToken)
    }

    /** Sprint 20: reports that someone just tried to deactivate this app's Device Administrator
     * registration — Android's mandatory first step before uninstalling it. Fired from
     * [com.netprotect.app.core.tamper.TamperReportWorker], never inline in the receiver callback:
     * `onDisableRequested` runs on the main thread and must return promptly.
     */
    suspend fun reportTamperEvent(
        accessToken: String,
        deviceId: String,
        eventType: String,
        occurredAt: Instant,
    ) {
        val body = JSONObject()
            .put("event_type", eventType)
            .put("occurred_at", occurredAt.toString())
        sendJson("/api/v1/devices/$deviceId/tamper-events", "POST", body, accessToken)
    }

    private fun JSONObject.toSummary(): DeviceSummary {
        val status = getJSONObject("status")
        return DeviceSummary(
            id = getString("id"),
            name = getString("name"),
            platform = getString("platform"),
            status = status.getString("status"),
            lastSeenAt = status.optString("last_seen_at").takeIf { it.isNotBlank() },
            timezone = optString("timezone").takeIf { it.isNotBlank() },
        )
    }
}
