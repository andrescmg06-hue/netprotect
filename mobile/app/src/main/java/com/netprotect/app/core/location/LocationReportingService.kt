package com.netprotect.app.core.location

import com.netprotect.app.core.status.ServiceStatusRegistry
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.network.LocationClient
import com.netprotect.app.core.permissions.LocationAccess
import com.netprotect.app.core.permissions.LocationPermission
import com.netprotect.app.core.supervision.SupervisionServices
import java.time.Instant
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** Foreground service that reports this supervised device's location to the
 * backend every [REPORT_INTERVAL_MS] — has to keep running while the supervised person uses
 * OTHER apps, same reason [com.netprotect.app.core.rules.RuleEnforcementService] (Sprint 8)
 * exists as a service rather than a plain Compose effect.
 *
 * Declared with foregroundServiceType="location" (AndroidManifest.xml) and started only from a
 * foreground Activity (SupervisedShell's DisposableEffect, exactly like RuleEnforcementService)
 * — never from a background context. That combination is deliberate, not incidental: per
 * Android's own docs (see docs/android/capability-matrix.md, Sprint 13), an app running a
 * `location`-typed foreground service already counts as "in the foreground" for the location
 * permission system, which is what lets this project report location in the background WITHOUT
 * ever requesting ACCESS_BACKGROUND_LOCATION (and without taking on the Play "Prominent
 * Disclosure" policy that permission carries). The real cost of that choice: if the system kills
 * the process or the user swipes the app away, reporting stops until NetProtect reopens — same
 * documented limit as RuleEnforcementService, not an oversight. Sprint 73: the service is
 * START_STICKY and no longer stopped when the screen closes, but a restart from the background
 * can only report on Android 10 and older (see [canStartFromBackground]); on newer versions it
 * stops itself and waits for the app to be opened.
 *
 * Precise location since 2026-10-14 (see LocationPermission): GPS_PROVIDER first when
 * ACCESS_FINE_LOCATION is granted, NETWORK_PROVIDER as its fallback and as the only provider when
 * the user chose approximate. Plain LocationManager, still without play-services-location.
 */
class LocationReportingService : Service() {

    companion object {
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_DEVICE_ID = "device_id"

        private const val CHANNEL_ID = "location_reporting"
        private const val NOTIFICATION_ID = 1002

        /** Ongoing notification text; must match [REPORT_INTERVAL_MS]. Pinned by ForegroundNotificationTextTest. */
        internal const val NOTIFICATION_TEXT = "Tu tutor recibe tu ubicación cada 15 minutos."

        // ~15 minutes: enough to tell a tutor which zone the device is in, not to track it in
        // real time — see docs/sprint-13.md for the reasoning (and MAX_HISTORY_REPORTS in
        // backend/app/schemas/location.py, sized against this same interval).
        // Not private: "Estado" derives its "Sin confirmar" threshold from it (B2).
        internal const val REPORT_INTERVAL_MS = 15 * 60_000L

        // How long to wait for a fresh NETWORK_PROVIDER fix before moving on, rather than
        // blocking this cycle indefinitely for one that may never arrive (no connectivity, ...).
        private const val LOCATION_TIMEOUT_MS = 30_000L

        // A GPS fix from a cold start usually needs longer than a network one; indoors it may
        // never come, so it is bounded too and NETWORK_PROVIDER is tried after it.
        private const val GPS_TIMEOUT_MS = 60_000L

        // A cached fix older than one report interval belongs to an earlier cycle: reporting it
        // as this cycle's would show the tutor a place the device may have left long ago (the
        // case behind the 2026-10-14 decision). Skipping the cycle is more honest; the next one
        // tries again 15 minutes later.
        private const val MAX_LAST_KNOWN_AGE_MS = REPORT_INTERVAL_MS

        private const val TAG = "LocationReporting"

        fun start(context: Context, baseUrl: String, accessToken: String, deviceId: String) {
            val intent = Intent(context, LocationReportingService::class.java)
                .putExtra(EXTRA_BASE_URL, baseUrl)
                .putExtra(EXTRA_ACCESS_TOKEN, accessToken)
                .putExtra(EXTRA_DEVICE_ID, deviceId)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LocationReportingService::class.java))
        }

        /** Sprint 73: whether this service can do its job when started with no visible Activity
         * (boot, system restart). Location is a while-in-use permission: from Android 11 (API 30)
         * a `location` foreground service started from the background gets no location access,
         * and from Android 14 (API 34) startForeground() itself throws SecurityException — this
         * project never asks for ACCESS_BACKGROUND_LOCATION (capability matrix, Sprints 13 and
         * 73). Below API 30 there is no such restriction. Otherwise it waits for the app to open.
         */
        fun canStartFromBackground(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.R
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var reportingJob: Job? = null
    // Sprint 73: the device the running loop reports for (main thread only, like reportingJob).
    private var runningDeviceId: String? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Sprint 73: from the background (system restart, boot) Android 14+ refuses a location
        // foreground service with SecurityException, and Android 12+ may refuse any foreground
        // start (ForegroundServiceStartNotAllowedException, an IllegalStateException). Stop
        // quietly instead of crashing; opening the app starts it again from the foreground.
        try {
            startForeground(NOTIFICATION_ID, buildNotification())
        } catch (refused: SecurityException) {
            return giveUp(refused)
        } catch (refused: IllegalStateException) {
            return giveUp(refused)
        }

        // Sprint 73: with no extras (system restart) the configuration is rebuilt from what is
        // stored, but only where a background-started location service can work at all
        // (canStartFromBackground) and the location permission is still granted.
        val deviceId = intent?.getStringExtra(EXTRA_DEVICE_ID)
            ?: SupervisionServices.restorableDeviceId(this)
                ?.takeIf { canStartFromBackground() && LocationPermission.isGranted(this) }
        if (deviceId == null) {
            if (reportingJob == null) stopSelf()
            return if (reportingJob == null) START_NOT_STICKY else START_STICKY
        }
        // Idempotent: the same device keeps the running loop; a different one replaces it.
        if (reportingJob != null && deviceId == runningDeviceId) return START_STICKY
        reportingJob?.cancel()

        val baseUrl = intent?.getStringExtra(EXTRA_BASE_URL) ?: BuildConfig.API_BASE_URL
        val accessToken = intent?.getStringExtra(EXTRA_ACCESS_TOKEN).orEmpty()
        runningDeviceId = deviceId
        reportingJob = serviceScope.launch { runReportingLoop(baseUrl, accessToken, deviceId) }
        // Sprint 50: only reports its own life to "Estado de NetProtect"; changes nothing it does.
        ServiceStatusRegistry.locationRunning(true)
        // Sprint 73: if the system kills the process it brings the service back (intent == null).
        return START_STICKY
    }

    private fun giveUp(refused: Exception): Int {
        Log.w(TAG, "Could not start in the foreground: ${refused.javaClass.simpleName}")
        if (reportingJob == null) stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ServiceStatusRegistry.locationRunning(false)
        serviceJob.cancel()
        super.onDestroy()
    }

    private suspend fun runReportingLoop(baseUrl: String, accessToken: String, deviceId: String) {
        val client = LocationClient(baseUrl)
        while (serviceScope.isActive) {
            val location = runCatching { fetchCurrentLocation() }.getOrNull()
            if (location != null) {
                // Sprint 41: the token handed over at start() was never renewed, so every report
                // after its 15-minute lifetime failed silently with 401. Ask the shared session.
                val token = BackgroundTokenRefresher.refresh(applicationContext) ?: accessToken
                reportAndRecord {
                    client.reportLocation(
                        accessToken = token,
                        deviceId = deviceId,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracyMeters = location.accuracy.toDouble(),
                        capturedAt = Instant.ofEpochMilli(location.time).toString(),
                    )
                }
            }
            delay(REPORT_INTERVAL_MS)
        }
    }

    /** Null if the permission was revoked mid-run, the provider is disabled, or no fix (fresh
     * or cached) is available at all — every case is handled the same way by the caller: skip
     * this cycle, try again after the next delay.
     */
    // Lint's @RequiresPermission check only recognises a direct ContextCompat/ActivityCompat
    // call, not the LocationPermission.isGranted(this) wrapper this project uses everywhere else
    // (Sprint 13) — the check right below is real, and both calls flagged are already wrapped in
    // runCatching for the race between that check and the call landing.
    @SuppressLint("MissingPermission")
    private suspend fun fetchCurrentLocation(): Location? {
        val access = LocationPermission.access(this)
        if (access == LocationAccess.NONE) return null
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        // GPS_PROVIDER needs ACCESS_FINE_LOCATION (SecurityException with only the coarse grant),
        // so it is used only with precise access, first, and NETWORK_PROVIDER is the fallback —
        // or the only provider when the user chose approximate.
        val allowed = if (access == LocationAccess.PRECISE) {
            listOf(LocationManager.GPS_PROVIDER to GPS_TIMEOUT_MS, LocationManager.NETWORK_PROVIDER to LOCATION_TIMEOUT_MS)
        } else {
            listOf(LocationManager.NETWORK_PROVIDER to LOCATION_TIMEOUT_MS)
        }

        for ((provider, timeout) in allowed) {
            val enabled = runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
            if (!enabled) continue
            val fresh = withTimeoutOrNull(timeout) { requestSingleUpdate(locationManager, provider) }
            if (fresh != null) return fresh
        }

        // No fresh fix from any provider: the newest cached one, but only if it is recent enough
        // to describe this cycle rather than an older place (see MAX_LAST_KNOWN_AGE_MS).
        val now = System.currentTimeMillis()
        return allowed
            .mapNotNull { (provider, _) -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { now - it.time <= MAX_LAST_KNOWN_AGE_MS }
            .maxByOrNull { it.time }
    }

    private suspend fun requestSingleUpdate(locationManager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    if (continuation.isActive) continuation.resume(location)
                }
            }
            continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            try {
                locationManager.requestLocationUpdates(
                    provider,
                    /* minTimeMs = */ 0L,
                    /* minDistanceM = */ 0f,
                    listener,
                    Looper.getMainLooper(),
                )
            } catch (_: SecurityException) {
                // Permission revoked between the check above and this call — treat like "no fix".
                if (continuation.isActive) continuation.resume(null)
            }
        }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Ubicación",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("NetProtect comparte la ubicación")
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}

/**
 * Sends one report and, only if it succeeded, stamps the instant in [ServiceStatusRegistry] (B2).
 * A failure is swallowed as before and changes nothing; nothing about the location is logged or kept.
 */
internal suspend fun reportAndRecord(now: () -> Instant = Instant::now, send: suspend () -> Unit): Boolean {
    val ok = runCatching { send() }.isSuccess
    if (ok) ServiceStatusRegistry.locationReported(now())
    return ok
}
