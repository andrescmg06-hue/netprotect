package com.netprotect.app.core.network

import java.time.Instant
import org.json.JSONObject

data class MyDeviceInfo(
    val deviceId: String,
    val deviceName: String,
    val status: String,
    // Null means the device row exists but every tutor has since unlinked from it — that's
    // not the same as being genuinely linked, even though /devices/me still returns 200.
    val tutorLabel: String?,
    /** Sprint 48: every active tutor's name (or e-mail), for "Supervisado por". */
    val tutors: List<String> = emptyList(),
    /** Sprint 48: the server's last heartbeat from this device (ISO-8601), or null. */
    val lastSeenAt: String? = null,
)

class DeviceClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    /** What the server currently believes about this signed-in account's own device, or null
     * if it isn't linked (server-side truth, not the local pairing cache — see
     * [com.netprotect.app.core.auth.LinkedDeviceStore]).
     */
    suspend fun getMyDevice(accessToken: String, deviceInstanceId: String? = null): MyDeviceInfo? = try {
        // Sprint 48 (B-01): one account can own several device rows (a reinstall makes a new
        // instance id); saying which install this is makes the answer exact.
        val query = deviceInstanceId?.let { "?device_instance_id=" + java.net.URLEncoder.encode(it, "UTF-8") }.orEmpty()
        val payload = getJson("/api/v1/devices/me$query", accessToken)
        val tutorsJson = payload.getJSONArray("tutors")
        val tutors = (0 until tutorsJson.length()).map { index ->
            val tutor = tutorsJson.getJSONObject(index)
            tutor.optStringOrNull("display_name") ?: tutor.getString("email")
        }
        val status = payload.getJSONObject("status")
        MyDeviceInfo(
            deviceId = payload.getString("device_id"),
            deviceName = payload.getString("device_name"),
            status = status.getString("status"),
            tutorLabel = if (tutors.isEmpty()) null else tutors.joinToString(", "),
            tutors = tutors,
            lastSeenAt = if (status.isNull("last_seen_at")) null else status.getString("last_seen_at"),
        )
    } catch (exception: ApiException) {
        if (isNotLinkedAnswer(exception.statusCode, exception.message)) null else throw exception
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
}

/** The `detail` the backend sends with the 404 of `/devices/me` when the account has no linked device. */
const val NOT_LINKED_DETAIL = "not_linked"

/** Sprint 64 (B1): only the backend's own "not linked" 404 means the device was unlinked. Any other 404
 * (a proxy mid-deploy, a mis-routed path) says nothing about the link, and treating it as an unlink would
 * stop every service on the phone. */
fun isNotLinkedAnswer(statusCode: Int?, message: String?): Boolean =
    statusCode == 404 && message == NOT_LINKED_DETAIL
