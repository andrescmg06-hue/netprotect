package com.netprotect.app.core.network

/** One ICE server as the backend describes it: STUN entries carry only URLs, the TURN relay
 * (Sprint 28) also carries a short-lived username/credential pair minted per request.
 */
data class IceServerConfig(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null,
)

/** Sprint 23: the ICE servers this device should use for a screen-sharing session.
 *
 * Read from the backend instead of being compiled in, so the relay and its credentials never
 * require shipping a new APK. `ice_servers` (plain STUN URLs) and `turn_servers` (relay with
 * credentials) are separate fields because the Sprint 23 APK only understands the first.
 */
class WebRtcConfigClient(baseUrl: String) : HttpJsonClient(baseUrl) {

    suspend fun getIceServers(accessToken: String, deviceId: String): List<IceServerConfig> {
        val response = getJson("/api/v1/devices/$deviceId/webrtc-config", accessToken)

        val stun = response.optJSONArray("ice_servers")?.let { urls ->
            (0 until urls.length()).map { IceServerConfig(urls = listOf(urls.getString(it))) }
        }.orEmpty()

        val turn = response.optJSONArray("turn_servers")?.let { servers ->
            (0 until servers.length()).map { index ->
                val server = servers.getJSONObject(index)
                val urls = server.getJSONArray("urls")
                IceServerConfig(
                    urls = (0 until urls.length()).map { urls.getString(it) },
                    username = server.getString("username"),
                    credential = server.getString("credential"),
                )
            }
        }.orEmpty()

        return stun + turn
    }
}
