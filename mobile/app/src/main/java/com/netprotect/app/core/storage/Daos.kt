package com.netprotect.app.core.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RulesCacheDao {
    @Query("DELETE FROM cached_app_rules WHERE deviceId = :deviceId")
    suspend fun clearAppRules(deviceId: String)

    @Insert
    suspend fun insertAppRules(rules: List<CachedAppRuleEntity>)

    @Query("SELECT * FROM cached_app_rules WHERE deviceId = :deviceId")
    suspend fun getAppRules(deviceId: String): List<CachedAppRuleEntity>

    @Query("DELETE FROM cached_category_assignments WHERE deviceId = :deviceId")
    suspend fun clearCategoryAssignments(deviceId: String)

    @Insert
    suspend fun insertCategoryAssignments(assignments: List<CachedCategoryAssignmentEntity>)

    @Query("SELECT * FROM cached_category_assignments WHERE deviceId = :deviceId")
    suspend fun getCategoryAssignments(deviceId: String): List<CachedCategoryAssignmentEntity>

    @Query("DELETE FROM cached_category_rules WHERE deviceId = :deviceId")
    suspend fun clearCategoryRules(deviceId: String)

    @Insert
    suspend fun insertCategoryRules(rules: List<CachedCategoryRuleEntity>)

    @Query("SELECT * FROM cached_category_rules WHERE deviceId = :deviceId")
    suspend fun getCategoryRules(deviceId: String): List<CachedCategoryRuleEntity>

    @Query("DELETE FROM cached_device_policies WHERE deviceId = :deviceId")
    suspend fun clearDevicePolicy(deviceId: String)

    @Insert
    suspend fun insertDevicePolicy(policy: CachedDevicePolicyEntity)

    @Query("SELECT * FROM cached_device_policies WHERE deviceId = :deviceId")
    suspend fun getDevicePolicy(deviceId: String): CachedDevicePolicyEntity?
}

@Dao
interface PendingRuleEventDao {
    @Insert
    suspend fun insert(event: PendingRuleEventEntity)

    @Query("SELECT * FROM pending_rule_events WHERE deviceId = :deviceId ORDER BY id ASC")
    suspend fun getAll(deviceId: String): List<PendingRuleEventEntity>

    @Delete
    suspend fun delete(event: PendingRuleEventEntity)
}
