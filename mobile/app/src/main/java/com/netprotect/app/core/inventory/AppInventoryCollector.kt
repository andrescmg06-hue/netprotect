package com.netprotect.app.core.inventory

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.time.LocalDate
import java.time.ZoneId

data class InstalledAppInfo(
    val packageName: String,
    val appLabel: String,
    val isSystemApp: Boolean,
)

data class AppUsageInfo(
    val packageName: String,
    val foregroundSeconds: Int,
)

/** Reads what's installed and how long each app has run in the foreground today. Both are
 * read fresh on every call — there is no caching or background scheduling here, matching the
 * rest of this project's "no scheduler yet, foreground-only" approach (see SupervisedScreen).
 */
object AppInventoryCollector {

    /** Only apps with a launcher entry: a parent recognizes "Instagram" or "Cámara", not the
     * couple hundred background system packages (telephony providers, system UI, etc.) that
     * QUERY_ALL_PACKAGES also makes visible but that mean nothing to them.
     */
    fun collectInstalledApps(context: Context): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchablePackages = packageManager.queryIntentActivities(launcherIntent, 0)
            .mapNotNull { it.activityInfo?.packageName }
            .toSet()

        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.packageName in launchablePackages }
            .map { appInfo ->
                InstalledAppInfo(
                    packageName = appInfo.packageName,
                    appLabel = packageManager.getApplicationLabel(appInfo).toString(),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
    }

    /** The device's current local calendar day, as the ISO date string the backend expects. */
    fun todayDateString(): String = LocalDate.now().toString()

    fun collectTodayUsage(context: Context): List<AppUsageInfo> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault())
            .toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, startOfToday, now
        ) ?: return emptyList()

        // queryUsageStats can return more than one bucket per package within a single range;
        // sum them rather than assuming one row per app.
        return stats
            .groupBy { it.packageName }
            .mapNotNull { (packageName, entries) ->
                val totalMillis = entries.sumOf { it.totalTimeInForeground }
                if (totalMillis <= 0) null else AppUsageInfo(packageName, (totalMillis / 1000).toInt())
            }
    }
}
