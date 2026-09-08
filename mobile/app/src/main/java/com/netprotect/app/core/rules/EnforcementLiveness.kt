package com.netprotect.app.core.rules

import android.content.Context

/** Sprint 20: how anything outside RuleEnforcementService's own process can tell whether that
 * service is still running. The service stamps [markActive] on every poll cycle (~3s); everything
 * that sends a heartbeat reads [isRecentlyActive] and reports it as `service_active`, which the
 * backend turns into a SERVICE_INACTIVE tamper alert (app/services/tamper.py).
 *
 * Why a stored timestamp instead of asking Android: since Android 5.0 there is no supported way
 * for an app to ask the system whether one of its own services is alive from another process
 * (`ActivityManager.getRunningServices()` is deprecated and returns only this app's own process's
 * view). A cooperative marker is honest about what it is: it detects the ordinary cases this
 * sprint targets — the user swipes the app away, force-stops it, or the system kills it — and
 * makes no claim to survive a determined technical adversary, exactly like the rest of this
 * project's manipulation detection (see docs/sprint-20.md).
 *
 * Returns null, never false, when nothing has ever been stamped: a fresh install that has not yet
 * started enforcing is "no information", not "the service was disabled". The heartbeat omits the
 * field entirely in that case rather than reporting a tamper condition that didn't happen.
 */
object EnforcementLiveness {
    private const val PREFS_NAME = "netprotect_enforcement_liveness"
    private const val KEY_LAST_ACTIVE_AT = "last_active_at"

    // Comfortably above RuleEnforcementService's 3s poll interval, so a live service is never
    // mistaken for a dead one because of a slow cycle, and well under SyncWorker's 15-minute
    // cadence, so a service that really did stop is caught on the next background beat.
    private const val STALE_AFTER_MS = 2 * 60_000L

    fun markActive(context: Context) {
        prefs(context).edit().putLong(KEY_LAST_ACTIVE_AT, System.currentTimeMillis()).apply()
    }

    fun isRecentlyActive(context: Context): Boolean? {
        val lastActiveAt = prefs(context).getLong(KEY_LAST_ACTIVE_AT, 0L)
        if (lastActiveAt == 0L) return null
        return System.currentTimeMillis() - lastActiveAt < STALE_AFTER_MS
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_LAST_ACTIVE_AT).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
