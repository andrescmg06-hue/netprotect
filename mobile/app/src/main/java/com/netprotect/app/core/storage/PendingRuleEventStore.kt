package com.netprotect.app.core.storage

import com.netprotect.app.core.network.RuleEnforcementClient
import com.netprotect.app.core.rules.BlockReason
import java.time.Instant

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
     * — later events are almost certainly for the same outage, so there's no point burning a
     * request per row only to fail all of them the same way.
     */
    suspend fun flush(client: RuleEnforcementClient, accessToken: String, deviceId: String) {
        for (event in dao.getAll(deviceId)) {
            val reason = BlockReason.entries.find { it.wireValue == event.ruleTypeApplied } ?: run {
                // Not a reason this build recognizes (an older row from a previous app version,
                // say): drop it rather than retry forever on something that can never succeed.
                dao.delete(event)
                continue
            }
            val reported = runCatching {
                client.reportRuleEvent(
                    accessToken,
                    deviceId,
                    event.packageName,
                    reason,
                    Instant.ofEpochMilli(event.occurredAtEpochMs),
                )
            }.isSuccess
            if (!reported) return
            dao.delete(event)
        }
    }
}
