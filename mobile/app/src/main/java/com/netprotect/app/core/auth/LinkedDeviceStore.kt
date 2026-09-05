package com.netprotect.app.core.auth

import android.content.Context

/** Remembers this phone's own device_id once it has redeemed a pairing code, so the app
 * doesn't ask for a code again on every launch. Not sensitive (a device_id is meaningless
 * without a valid session), so plain SharedPreferences is enough.
 */
object LinkedDeviceStore {
    private const val PREFS_NAME = "netprotect_linked_device"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_TUTOR_LABEL = "tutor_label"

    data class Linked(val deviceId: String, val tutorLabel: String)

    fun read(context: Context): Linked? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val tutorLabel = prefs.getString(KEY_TUTOR_LABEL, null) ?: return null
        return Linked(deviceId, tutorLabel)
    }

    fun write(context: Context, deviceId: String, tutorLabel: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).putString(KEY_TUTOR_LABEL, tutorLabel).apply()
    }

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
