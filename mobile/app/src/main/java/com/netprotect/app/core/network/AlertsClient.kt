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
    /** What a silence applies to: every future alert with the same key (same signal, same app or
     * zone), not just this row. */
    val dedupKey: String = "",
)

/** A silence on one `dedup_key`; `silencedUntil == null` means indefinite. */
data class AlertSilence(
    val id: String,
    val dedupKey: String,
    val silencedUntil: String?,
)

/** Sprint 17 listed the tutor's alert inbox, generated server-side from signals that already
 * existed (bloqueos de reglas, entradas/salidas de geocercas, manipulación). Sprint 46 adds the two
 * tutor actions the web panel already had — marcar leída and silenciar — against the same endpoints
 * (authorized by `require_tutor_of_device` and audited as ALERT_READ / ALERT_SILENCED on the
 * backend; an alert of another tutor's device is a 404, never simulated here). Removing a silence
 * stays web-only (D-09 a), so this client only reads the silences.
 */
class AlertsClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun listAlerts(accessToken: String, deviceId: String): List<DeviceAlert> {
        val payload = getJson("/api/v1/devices/$deviceId/alerts", accessToken)
        val alerts = payload.getJSONArray("alerts")
        return (0 until alerts.length()).map { index -> alerts.getJSONObject(index).toDeviceAlert() }
    }

    /** POST with an empty body: the endpoint takes none. Returns the updated alert. */
    suspend fun markRead(accessToken: String, deviceId: String, alertId: String): DeviceAlert =
        sendJson("/api/v1/devices/$deviceId/alerts/$alertId/read", "POST", JSONObject(), accessToken)
            .toDeviceAlert()

    /** `days == null` silences indefinitely — the only option the app offers (D-09 a), same as the
     * web. Body: `{"days": null}` or `{"days": n}` (n > 0, validated by the backend). */
    suspend fun silence(accessToken: String, deviceId: String, alertId: String, days: Int?): AlertSilence {
        val body = JSONObject().put("days", days ?: JSONObject.NULL)
        return sendJson("/api/v1/devices/$deviceId/alerts/$alertId/silence", "POST", body, accessToken)
            .toAlertSilence()
    }

    suspend fun listSilences(accessToken: String, deviceId: String): List<AlertSilence> {
        val payload = getJson("/api/v1/devices/$deviceId/alert-silences", accessToken)
        val silences = payload.getJSONArray("silences")
        return (0 until silences.length()).map { index -> silences.getJSONObject(index).toAlertSilence() }
    }

    private fun JSONObject.toDeviceAlert() = DeviceAlert(
        id = getString("id"),
        level = getString("level"),
        alertType = getString("alert_type"),
        packageName = optNullableString("package_name"),
        geofenceName = optNullableString("geofence_name"),
        occurrenceCount = getInt("occurrence_count"),
        lastOccurredAt = getString("last_occurred_at"),
        readAt = optNullableString("read_at"),
        dedupKey = optString("dedup_key", ""),
    )

    private fun JSONObject.toAlertSilence() = AlertSilence(
        id = getString("id"),
        dedupKey = getString("dedup_key"),
        silencedUntil = optNullableString("silenced_until"),
    )
}
