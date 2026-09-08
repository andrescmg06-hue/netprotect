package com.netprotect.app.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedAppRuleEntity::class,
        CachedCategoryAssignmentEntity::class,
        CachedCategoryRuleEntity::class,
        CachedDevicePolicyEntity::class,
        PendingRuleEventEntity::class,
    ],
    version = 1,
    // No migration history to test against yet (version 1) — exported schema JSON only matters
    // once there's a prior version to validate a migration against.
    exportSchema = false,
)
abstract class NetProtectDatabase : RoomDatabase() {
    abstract fun rulesCacheDao(): RulesCacheDao
    abstract fun pendingRuleEventDao(): PendingRuleEventDao

    companion object {
        @Volatile private var instance: NetProtectDatabase? = null

        fun getInstance(context: Context): NetProtectDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetProtectDatabase::class.java,
                    "netprotect.db",
                ).build().also { instance = it }
            }
    }
}
