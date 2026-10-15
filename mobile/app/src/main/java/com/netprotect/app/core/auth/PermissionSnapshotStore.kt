package com.netprotect.app.core.auth

import android.content.Context
import com.netprotect.app.feature.supervised.permissions.SupervisedPermission

/** Sprint 67 (lámina 13.4): which permissions were granted the last time the shell read them, for the linked
 * device they were read under. It has to outlive the process: removing a permission in Settings (location, for
 * one) can make Android kill the app, which then starts again with no memory of what it had. Pinned to the
 * device_id like [WelcomeStore], so another link never compares against this one; the shell clears it when the
 * link ends. It keeps only permission names and the device_id [LinkedDeviceStore] already keeps.
 */
object PermissionSnapshotStore {
    private const val PREFS_NAME = "netprotect_permission_snapshot"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_GRANTED = "granted"

    /** The permissions granted at the last read for [deviceId], or null if there is no such read yet. */
    fun read(context: Context, deviceId: String): Set<SupervisedPermission>? {
        val prefs = prefs(context)
        if (prefs.getString(KEY_DEVICE_ID, null) != deviceId) return null
        val names = prefs.getStringSet(KEY_GRANTED, null) ?: return null
        return SupervisedPermission.entries.filter { it.name in names }.toSet()
    }

    fun write(context: Context, deviceId: String, granted: Set<SupervisedPermission>) {
        prefs(context).edit()
            .putString(KEY_DEVICE_ID, deviceId)
            .putStringSet(KEY_GRANTED, granted.map { it.name }.toSet())
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
