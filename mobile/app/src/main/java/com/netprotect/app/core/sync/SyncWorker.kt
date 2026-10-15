package com.netprotect.app.core.sync

import android.content.Context
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.inventory.AppInventoryCollector
import com.netprotect.app.core.network.ApplicationsClient
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.isDeviceGone
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.storage.NetProtectDatabase
import com.netprotect.app.core.storage.PendingRuleEventStore
import com.netprotect.app.core.supervision.SupervisionServices
import java.time.Instant
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val KEY_BASE_URL = "base_url"
private const val KEY_DEVICE_ID = "device_id"
private const val UNIQUE_WORK_NAME = "netprotect_sync"

/** Sprint 19: does, on a schedule, what SupervisedScreen's foreground-only `LaunchedEffect`
 * loops (heartbeat, app/usage sync) already do while the screen is composed, plus flushing
 * `PendingRuleEventStore` — this is the piece the screen's own comment on the heartbeat loop
 * pointed at ("a real background schedule (WorkManager) is Sprint 19's job").
 *
 * Mints its own access token via BackgroundTokenRefresher instead of taking one as input: a
 * token handed in at schedule time would already be expired well before WorkManager's 15-minute
 * floor interval elapses (`access_token_ttl_minutes` is also 15). Neither `deviceId` nor
 * `base_url` needs the same treatment — they don't expire.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val baseUrl = inputData.getString(KEY_BASE_URL) ?: return Result.failure()
        val deviceId = inputData.getString(KEY_DEVICE_ID) ?: return Result.failure()

        // Sprint 73: the watch. Sprint 73 (T5): first, and local — it used to run only after the
        // network work, so with no token or no network it never ran (EnforcementWatchWorker, with
        // no network constraint, is now the main watch; this one is a second chance). The heartbeat
        // below still reports what the marker said BEFORE this restart (a restart stamps it at
        // once), so the tutor is told the service had died. A deliberate stop never gets here:
        // sign-out and unlink cancel this worker (SupervisionServices.stopAll) and clear the link or
        // session the watch checks.
        val serviceActive = SupervisionServices.restartEnforcementIfDead(applicationContext)

        // No stored session (signed out) or genuinely offline: nothing to do, and not a
        // worker "failure" either way — WorkManager's exponential backoff on Result.retry()
        // is for transient errors, not for "there was nothing to sync this cycle".
        val accessToken = BackgroundTokenRefresher.refresh(applicationContext)
            ?: return Result.success()

        // Sprint 20: this is the heartbeat that matters most for manipulation detection — it
        // keeps running when SupervisedScreen isn't composed, which is exactly when the
        // enforcement service is most likely to have been stopped without anyone noticing.
        val heartbeat = runCatching {
            DeviceClient(baseUrl).sendHeartbeat(
                accessToken,
                deviceId,
                Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME,
                TimeZone.getDefault().id,
                usageAccessGranted = UsageAccessPermission.isGranted(applicationContext),
                serviceActive = serviceActive,
                deviceTime = Instant.now(),
            )
        }
        // Sprint 73 (T5): the backend refuses a device with no active link (uniform 404). With the
        // app closed this worker is what notices an unlink: end it here instead of reporting usage
        // and enforcing old rules for a device nobody supervises.
        if (heartbeat.exceptionOrNull()?.isDeviceGone() == true) return endLink(deviceId)

        if (UsageAccessPermission.isGranted(applicationContext)) {
            val sync = runCatching {
                ApplicationsClient(baseUrl).syncApplications(
                    accessToken = accessToken,
                    deviceId = deviceId,
                    usageDate = AppInventoryCollector.todayDateString(),
                    installedApps = AppInventoryCollector.collectInstalledApps(applicationContext),
                    dailyUsage = AppInventoryCollector.collectTodayUsage(applicationContext),
                )
            }
            if (sync.exceptionOrNull()?.isDeviceGone() == true) return endLink(deviceId)
        }

        val database = NetProtectDatabase.getInstance(applicationContext)
        val flush = runCatching {
            PendingRuleEventStore(database).flush(RuleEnforcementClient(baseUrl), accessToken, deviceId)
        }
        if (flush.exceptionOrNull()?.isDeviceGone() == true) return endLink(deviceId)

        return Result.success()
    }

    private suspend fun endLink(deviceId: String): Result {
        SupervisionServices.endLinkFromBackground(applicationContext, deviceId)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context, baseUrl: String, deviceId: String) {
            val request = PeriodicWorkRequest.Builder(
                SyncWorker::class.java,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS,
            )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setInputData(
                    Data.Builder()
                        .putString(KEY_BASE_URL, baseUrl)
                        .putString(KEY_DEVICE_ID, deviceId)
                        .build()
                )
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
            // Sprint 73 (T5): the network-free watch goes wherever this worker goes (the shell's
            // effect, BootReceiver) and is cancelled with it (SupervisionServices.stopAll).
            EnforcementWatchWorker.schedule(context)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
            EnforcementWatchWorker.cancel(context)
        }
    }
}
