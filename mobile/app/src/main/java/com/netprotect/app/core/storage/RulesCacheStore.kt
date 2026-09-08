package com.netprotect.app.core.storage

import androidx.room.withTransaction
import com.netprotect.app.core.network.ActiveRules
import com.netprotect.app.core.rules.AppRule
import com.netprotect.app.core.rules.Category
import com.netprotect.app.core.rules.CategoryAssignment
import com.netprotect.app.core.rules.CategoryRule
import com.netprotect.app.core.rules.DefaultAppPolicy
import com.netprotect.app.core.rules.RuleType
import com.netprotect.app.core.rules.SchoolMode

/** The mapping layer between the network shape (`ActiveRules`, `core.rules.*`) and the Room
 * entities in this package, plus the "replace wholesale" transaction — see Entities.kt's top
 * comment for why a full replace, not a merge, is this project's whole conflict strategy.
 */
class RulesCacheStore(private val database: NetProtectDatabase) {

    private val dao get() = database.rulesCacheDao()

    suspend fun replaceAll(deviceId: String, active: ActiveRules) {
        database.withTransaction {
            dao.clearAppRules(deviceId)
            dao.insertAppRules(active.rules.map { it.toEntity(deviceId) })

            dao.clearCategoryAssignments(deviceId)
            dao.insertCategoryAssignments(active.categoryAssignments.map { it.toEntity(deviceId) })

            dao.clearCategoryRules(deviceId)
            dao.insertCategoryRules(active.categoryRules.map { it.toEntity(deviceId) })

            dao.clearDevicePolicy(deviceId)
            dao.insertDevicePolicy(
                CachedDevicePolicyEntity(
                    deviceId = deviceId,
                    defaultPolicy = active.defaultPolicy.wireValue,
                    schoolModeEnabled = active.schoolMode.enabled,
                    schoolModeStartMinute = active.schoolMode.startMinute,
                    schoolModeEndMinute = active.schoolMode.endMinute,
                    schoolModeDaysMask = active.schoolMode.daysMask,
                    cachedAtEpochMs = System.currentTimeMillis(),
                )
            )
        }
    }

    /** Null only when nothing has ever been cached for this device (fresh install, first launch
     * offline) — callers must tell that apart from "cached, and empty", which is a legitimate
     * state (a device with no rules at all yet).
     */
    suspend fun loadCached(deviceId: String): ActiveRules? {
        val policyEntity = dao.getDevicePolicy(deviceId) ?: return null

        val policy = DefaultAppPolicy.fromWire(policyEntity.defaultPolicy) ?: DefaultAppPolicy.ALLOW
        val schoolMode = SchoolMode(
            enabled = policyEntity.schoolModeEnabled,
            startMinute = policyEntity.schoolModeStartMinute,
            endMinute = policyEntity.schoolModeEndMinute,
            daysMask = policyEntity.schoolModeDaysMask,
        )

        return ActiveRules(
            rules = dao.getAppRules(deviceId).mapNotNull { it.toDomainOrNull() },
            categoryAssignments = dao.getCategoryAssignments(deviceId).mapNotNull { it.toDomainOrNull() },
            categoryRules = dao.getCategoryRules(deviceId).mapNotNull { it.toDomainOrNull() },
            defaultPolicy = policy,
            schoolMode = schoolMode,
        )
    }
}

private fun AppRule.toEntity(deviceId: String) = CachedAppRuleEntity(
    deviceId = deviceId,
    packageName = packageName,
    ruleType = ruleType.wireValue,
    dailyLimitMinutes = dailyLimitMinutes,
    weeklyLimitMinutes = weeklyLimitMinutes,
    scheduleStartMinute = scheduleStartMinute,
    scheduleEndMinute = scheduleEndMinute,
    scheduleDaysMask = scheduleDaysMask,
)

private fun CachedAppRuleEntity.toDomainOrNull(): AppRule? {
    val type = RuleType.fromWire(ruleType) ?: return null
    return AppRule(
        packageName = packageName,
        ruleType = type,
        dailyLimitMinutes = dailyLimitMinutes,
        weeklyLimitMinutes = weeklyLimitMinutes,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndMinute = scheduleEndMinute,
        scheduleDaysMask = scheduleDaysMask,
    )
}

private fun CategoryAssignment.toEntity(deviceId: String) = CachedCategoryAssignmentEntity(
    deviceId = deviceId,
    packageName = packageName,
    category = category.wireValue,
)

private fun CachedCategoryAssignmentEntity.toDomainOrNull(): CategoryAssignment? {
    val parsedCategory = Category.fromWire(category) ?: return null
    return CategoryAssignment(packageName = packageName, category = parsedCategory)
}

private fun CategoryRule.toEntity(deviceId: String) = CachedCategoryRuleEntity(
    deviceId = deviceId,
    category = category.wireValue,
    ruleType = ruleType.wireValue,
    dailyLimitMinutes = dailyLimitMinutes,
    weeklyLimitMinutes = weeklyLimitMinutes,
    scheduleStartMinute = scheduleStartMinute,
    scheduleEndMinute = scheduleEndMinute,
    scheduleDaysMask = scheduleDaysMask,
)

private fun CachedCategoryRuleEntity.toDomainOrNull(): CategoryRule? {
    val parsedCategory = Category.fromWire(category) ?: return null
    val type = RuleType.fromWire(ruleType) ?: return null
    return CategoryRule(
        category = parsedCategory,
        ruleType = type,
        dailyLimitMinutes = dailyLimitMinutes,
        weeklyLimitMinutes = weeklyLimitMinutes,
        scheduleStartMinute = scheduleStartMinute,
        scheduleEndMinute = scheduleEndMinute,
        scheduleDaysMask = scheduleDaysMask,
    )
}
