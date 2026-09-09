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

    /** Fire-and-forget: reconnection on failure is intentionally not handled here. The original
     * caller (RuleEnforcementService) already polls `/rules/active` every
     * RULES_REFRESH_INTERVAL_MS regardless of this channel — losing the socket only means
     * falling back to that existing interval instead of reacting instantly, never losing
     * enforcement itself. The Sprint 23 callers accept the same terms: a dropped socket ends a
     * screen-sharing session rather than silently continuing one nobody is watching.
     *
     * [onEvent] receives the frame's name and its whole body. Server-initiated notifications name
     * themselves in `event` (`rules_changed`, Sprint 18); WebRTC signalling frames relayed from
     * the other peer name themselves in `type` (Sprint 23) — both are handed over here under one
     * name so a caller can ignore what isn't theirs.
     */
    fun connect(deviceId: String, accessToken: String, onEvent: (String, JSONObject) -> Unit) {
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
                    val body = runCatching { JSONObject(text) }.getOrNull() ?: return
                    val name = body.optString("event").ifEmpty { body.optString("type") }
                    if (name.isNotEmpty()) onEvent(name, body)
                }
            },
        )
    }

    /** Sends a frame to the backend, which relays it to the other peer on this device's channel.
     * Silently does nothing if the socket isn't open — signalling is best-effort by nature, and
     * the session's own failure handling (ICE state, or the user stopping it) is what recovers.
     */
    fun send(message: JSONObject) {
        socket?.send(message.toString())
    }

    fun disconnect() {
        socket?.close(1000, null)
        socket = null
    }
}
