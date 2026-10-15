package com.netprotect.app.core.inventory

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import java.time.DayOfWeek
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
 * rest of this project's "no scheduler yet, foreground-only" approach (see SupervisedShell).
 */
object AppInventoryCollector {

    // Same limit as app_label in backend/app/schemas/application.py.
    private const val MAX_LABEL_LENGTH = 255

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
                    appLabel = displayLabel(
                        packageManager.getApplicationLabel(appInfo),
                        appInfo.packageName,
                    ),
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                )
            }
    }

    /** The backend takes an app_label of 1 to 255 characters and rejects the whole sync for a
     * single bad entry. A real OPPO (Android 16) has a launcher app whose label is an empty
     * string, which kept every app's usage from reaching the tutor; the package name is the one
     * value that always identifies it.
     */
    internal fun displayLabel(rawLabel: CharSequence?, packageName: String): String =
        rawLabel?.toString()?.trim().orEmpty().ifEmpty { packageName }.take(MAX_LABEL_LENGTH)

    /** The device's current local calendar day, as the ISO date string the backend expects. */
    fun todayDateString(): String = LocalDate.now().toString()

    fun collectTodayUsage(context: Context): List<AppUsageInfo> =
        collectUsageSince(context, LocalDate.now())

    /** Same mechanism as [collectTodayUsage], just a wider window: Monday 00:00 local time
     * through now, matching schedule_days_mask's bit 0 = Monday convention (Sprint 11) rather
     * than a rolling 7-day window whose meaning would shift depending on when it's checked.
     */
    fun collectWeekUsage(context: Context): List<AppUsageInfo> {
        val today = LocalDate.now()
        val monday = today.minusDays((today.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
        return collectUsageSince(context, monday)
    }

    private fun collectUsageSince(context: Context, sinceDate: LocalDate): List<AppUsageInfo> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val startMillis = sinceDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, startMillis, now
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
