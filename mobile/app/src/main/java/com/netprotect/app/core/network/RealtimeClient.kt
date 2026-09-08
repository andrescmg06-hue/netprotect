package com.netprotect.app.core.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

/** Sprint 18 real-time channel: one WebSocket per device, authenticated by sending
 * `{"token": accessToken}` as the first frame right after the handshake completes — not a
 * header, matching the backend's app/api/v1/endpoints/realtime.py (a browser client on the web
 * panel side can't set custom headers on a WebSocket handshake either, so both clients speak the
 * same first-message protocol).
 *
 * Built on OkHttp rather than this project's usual java.net.HttpURLConnection (see
 * HttpJsonClient's docstring for why REST doesn't need a library) — plain java.net has no
 * WebSocket client at all, and java.net.http.WebSocket (JDK 11+) only reached Android at API 34,
 * well above this project's minSdk 26. A narrow, deliberate exception, not a reversal of that
 * decision.
 */
class RealtimeClient(private val baseUrl: String) {

    private val client = OkHttpClient()
    private var socket: WebSocket? = null

    /** Fire-and-forget: reconnection on failure is intentionally not handled here. The one
     * caller today (RuleEnforcementService) already polls `/rules/active` every
     * RULES_REFRESH_INTERVAL_MS regardless of this channel — losing the socket only means
     * falling back to that existing interval instead of reacting instantly, never losing
     * enforcement itself.
     */
    fun connect(deviceId: String, accessToken: String, onRulesChanged: () -> Unit) {
        val wsUrl = "${baseUrl.trimEnd('/').replaceFirst(Regex("^http"), "ws")}" +
            "/api/v1/devices/$deviceId/ws"
        val request = Request.Builder().url(wsUrl).build()
        socket = client.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    webSocket.send(JSONObject().put("token", accessToken).toString())
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val event = runCatching { JSONObject(text).optString("event") }.getOrNull()
                    if (event == "rules_changed") {
                        onRulesChanged()
                    }
                }
            },
        )
    }

    fun disconnect() {
        socket?.close(1000, null)
        socket = null
    }
}
