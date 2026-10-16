package com.netprotect.app.core.sync

import android.content.Context
import android.os.Build
import android.os.SystemClock
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.isDeviceGone
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.rules.EnforcementLiveness
import com.netprotect.app.core.status.ServiceStatusRegistry
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Sprint 73 (T7): the cadence of the periodic reports, as pure functions of time. Kept apart from the code that
 * sends them so "is a heartbeat due?" and "is an app sync due?" have tests of their own. All times are
 * `SystemClock.elapsedRealtime()` milliseconds (monotonic: a wall clock changed by hand cannot silence or flood the
 * reports); 0 means "never".
 *
 * The heartbeat is every [HEARTBEAT_INTERVAL_MS] and the app/usage sync every [APP_SYNC_INTERVAL_MS] while the app is
 * open (the loops of SupervisedShell). With the app closed the same reports come from RuleEnforcementService
 * ([PeriodicReporter]), which only steps in as a backstop: a report is due for it when [BACKSTOP_MARGIN_MS] more than
 * the interval has passed since the last attempt or the last success by anyone in this process (screen, SyncWorker or
 * the service itself, see [ReportClock]). So with the screen open the service stays quiet instead of doubling the
 * traffic, and with it closed the cadence is about 75 s for the heartbeat (the backend marks a device offline after
 * 5 min) and 5 min 15 s for the sync.
 */
object ReportCadence {
    const val HEARTBEAT_INTERVAL_MS = 60_000L
    const val APP_SYNC_INTERVAL_MS = 5 * 60_000L

    /** Slack the service leaves the screen's loops before sending in their place. */
    const val BACKSTOP_MARGIN_MS = 15_000L

    /** Nothing sent yet (both times 0) or enough time since the last attempt and the last success. */
    fun heartbeatDue(nowMs: Long, lastAttemptMs: Long, lastSuccessMs: Long): Boolean =
        isDue(nowMs, lastAttemptMs, lastSuccessMs, HEARTBEAT_INTERVAL_MS)

    /** Same for the app/usage sync, which also needs usage access: without it nothing is sent. */
    fun appSyncDue(nowMs: Long, lastAttemptMs: Long, lastSuccessMs: Long, usageAccessGranted: Boolean): Boolean =
        usageAccessGranted && isDue(nowMs, lastAttemptMs, lastSuccessMs, APP_SYNC_INTERVAL_MS)

    private fun isDue(nowMs: Long, lastAttemptMs: Long, lastSuccessMs: Long, intervalMs: Long): Boolean {
        val last = maxOf(lastAttemptMs, lastSuccessMs)
        return last <= 0L || nowMs - last >= intervalMs + BACKSTOP_MARGIN_MS
    }
}

/** Sprint 73 (T7): when ([SystemClock.elapsedRealtime]) a heartbeat and an app sync last succeeded in this process,
 * whoever sent them. In memory only (the screen, the service and WorkManager's SyncWorker run in the same process);
 * after a restart nothing is known and the first report is due at once. */
object ReportClock {
    @Volatile var lastHeartbeatSuccessMs: Long = 0L
        private set
    @Volatile var lastAppSyncSuccessMs: Long = 0L
        private set

    fun heartbeatSucceeded(nowMs: Long) {
        lastHeartbeatSuccessMs = nowMs
    }

    fun appSyncSucceeded(nowMs: Long) {
        lastAppSyncSuccessMs = nowMs
    }

    /** Tests only. */
    internal fun reset() {
        lastHeartbeatSuccessMs = 0L
        lastAppSyncSuccessMs = 0L
    }
}

/** Sprint 73 (T7): the one place that builds the heartbeat and the app/usage sync, shared by SyncWorker,
 * RuleEnforcementService ([PeriodicReporter]) and SupervisedShell's loops, so the fields (usage_access_granted,
 * service_active, device_time...) are never copied. Both throw on failure, as the clients do: each caller decides what
 * a failure means (the loops ignore it and retry; [isDeviceGone] ends the link). A success is recorded in [ReportClock]
 * and, for the heartbeat, in [ServiceStatusRegistry] ("Estado de NetProtect").
 */
object DeviceReporter {
    /** [serviceActive] is what EnforcementLiveness says (null: nothing stamped yet, "no information"). */
    suspend fun sendHeartbeat(
        context: Context,
        client: DeviceClient,
        accessToken: String,
        deviceId: String,
        serviceActive: Boolean?,
    ) {
        client.sendHeartbeat(
            accessToken,
            deviceId,
            Build.VERSION.RELEASE,
            BuildConfig.VERSION_NAME,
            TimeZone.getDefault().id,
            // Sprint 20: read fresh on every beat, never cached — the point is noticing the moment one changes.
            usageAccessGranted = UsageAccessPermission.isGranted(context),
            serviceActive = serviceActive,
            deviceTime = Instant.now(),
        )
        ReportClock.heartbeatSucceeded(SystemClock.elapsedRealtime())
        ServiceStatusRegistry.heartbeatSucceeded(Instant.now())
    }

    suspend fun syncApps(context: Context, client: ApplicationsClient, accessToken: String, deviceId: String) {
        client.syncApplications(
            accessToken = accessToken,
            deviceId = deviceId,
            usageDate = AppInventoryCollector.todayDateString(),
            installedApps = AppInventoryCollector.collectInstalledApps(context),
            dailyUsage = AppInventoryCollector.collectTodayUsage(context),
        )
        ReportClock.appSyncSucceeded(SystemClock.elapsedRealtime())
    }
}

/** Sprint 73 (T7): RuleEnforcementService's share of the reporting, so the tutor keeps seeing the device (and its
 * apps and usage) with the app closed. [tick] is called by the service's loop (every ~3 s); it decides with
 * [ReportCadence] and, when something is due, sends it from a child coroutine of [scope], so a slow network never
 * delays the blocking cycle, and never two sends at once. Nothing thrown by a send can end the loop: a failure is
 * dropped and the next due time retries. A 404 `device_not_found` ([isDeviceGone]) is remembered in [deviceGone] for
 * the loop to end the link (SupervisionServices.endLinkFromBackground), like SyncWorker does.
 */
class PeriodicReporter(
    private val context: Context,
    private val baseUrl: String,
    private val deviceId: String,
    private val scope: CoroutineScope,
) {
    private val deviceClient = DeviceClient(baseUrl)
    private val applicationsClient = ApplicationsClient(baseUrl)
    private val gone = AtomicBoolean(false)
    private var job: Job? = null
    private var lastHeartbeatAttemptMs = 0L
    private var lastAppSyncAttemptMs = 0L

    /** True once the backend said this device has no active link any more. */
    val deviceGone: Boolean get() = gone.get()

    fun tick(nowMs: Long = SystemClock.elapsedRealtime()) {
        if (job?.isActive == true || gone.get()) return
        val heartbeat = ReportCadence.heartbeatDue(nowMs, lastHeartbeatAttemptMs, ReportClock.lastHeartbeatSuccessMs)
        val appSync = ReportCadence.appSyncDue(
            nowMs, lastAppSyncAttemptMs, ReportClock.lastAppSyncSuccessMs, UsageAccessPermission.isGranted(context),
        )
        if (!heartbeat && !appSync) return
        if (heartbeat) lastHeartbeatAttemptMs = nowMs
        if (appSync) lastAppSyncAttemptMs = nowMs
        job = scope.launch {
            // `scope` belongs to the enforcement service (a plain Job, no SupervisorJob): anything that
            // escapes this coroutine cancels the whole service scope and with it the blocking loop. An
            // Error (OutOfMemoryError listing thousands of apps, NoSuchMethodError on an odd OEM) is not
            // an Exception, so it is caught here too; only cancellation is allowed through.
            try {
                // No usable token right now (offline, session ended): try again at the next due time.
                val token = BackgroundTokenRefresher.refresh(context) ?: return@launch
                if (heartbeat) {
                    val serviceActive = EnforcementLiveness.isRecentlyActive(context)
                    if (!attempt { DeviceReporter.sendHeartbeat(context, deviceClient, token, deviceId, serviceActive) }) return@launch
                }
                if (appSync) attempt { DeviceReporter.syncApps(context, applicationsClient, token, deviceId) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                // Dropped: the next due time retries.
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    /** False when the device is gone (stop sending); every other failure is just dropped. */
    private suspend fun attempt(send: suspend () -> Unit): Boolean =
        try {
            send()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            if (failure.isDeviceGone()) {
                gone.set(true)
                false
            } else {
                true
            }
        }
}
