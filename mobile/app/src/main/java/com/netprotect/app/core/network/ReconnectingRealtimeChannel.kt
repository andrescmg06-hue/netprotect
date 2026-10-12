package com.netprotect.app.core.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Sprint 50 (B-02): how long to wait before reconnection attempt number [attempt] (0-based):
 * 1, 2, 4, 8, 16 s, then every 30 s. Pure, so the schedule is tested on the JVM. */
object ReconnectPolicy {
    private const val BASE_MS = 1_000L
    const val MAX_DELAY_MS = 30_000L

    fun delayFor(attempt: Int): Long {
        if (attempt <= 0) return BASE_MS
        // Capped before shifting so a long outage can't overflow.
        val shift = attempt.coerceAtMost(5)
        return (BASE_MS shl shift).coerceAtMost(MAX_DELAY_MS)
    }

    /** The backend closed with "this device isn't yours / doesn't exist": retrying can never help. */
    fun isPermanent(closeCode: Int): Boolean = closeCode == DEVICE_NOT_FOUND

    private const val DEVICE_NOT_FOUND = 4404
}

/**
 * Sprint 50 (B-02): the supervised device's listening channel, kept open while the supervised
 * mode is on. Before, a single drop (network change, backend restart) left it closed for good and
 * every later screen-share request from the tutor was lost without a trace.
 *
 * - Each attempt asks [tokenProvider] for a valid token, and that token is still the first frame
 *   (RealtimeClient queues it before anything else) — invariant 11.
 * - Waits grow 1, 2, 4… up to 30 s between attempts ([ReconnectPolicy]) so an outage doesn't
 *   drain the battery or trip the backend's rate limit; the count resets only once the backend
 *   has said `connected` (authenticated), so a socket that opens and is immediately rejected
 *   (4401) still backs off.
 * - A 4404 (not this account's device) stops for good. [stop] closes cleanly and never retries.
 *
 * Only this listening channel reconnects. A screen-sharing session (ScreenShareService) does not:
 * if its socket drops, the session ends, as it always has.
 */
class ReconnectingRealtimeChannel(
    private val baseUrl: String,
    private val deviceId: String,
    private val tokenProvider: suspend () -> String?,
    private val onConnectedChange: (Boolean) -> Unit = {},
    private val onEvent: (String, JSONObject) -> Unit,
) {
    @Volatile private var client: RealtimeClient? = null
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job != null) return
        job = scope.launch {
            var attempt = 0
            while (isActive) {
                val token = runCatching { tokenProvider() }.getOrNull()
                if (token == null) {
                    delay(ReconnectPolicy.delayFor(attempt++))
                    continue
                }
                val closed = CompletableDeferred<Int>()
                val current = RealtimeClient(baseUrl)
                client = current
                current.connect(
                    deviceId = deviceId,
                    accessToken = token,
                    onConnection = { event ->
                        if (event is RealtimeClient.ConnectionEvent.Closed) closed.complete(event.code)
                    },
                ) { name, body ->
                    if (name == "connected") {
                        attempt = 0
                        onConnectedChange(true)
                    }
                    onEvent(name, body)
                }
                val code = closed.await()
                onConnectedChange(false)
                current.disconnect()
                if (ReconnectPolicy.isPermanent(code)) break
                delay(ReconnectPolicy.delayFor(attempt++))
            }
        }
    }

    /** Best-effort, like RealtimeClient.send: nothing is queued while disconnected. */
    fun send(message: JSONObject) {
        client?.send(message)
    }

    fun stop() {
        job?.cancel()
        job = null
        client?.disconnect()
        client = null
        onConnectedChange(false)
    }
}
