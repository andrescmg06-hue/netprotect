package com.netprotect.app.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.netprotect.app.core.supervision.SupervisionServices
import java.util.concurrent.TimeUnit

private const val UNIQUE_WORK_NAME = "netprotect_enforcement_watch"

/** Sprint 73 (T5): the watch that brings RuleEnforcementService back if it died, on its own
 * schedule and with NO network constraint. It used to live only inside SyncWorker, which requires
 * a connection (NetworkType.CONNECTED) and returned before the watch when no token could be
 * minted — so a phone offline (or with the backend down) never restarted its blocking, which works
 * offline from the cached rules. Restarting is local: EnforcementLiveness, the stored link and
 * session, usage access, and a start request to the system.
 *
 * Unique periodic work (WorkManager's 15-minute floor) with ExistingPeriodicWorkPolicy.UPDATE,
 * scheduled and cancelled together with SyncWorker (SyncWorker.schedule/cancel), so every place
 * that schedules or cancels the sync (the shell, BootReceiver, SupervisionServices.stopAll) also
 * handles this watch, and there is never more than one.
 */
class EnforcementWatchWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        SupervisionServices.restartEnforcementIfDead(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequest.Builder(
                EnforcementWatchWorker::class.java,
                PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS,
                TimeUnit.MILLISECONDS,
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
        }
    }
}
