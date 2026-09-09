package com.netprotect.app.core.screenshare

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.netprotect.app.core.network.RealtimeClient
import com.netprotect.app.core.network.WebRtcConfigClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.ScreenCapturerAndroid
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

/** Sprint 23. Captures this supervised device's screen and streams it to the tutor's browser over
 * WebRTC, for as long as one session lasts.
 *
 * Why a foreground service at all: Android requires one, of type `mediaProjection`, for the whole
 * duration of a capture — and requires its notification to be visible while it runs. That matches
 * what this project already decided about not being covert (Sprint 8): the person being watched
 * can always see that they are, and stop it from the notification's own action.
 *
 * Consent is per session, not a setting. Verified against Android's documentation before writing
 * this (docs/android/capability-matrix.md, Sprint 23): one MediaProjection is good for exactly one
 * createVirtualDisplay() call, and reusing the Intent from createScreenCaptureIntent() throws
 * SecurityException. So this service is handed a *fresh* projection result every time, obtained by
 * the supervised user tapping through the system dialog in SupervisedScreen — there is no stored
 * grant this service could quietly reuse later, by design of the platform and of this feature.
 *
 * It holds its own WebSocket rather than sharing RuleEnforcementService's: that one is gated on
 * usage-access permission (which has nothing to do with screen sharing), and this one must outlive
 * SupervisedScreen going to the background, which a Compose effect cannot.
 */
class ScreenShareService : Service() {

    companion object {
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_DEVICE_ID = "device_id"
        const val EXTRA_RESULT_DATA = "result_data"

        const val ACTION_STOP = "com.netprotect.app.SCREEN_SHARE_STOP"

        private const val CHANNEL_ID = "screen_share"
        private const val NOTIFICATION_ID = 1003

        // Capture is downscaled before it ever reaches the encoder: a tutor checking what their
        // child is doing needs to read the screen, not to receive it pixel-perfect, and the
        // difference is bandwidth on a phone connection.
        private const val MAX_CAPTURE_DIMENSION = 1280
        private const val CAPTURE_FRAMES_PER_SECOND = 15

        private const val VIDEO_TRACK_ID = "netprotect_screen"
        private const val STREAM_ID = "netprotect_screen_stream"

        fun start(
            context: Context,
            baseUrl: String,
            accessToken: String,
            deviceId: String,
            projectionResultData: Intent,
        ) {
            val intent = Intent(context, ScreenShareService::class.java)
                .putExtra(EXTRA_BASE_URL, baseUrl)
                .putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
                .putExtra(EXTRA_RESULT_DATA, projectionResultData)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenShareService::class.java))
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)

    private var realtimeClient: RealtimeClient? = null
    private var eglBase: EglBase? = null
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var capturer: ScreenCapturerAndroid? = null
    private var videoSource: VideoSource? = null
    private var videoTrack: VideoTrack? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    private var sessionStarted = false

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            notifyPeerAndStop("supervised_stopped")
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification())

        if (intent == null || sessionStarted) return START_NOT_STICKY
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: return START_NOT_STICKY
        val accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: return START_NOT_STICKY
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return START_NOT_STICKY
        val resultData = getResultData(intent) ?: return START_NOT_STICKY

        sessionStarted = true
        serviceScope.launch { startSession(baseUrl, accessToken, deviceId, resultData) }
        return START_NOT_STICKY
    }

    @Suppress("DEPRECATION")
    private fun getResultData(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            intent.getParcelableExtra(EXTRA_RESULT_DATA)
        }

    override fun onDestroy() {
        releaseEverything()
        serviceJob.cancel()
        super.onDestroy()
    }

    private suspend fun startSession(
        baseUrl: String,
        accessToken: String,
        deviceId: String,
        resultData: Intent,
    ) {
        // Asked of the backend rather than hardcoded so that adding a TURN server later needs no
        // new APK. If it can't be reached, fall back to no ICE servers at all: on the same LAN
        // (host-candidate to host-candidate) a connection can still succeed, and failing the whole
        // session over a config fetch would be worse than trying.
        val iceServers = runCatching {
            WebRtcConfigClient(baseUrl).getIceServers(accessToken, deviceId)
        }.getOrDefault(emptyList())

        val client = RealtimeClient(baseUrl).also { realtimeClient = it }
        client.connect(deviceId, accessToken) { event, body -> onSignal(event, body) }

        val egl = EglBase.create().also { eglBase = it }
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(applicationContext)
                .createInitializationOptions()
        )
        val factory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(egl.eglBaseContext, true, true)
            )
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(egl.eglBaseContext))
            .createPeerConnectionFactory()
            .also { peerConnectionFactory = it }

        val configuration = PeerConnection.RTCConfiguration(
            iceServers.map { PeerConnection.IceServer.builder(it).createIceServer() }
        ).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN }

        val connection = factory.createPeerConnection(configuration, PeerObserver())
            ?: run {
                stopSelf()
                return
            }
        peerConnection = connection

        startCapture(factory, egl, resultData)
        videoTrack?.let { connection.addTrack(it, listOf(STREAM_ID)) }

        connection.createOffer(
            object : SimpleSdpObserver() {
                override fun onCreateSuccess(description: SessionDescription) {
                    connection.setLocalDescription(SimpleSdpObserver(), description)
                    realtimeClient?.send(
                        JSONObject()
                            .put("type", "screen_share_offer")
                            .put("sdp", description.description)
                    )
                }
            },
            MediaConstraints(),
        )
    }

    /** The MediaProjection.Callback handed to ScreenCapturerAndroid is not optional decoration:
     * Android throws IllegalStateException from createVirtualDisplay() if no callback is
     * registered. It also fires when the system itself ends the projection — the user tapping the
     * status-bar chip, or (Android 15 QPR1+) simply locking the screen — which is exactly when
     * this session has to be torn down and the tutor told it ended.
     */
    private fun startCapture(
        factory: PeerConnectionFactory,
        egl: EglBase,
        projectionResultData: Intent,
    ) {
        val metrics = captureMetrics()
        val screenCapturer = ScreenCapturerAndroid(
            projectionResultData,
            object : MediaProjection.Callback() {
                override fun onStop() {
                    notifyPeerAndStop("projection_stopped")
                }
            },
        ).also { capturer = it }

        val helper = SurfaceTextureHelper.create("ScreenCapture", egl.eglBaseContext)
            .also { surfaceTextureHelper = it }
        val source = factory.createVideoSource(/* isScreencast = */ true).also { videoSource = it }
        screenCapturer.initialize(helper, applicationContext, source.capturerObserver)
        screenCapturer.startCapture(metrics.first, metrics.second, CAPTURE_FRAMES_PER_SECOND)

        videoTrack = factory.createVideoTrack(VIDEO_TRACK_ID, source)
    }

    /** Width and height to capture at: the display's own aspect ratio, scaled down so neither
     * side exceeds MAX_CAPTURE_DIMENSION.
     */
    private fun captureMetrics(): Pair<Int, Int> {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        (getSystemService(WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
        val longest = maxOf(metrics.widthPixels, metrics.heightPixels)
        if (longest <= MAX_CAPTURE_DIMENSION) return metrics.widthPixels to metrics.heightPixels
        val scale = MAX_CAPTURE_DIMENSION.toDouble() / longest
        // Even dimensions: video encoders reject odd ones.
        fun even(value: Int) = (value / 2) * 2
        return even((metrics.widthPixels * scale).toInt()) to
            even((metrics.heightPixels * scale).toInt())
    }

    private fun onSignal(event: String, body: JSONObject) {
        when (event) {
            "screen_share_answer" -> {
                val sdp = body.optString("sdp")
                if (sdp.isNotEmpty()) {
                    peerConnection?.setRemoteDescription(
                        SimpleSdpObserver(),
                        SessionDescription(SessionDescription.Type.ANSWER, sdp),
                    )
                }
            }

            "screen_share_ice_candidate" -> {
                val candidate = body.optString("candidate")
                if (candidate.isNotEmpty()) {
                    peerConnection?.addIceCandidate(
                        IceCandidate(
                            body.optString("sdp_mid"),
                            body.optInt("sdp_m_line_index", 0),
                            candidate,
                        )
                    )
                }
            }

            // The tutor closed the viewer. Nothing to answer — capture stops here.
            "screen_share_stop" -> stopSelf()
        }
    }

    /** Tells the tutor's side why the stream is about to end before tearing it down. Without this
     * the viewer would just see the video freeze and have to guess.
     */
    private fun notifyPeerAndStop(reason: String) {
        realtimeClient?.send(
            JSONObject().put("type", "screen_share_stop").put("reason", reason)
        )
        stopSelf()
    }

    private fun releaseEverything() {
        runCatching { capturer?.stopCapture() }
        capturer?.dispose()
        videoTrack?.dispose()
        videoSource?.dispose()
        surfaceTextureHelper?.dispose()
        peerConnection?.dispose()
        peerConnectionFactory?.dispose()
        eglBase?.release()
        realtimeClient?.disconnect()
        capturer = null
        videoTrack = null
        videoSource = null
        surfaceTextureHelper = null
        peerConnection = null
        peerConnectionFactory = null
        eglBase = null
        realtimeClient = null
    }

    private inner class PeerObserver : PeerConnection.Observer {
        override fun onIceCandidate(candidate: IceCandidate) {
            realtimeClient?.send(
                JSONObject()
                    .put("type", "screen_share_ice_candidate")
                    .put("candidate", candidate.sdp)
                    .put("sdp_mid", candidate.sdpMid)
                    .put("sdp_m_line_index", candidate.sdpMLineIndex)
            )
        }

        /** The only end-of-session signal that survives the tutor's browser closing without
         * warning: there is no session state on the backend to time one out (see
         * docs/sprint-23.md), so the peer connection's own state is what ends capture.
         */
        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            if (state == PeerConnection.IceConnectionState.FAILED ||
                state == PeerConnection.IceConnectionState.CLOSED ||
                state == PeerConnection.IceConnectionState.DISCONNECTED
            ) {
                stopSelf()
            }
        }

        override fun onSignalingChange(state: PeerConnection.SignalingState) = Unit
        override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) = Unit
        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
        override fun onAddStream(stream: MediaStream) = Unit
        override fun onRemoveStream(stream: MediaStream) = Unit
        override fun onDataChannel(channel: org.webrtc.DataChannel) = Unit
        override fun onRenegotiationNeeded() = Unit
    }

    private open inner class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(description: SessionDescription) = Unit
        override fun onSetSuccess() = Unit
        override fun onCreateFailure(error: String?) = Unit
        override fun onSetFailure(error: String?) = Unit
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Pantalla compartida",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, ScreenShareService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Tu tutor está viendo esta pantalla")
            .setContentText("La transmisión está activa. Puedes detenerla cuando quieras.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", stopIntent)
            .build()
    }
}
