package com.netprotect.app.core.auth

import android.content.Context

/** Remembers the id of the user whose SUPERVISADO role the backend already confirmed on this
 * install, so the app can enter supervised mode at start without an extra network call — the
 * protection services must not depend on `GET /users/me/roles` answering every time.
 *
 * It is a local shortcut, not a source of authorization: every supervised request is still
 * checked by the backend, which rejects it if the account lacks the role. Keyed by user id so a
 * different account signing in on the same phone goes through the role check again.
 */
object SupervisedRoleCache {
    private const val PREFS_NAME = "netprotect_supervised_role"
    private const val KEY_USER_ID = "confirmed_user_id"

    fun isConfirmedFor(context: Context, userId: String): Boolean =
        prefs(context).getString(KEY_USER_ID, null) == userId

    fun write(context: Context, userId: String) {
        prefs(context).edit().putString(KEY_USER_ID, userId).apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_USER_ID).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
