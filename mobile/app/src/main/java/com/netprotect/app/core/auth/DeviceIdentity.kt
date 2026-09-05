package com.netprotect.app.core.auth

import android.content.Context
import java.util.UUID

/** A random id generated once per install and reused for every future pairing.
 *
 * Not sensitive (it identifies a phone, not a person) and not tied to hardware, so plain
 * SharedPreferences is enough — no need for the Keystore-backed encryption TokenStore uses.
 */
object DeviceIdentity {
    private const val PREFS_NAME = "netprotect_device_identity"
    private const val KEY_INSTANCE_ID = "device_instance_id"

    fun getOrCreate(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.getString(KEY_INSTANCE_ID, null)?.let { return it }

        val generated = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_INSTANCE_ID, generated).apply()
        return generated
    }
}
