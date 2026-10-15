package com.netprotect.app.core.storage

import android.util.Log
import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.QueuedReportFailure
import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.network.classifyQueuedReportFailure
import com.netprotect.app.core.rules.BlockReason
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** The queue behind "las reglas siguen aplicándose sin Internet" (Sprint 19): a block the device
 * already enforced but couldn't report gets held here instead of lost, and flushed opportunistically
 * — both from RuleEnforcementService's own poll loop and from SyncWorker, whichever gets
 * connectivity back first. See PendingRuleEventEntity's docstring for the accepted at-least-once
 * delivery limitation.
 */
class PendingRuleEventStore(private val database: NetProtectDatabase) {

    private val dao get() = database.pendingRuleEventDao()

    suspend fun enqueue(deviceId: String, packageName: String, reason: BlockReason, occurredAt: Instant) {
        dao.insert(
            PendingRuleEventEntity(
                deviceId = deviceId,
                packageName = packageName,
                ruleTypeApplied = reason.wireValue,
                occurredAtEpochMs = occurredAt.toEpochMilli(),
            )
        )
    }

    /** Attempts every queued event for this device, oldest first, stopping at the first failure
     * that can succeed later — later events are almost certainly for the same outage, so there's
     * no point burning a request per row only to fail all of them the same way.
     *
     * Sprint 73 (T5): a failure that can never succeed (a 4xx such as 422, see
     * classifyQueuedReportFailure) no longer stops the queue forever: that event is dropped, logged
     * by status only, and the next one is tried. The uniform `device_not_found` 404 (the link
     * ended) is rethrown for the caller to end the link (SupervisionServices.endLinkFromBackground).
     */
    suspend fun flush(client: RuleEnforcementClient, accessToken: String, deviceId: String) {
        for (event in dao.getAll(deviceId)) {
            val reason = BlockReason.entries.find { it.wireValue == event.ruleTypeApplied } ?: run {
                // Not a reason this build recognizes (an older row from a previous app version,
                // say): drop it rather than retry forever on something that can never succeed.
                dao.delete(event)
                continue
            }
            val failure = runCatching {
                client.reportRuleEvent(
                    accessToken,
                    deviceId,
                    event.packageName,
                    reason,
                    Instant.ofEpochMilli(event.occurredAtEpochMs),
                )
            }.exceptionOrNull()
            if (failure is CancellationException) throw failure
            when (failure?.let(::classifyQueuedReportFailure)) {
                null -> dao.delete(event)
                QueuedReportFailure.DROP -> {
                    Log.w(TAG, "Dropped a queued rule event the server rejected: HTTP ${(failure as ApiException).statusCode}")
                    dao.delete(event)
                }
                QueuedReportFailure.DEVICE_GONE -> throw failure
                QueuedReportFailure.RETRY_LATER -> return
            }
        }
    }

    private companion object {
        const val TAG = "PendingRuleEvents"
    }
}
