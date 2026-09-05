package com.netprotect.app.core.auth

import android.content.Context

/** Remembers which mode (TUTOR or SUPERVISADO) this install last chose, purely so the app
 * doesn't ask again on every launch. The backend is the one that actually grants the role
 * (see RoleClient) — this is just a local UI shortcut, not a source of authorization.
 */
object RolePreference {
    private const val PREFS_NAME = "netprotect_role_preference"
    private const val KEY_ROLE_CODE = "role_code"

    fun read(context: Context): String? {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE_CODE, null)
    }

    fun write(context: Context, roleCode: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ROLE_CODE, roleCode).apply()
    }

    fun clear(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_ROLE_CODE).apply()
    }
}
