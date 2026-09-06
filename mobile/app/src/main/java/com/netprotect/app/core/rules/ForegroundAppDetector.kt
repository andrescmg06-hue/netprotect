package com.netprotect.app.core.rules

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/** Polls UsageStatsManager.queryEvents() for MOVE_TO_FOREGROUND events — the only way to learn
 * what app another process brought to the foreground without device owner privileges.
 * ActivityManager#getRunningTasks()/getRunningAppProcesses() only return this app's own process
 * since Android 5.0, and there is no push API for "an app just came to the foreground": see
 * docs/android/capability-matrix.md (Sprint 8) for the verification behind this choice.
 */
class ForegroundAppDetector(private val context: Context) {
    private var lastQueriedAt = System.currentTimeMillis()

    /** Returns the package that most recently moved to the foreground since the last call, or
     * null if nothing did. Always advances the high-water mark, so a transient null from
     * queryEvents() (the device isn't "unlocked" yet — see UserManager#isUserUnlocked(), most
     * commonly right after boot) doesn't replay the same window forever.
     */
    fun pollForegroundChange(): String? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(lastQueriedAt, now)
        lastQueriedAt = now
        if (events == null) return null

        var latestForegroundPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION") // ACTIVITY_RESUMED (its API 29+ replacement) isn't
            // available down to this project's minSdk 26; MOVE_TO_FOREGROUND still works
            // identically for this app-level (not per-Activity) use, just flagged deprecated.
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                latestForegroundPackage = event.packageName
            }
        }
        return latestForegroundPackage
    }
}
