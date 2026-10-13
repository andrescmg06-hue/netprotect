package com.netprotect.app.core.auth

import android.content.Context

/** Sprint 67: remembers for which linked device the welcome (láminas 13.1 and 13.2) was already shown.
 * It keeps that device_id, not a global flag, so a different link shows the welcome again; the shell
 * clears it when the link ends. It holds nothing beyond the device_id [LinkedDeviceStore] already keeps.
 */
object WelcomeStore {
    private const val PREFS_NAME = "netprotect_welcome"
    private const val KEY_SEEN_DEVICE_ID = "seen_for_device_id"

    /** The device the welcome was last finished for, or null if never (or since the last unlink). */
    fun seenDeviceId(context: Context): String? = prefs(context).getString(KEY_SEEN_DEVICE_ID, null)

    fun isSeen(context: Context, deviceId: String): Boolean = seenDeviceId(context) == deviceId

    fun markSeen(context: Context, deviceId: String) {
        prefs(context).edit().putString(KEY_SEEN_DEVICE_ID, deviceId).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_SEEN_DEVICE_ID).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
