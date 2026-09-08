package com.netprotect.app.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Sprint 19: the local, read-through cache of what `RuleEnforcementClient.getActiveRules()`
 * last returned for this device, so enforcement can start from the last known-good rule set
 * instead of a hardcoded "allow everything" fallback when the very first fetch after a cold
 * start has no connectivity — see RuleEnforcementService's docstring for the offline strategy.
 *
 * Every table here is replaced wholesale on each successful fetch (RulesCacheDao.replaceAll), not
 * merged row by row: the backend is this project's only source of truth (see CLAUDE.md), the
 * device never edits a rule locally, so there is nothing to reconcile — "conflict resolution" is
 * simply "the newest successful fetch wins, unconditionally".
 */
@Entity(tableName = "cached_app_rules", primaryKeys = ["deviceId", "packageName"])
data class CachedAppRuleEntity(
    val deviceId: String,
    val packageName: String,
    val ruleType: String,
    val dailyLimitMinutes: Int?,
    val weeklyLimitMinutes: Int?,
    val scheduleStartMinute: Int?,
    val scheduleEndMinute: Int?,
    val scheduleDaysMask: Int?,
)

@Entity(tableName = "cached_category_assignments", primaryKeys = ["deviceId", "packageName"])
data class CachedCategoryAssignmentEntity(
    val deviceId: String,
    val packageName: String,
    val category: String,
)

@Entity(tableName = "cached_category_rules", primaryKeys = ["deviceId", "category"])
data class CachedCategoryRuleEntity(
    val deviceId: String,
    val category: String,
    val ruleType: String,
    val dailyLimitMinutes: Int?,
    val weeklyLimitMinutes: Int?,
    val scheduleStartMinute: Int?,
    val scheduleEndMinute: Int?,
    val scheduleDaysMask: Int?,
)

/** One row per device: the default policy and school-mode window, same shape as the backend's
 * per-device columns (not a separate table there either — see app/models/device.py).
 */
@Entity(tableName = "cached_device_policies")
data class CachedDevicePolicyEntity(
    @PrimaryKey val deviceId: String,
    val defaultPolicy: String,
    val schoolModeEnabled: Boolean,
    val schoolModeStartMinute: Int?,
    val schoolModeEndMinute: Int?,
    val schoolModeDaysMask: Int?,
    // When this row was written (epoch millis) — a staleness marker for logs/debugging only,
    // not a version number a conflict-resolution rule reads: see this file's top comment.
    val cachedAtEpochMs: Long,
)

/** A rule enforcement the device already applied and tried to report
 * (`RuleEnforcementClient.reportRuleEvent`), but couldn't — no connectivity, or the backend was
 * unreachable — so it's held here until a later attempt succeeds. Never dropped: a block that
 * happened is real regardless of whether the tutor gets to see it right away.
 *
 * No idempotency key: a retry that actually reached the server on a prior attempt, but whose
 * response never reached this device (a torn TCP connection right after the 200 arrives is the
 * realistic case), can be reported twice. Accepted the same way this project accepts
 * `RuleEnforcementService`'s "no restart after the process dies" limitation (Sprint 8) — the
 * failure mode is a rare, harmless duplicate row in `app_rule_events`/one alert `occurrence_count`
 * bumped an extra time, not lost data or incorrect enforcement.
 */
@Entity(tableName = "pending_rule_events")
data class PendingRuleEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val deviceId: String,
    val packageName: String,
    val ruleTypeApplied: String,
    val occurredAtEpochMs: Long,
)
