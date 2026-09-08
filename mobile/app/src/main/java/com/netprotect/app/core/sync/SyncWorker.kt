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
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.storage.NetProtectDatabase
import com.netprotect.app.core.storage.PendingRuleEventStore
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

        // No stored session (signed out) or genuinely offline: nothing to do, and not a
        // worker "failure" either way — WorkManager's exponential backoff on Result.retry()
        // is for transient errors, not for "there was nothing to sync this cycle".
        val accessToken = BackgroundTokenRefresher.refresh(applicationContext, baseUrl)
            ?: return Result.success()

        runCatching {
            DeviceClient(baseUrl).sendHeartbeat(
                accessToken,
                deviceId,
                Build.VERSION.RELEASE,
                BuildConfig.VERSION_NAME,
                TimeZone.getDefault().id,
            )
        }

        if (UsageAccessPermission.isGranted(applicationContext)) {
            runCatching {
                ApplicationsClient(baseUrl).syncApplications(
                    accessToken = accessToken,
                    deviceId = deviceId,
                    usageDate = AppInventoryCollector.todayDateString(),
                    installedApps = AppInventoryCollector.collectInstalledApps(applicationContext),
                    dailyUsage = AppInventoryCollector.collectTodayUsage(applicationContext),
                )
            }
        }

        val database = NetProtectDatabase.getInstance(applicationContext)
        runCatching {
            PendingRuleEventStore(database).flush(RuleEnforcementClient(baseUrl), accessToken, deviceId)
        }

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
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
