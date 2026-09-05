package com.netprotect.app.core.network

import org.json.JSONObject

data class PairingCode(
    val code: String,
    val expiresInSeconds: Int,
)

data class LinkedTutor(
    val displayName: String?,
    val email: String,
)

data class RedeemResult(
    val deviceId: String,
    val deviceName: String,
    val tutor: LinkedTutor,
)

class PairingClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun generateCode(accessToken: String): PairingCode {
        val payload = sendJson("/api/v1/pairing/codes", "POST", JSONObject(), accessToken)
        return PairingCode(
            code = payload.getString("code"),
            expiresInSeconds = payload.getInt("expires_in_seconds"),
        )
    }

    suspend fun revokeCurrentCode(accessToken: String) {
        deleteForJson("/api/v1/pairing/codes/current", accessToken)
    }

    suspend fun redeem(
        accessToken: String,
        code: String,
        deviceInstanceId: String,
        deviceName: String,
        osVersion: String?,
        appVersion: String?,
    ): RedeemResult {
        val body = JSONObject()
            .put("code", code)
            .put("device_instance_id", deviceInstanceId)
            .put("device_name", deviceName)
            .put("platform", "ANDROID")
            .put("os_version", osVersion)
            .put("app_version", appVersion)

        val payload = sendJson("/api/v1/pairing/redeem", "POST", body, accessToken)
        val tutor = payload.getJSONObject("tutor")
        return RedeemResult(
            deviceId = payload.getString("device_id"),
            deviceName = payload.getString("device_name"),
            tutor = LinkedTutor(
                displayName = tutor.optString("display_name").takeIf { it.isNotBlank() },
                email = tutor.getString("email"),
            ),
        )
    }

    suspend fun unlinkDevice(accessToken: String, deviceId: String) {
        deleteForJson("/api/v1/devices/$deviceId/link", accessToken)
    }
}
