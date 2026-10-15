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
    private const val KEY_ENDED_BY_SERVER = "ended_by_server"

    data class Linked(val deviceId: String, val tutorLabel: String)

    fun read(context: Context): Linked? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val tutorLabel = prefs.getString(KEY_TUTOR_LABEL, null) ?: return null
        return Linked(deviceId, tutorLabel)
    }

    fun write(context: Context, deviceId: String, tutorLabel: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putString(KEY_TUTOR_LABEL, tutorLabel)
            .remove(KEY_ENDED_BY_SERVER)
            .apply()
    }

    /** Sprint 73 (T5): the background work learnt from the server that the link ended (the
     * uniform `device_not_found`). Forgets the link like [clear] — so no restart revives the
     * protection — but remembers that it ended, so the next app open shows "unlinked" (as
     * SupervisedShell.enterUnlinked would) instead of the code entry of a phone never linked. */
    fun markEndedByServer(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().putBoolean(KEY_ENDED_BY_SERVER, true).apply()
    }

    fun endedByServer(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENDED_BY_SERVER, false)

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
