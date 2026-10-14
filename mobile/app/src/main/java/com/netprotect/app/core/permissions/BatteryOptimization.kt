package com.netprotect.app.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

/** Sprint 67 (lámina 13.3): whether Android's battery optimization applies to NetProtect. Not a permission:
 * nothing depends on it, it is only checked and the person is sent to Settings to change it themselves.
 *
 * Deliberately without REQUEST_IGNORE_BATTERY_OPTIMIZATIONS / ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
 * (Play policy risk): [settingsIntent] opens the general list, which needs no permission. On some phones
 * the manufacturer's own battery setting does not match this standard whitelist, so the check can be
 * wrong there (to be validated on a real Galaxy).
 */
object BatteryOptimization {
    fun isIgnoring(context: Context): Boolean {
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Android's list of apps and their battery optimization. */
    fun settingsIntent(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** NetProtect's own page in Settings, for a phone where [settingsIntent] has no screen. */
    fun appDetailsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
}
