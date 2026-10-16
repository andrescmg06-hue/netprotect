package com.netprotect.app.core.rules

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.time.LocalDate
import java.time.ZoneId

/** Polls UsageStatsManager.queryEvents() for MOVE_TO_FOREGROUND events — the only way to learn
 * what app another process brought to the foreground without device owner privileges.
 * ActivityManager#getRunningTasks()/getRunningAppProcesses() only return this app's own process
 * since Android 5.0, and there is no push API for "an app just came to the foreground": see
 * docs/android/capability-matrix.md (Sprint 8) for the verification behind this choice.
 */
class ForegroundAppDetector(private val context: Context) {
    // Sprint 73 (T5): reads overlap by EventWindow.OVERLAP_MS and never hand an event over twice.
    private val window = EventWindow(System.currentTimeMillis())

    /** Sprint 73: every foreground transition (RESUMED/PAUSED) since the last call, oldest
     * first — the whole sequence, not just the last package that came to the front, so a quick
     * "blocked app -> home -> blocked app" inside one poll window is still seen (VisitTracker).
     * Empty when nothing happened. Always advances the high-water mark, so a transient null from
     * queryEvents() (the device isn't "unlocked" yet — see UserManager#isUserUnlocked(), most
     * commonly right after boot) doesn't replay the same window forever.
     */
    fun pollTransitions(): List<ForegroundTransition> = read(window.nextStart())

    /** Sprint 73 (T5): the transitions of the last [lookbackMs], to learn which app is already in
     * front when the poll loop (re)starts (VisitTracker.seed). Later polls continue from here. */
    fun readBack(lookbackMs: Long): List<ForegroundTransition> = read(System.currentTimeMillis() - lookbackMs)

    private fun read(from: Long): List<ForegroundTransition> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(from, now)
            ?: return window.accept(emptyList(), now)

        val transitions = mutableListOf<TimedTransition>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION") // ACTIVITY_RESUMED/PAUSED (their API 29+ replacements) aren't
            // available down to this project's minSdk 26; MOVE_TO_FOREGROUND/BACKGROUND carry the
            // same values and still work identically, just flagged deprecated.
            val kind = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> ForegroundTransition.Kind.RESUMED
                UsageEvents.Event.MOVE_TO_BACKGROUND -> ForegroundTransition.Kind.PAUSED
                else -> null
            }
            val packageName = event.packageName
            if (kind != null && packageName != null) {
                transitions += TimedTransition(event.timeStamp, ForegroundTransition(packageName, kind))
            }
        }
        return window.accept(transitions, now)
    }

    /** Today's foreground seconds of [packageName] rebuilt from usage events (local midnight to
     * now), counting a session that is still open — which queryUsageStats may leave out until
     * the app pauses (B-15). Used only to enforce limits (LimitRecheck.missingSeconds); what is
     * synced to the tutor still comes from AppInventoryCollector alone. Null when the events are
     * unavailable (user not unlocked yet).
     */
    fun todayForegroundSeconds(packageName: String): Int? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val windowStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(windowStart, now) ?: return null

        val samples = mutableListOf<LimitRecheck.UsageEventSample>()
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION") // Same constants as ACTIVITY_RESUMED/PAUSED (API 29+), see above.
            val kind = when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    if (event.packageName == packageName) {
                        LimitRecheck.UsageEventKind.RESUMED
                    } else {
                        LimitRecheck.UsageEventKind.OTHER_RESUMED
                    }
                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    if (event.packageName == packageName) LimitRecheck.UsageEventKind.PAUSED else null
                else -> null
            }
            if (kind != null) samples += LimitRecheck.UsageEventSample(event.timeStamp, kind)
        }
        return (LimitRecheck.foregroundMillisFromEvents(samples, windowStart, now) / 1_000L).toInt()
    }
}
