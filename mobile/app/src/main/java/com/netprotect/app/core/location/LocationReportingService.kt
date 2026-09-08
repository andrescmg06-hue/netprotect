package com.netprotect.app.core.location

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.netprotect.app.core.network.LocationClient
import com.netprotect.app.core.permissions.LocationPermission
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

/** Foreground service that reports this supervised device's approximate location to the
 * backend every [REPORT_INTERVAL_MS] — has to keep running while the supervised person uses
 * OTHER apps, same reason [com.netprotect.app.core.rules.RuleEnforcementService] (Sprint 8)
 * exists as a service rather than a plain Compose effect.
 *
 * Declared with foregroundServiceType="location" (AndroidManifest.xml) and started only from a
 * foreground Activity (SupervisedScreen's DisposableEffect, exactly like RuleEnforcementService)
 * — never from a background context. That combination is deliberate, not incidental: per
 * Android's own docs (see docs/android/capability-matrix.md, Sprint 13), an app running a
 * `location`-typed foreground service already counts as "in the foreground" for the location
 * permission system, which is what lets this project report location in the background WITHOUT
 * ever requesting ACCESS_BACKGROUND_LOCATION (and without taking on the Play "Prominent
 * Disclosure" policy that permission carries). The real cost of that choice: if the system kills
 * the process or the user swipes the app away, reporting stops until NetProtect reopens — same
 * documented limit as RuleEnforcementService, not an oversight.
 *
 * Only ACCESS_COARSE_LOCATION is ever requested (see LocationPermission) — uses
 * LocationManager.NETWORK_PROVIDER exclusively; GPS_PROVIDER requires ACCESS_FINE_LOCATION and
 * would throw SecurityException with only the coarse grant.
 */
class LocationReportingService : Service() {

    companion object {
        const val EXTRA_BASE_URL = "base_url"
        const val EXTRA_ACCESS_TOKEN = "access_token"
        const val EXTRA_DEVICE_ID = "device_id"

        private const val CHANNEL_ID = "location_reporting"
        private const val NOTIFICATION_ID = 1002

        // ~15 minutes: enough to tell a tutor which zone the device is in, not to track it in
        // real time — see docs/sprint-13.md for the reasoning (and MAX_HISTORY_REPORTS in
        // backend/app/schemas/location.py, sized against this same interval).
        private const val REPORT_INTERVAL_MS = 15 * 60_000L

        // How long to wait for a fresh NETWORK_PROVIDER fix before falling back to whatever
        // last-known location the system already has cached, rather than blocking this cycle
        // indefinitely for one that may never arrive (no connectivity, provider disabled, ...).
        private const val LOCATION_TIMEOUT_MS = 30_000L

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
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var reportingJob: Job? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())

        if (intent == null || reportingJob != null) return START_NOT_STICKY
        val baseUrl = intent.getStringExtra(EXTRA_BASE_URL) ?: return START_NOT_STICKY
        val accessToken = intent.getStringExtra(EXTRA_ACCESS_TOKEN) ?: return START_NOT_STICKY
        val deviceId = intent.getStringExtra(EXTRA_DEVICE_ID) ?: return START_NOT_STICKY

        reportingJob = serviceScope.launch { runReportingLoop(baseUrl, accessToken, deviceId) }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }

    private suspend fun runReportingLoop(baseUrl: String, accessToken: String, deviceId: String) {
        val client = LocationClient(baseUrl)
        while (serviceScope.isActive) {
            val location = runCatching { fetchCurrentLocation() }.getOrNull()
            if (location != null) {
                runCatching {
                    client.reportLocation(
                        accessToken = accessToken,
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
    private suspend fun fetchCurrentLocation(): Location? {
        if (!LocationPermission.isGranted(this)) return null
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            return runCatching {
                locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }.getOrNull()
        }

        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestSingleNetworkUpdate(locationManager) }
        return fresh ?: runCatching {
            locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }.getOrNull()
    }

    private suspend fun requestSingleNetworkUpdate(locationManager: LocationManager): Location? =
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
                    LocationManager.NETWORK_PROVIDER,
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
            .setContentText("Tu tutor puede ver la ubicación aproximada de este dispositivo.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }
}
