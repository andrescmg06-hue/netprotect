package com.netprotect.app.core.network

/** Sprint 23: the ICE servers this device should use for a screen-sharing session.
 *
 * Read from the backend instead of being compiled in so that adding a TURN server (which this
 * project does not have — see docs/sprint-23.md) never requires shipping a new APK.
 */
class WebRtcConfigClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun getIceServers(accessToken: String, deviceId: String): List<String> {
        val response = getJson("/api/v1/devices/$deviceId/webrtc-config", accessToken)
        val urls = response.optJSONArray("ice_servers") ?: return emptyList()
        return (0 until urls.length()).map { urls.getString(it) }
    }
}
