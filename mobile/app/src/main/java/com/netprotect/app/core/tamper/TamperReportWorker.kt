package com.netprotect.app.core.tamper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.BackgroundTokenRefresher
import com.netprotect.app.core.auth.LinkedDeviceStore
import com.netprotect.app.core.network.DeviceClient
import java.time.Instant

private const val KEY_BASE_URL = "base_url"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_EVENT_TYPE = "event_type"
private const val KEY_OCCURRED_AT = "occurred_at"

const val UNINSTALL_ATTEMPT = "UNINSTALL_ATTEMPT"

/** Delivers a one-shot tamper event to the backend outside the broadcast that detected it.
 *
 * [NetProtectDeviceAdminReceiver.onDisableRequested] runs on the main thread and must return a
 * warning string promptly — a network call there would risk an ANR, and the process may well be
 * gone moments later once the user completes the deactivation. WorkManager is the officially
 * recommended way to hand guaranteed work off from a broadcast receiver: it survives the
 * receiver, retries on its own if the device is offline, and is already a dependency of this
 * project (SyncWorker, Sprint 19).
 *
 * Returns [Result.retry] on a failed send so the attempt isn't lost on a device that happens to
 * be offline at that moment — the same "a real event stays reportable" reasoning as
 * PendingRuleEventStore (Sprint 19), reusing WorkManager's own backoff instead of a second queue.
 */
class TamperReportWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val baseUrl = inputData.getString(KEY_BASE_URL) ?: return Result.failure()
        val deviceId = inputData.getString(KEY_DEVICE_ID) ?: return Result.failure()
        val eventType = inputData.getString(KEY_EVENT_TYPE) ?: return Result.failure()
        val occurredAt = inputData.getString(KEY_OCCURRED_AT) ?: return Result.failure()

        val accessToken = BackgroundTokenRefresher.refresh(applicationContext, baseUrl)
            ?: return Result.retry()

        return runCatching {
            DeviceClient(baseUrl).reportTamperEvent(
                accessToken,
                deviceId,
                eventType,
                Instant.parse(occurredAt),
            )
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }

    companion object {
        /** No-op when this install isn't linked to any tutor: there is no device to report
         * against, and an unlinked install being uninstalled is nobody's alert.
         */
        fun reportUninstallAttempt(context: Context) {
            val deviceId = LinkedDeviceStore.read(context)?.deviceId ?: return
            val request = OneTimeWorkRequest.Builder(TamperReportWorker::class.java)
                .setInputData(
                    Data.Builder()
                        .putString(KEY_BASE_URL, BuildConfig.API_BASE_URL)
                        .putString(KEY_DEVICE_ID, deviceId)
                        .putString(KEY_EVENT_TYPE, UNINSTALL_ATTEMPT)
                        .putString(KEY_OCCURRED_AT, Instant.now().toString())
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
