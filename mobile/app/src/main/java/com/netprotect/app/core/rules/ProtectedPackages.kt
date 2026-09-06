package com.netprotect.app.core.rules

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.telecom.TelecomManager

/** Apps this project refuses to block, in any mode.
 *
 * Allowlist mode (device policy BLOCK) would otherwise block the launcher, the phone app and
 * Settings, which would leave the supervised person with no home screen, no way to place an
 * emergency call, and no way to reach the setting that turns this off. None of that is an
 * acceptable outcome for a parental-control app, so it is enforced in code rather than left to
 * the tutor to remember.
 *
 * Everything is resolved at runtime instead of hardcoding package names, which differ across
 * manufacturers. See docs/android/capability-matrix.md (Sprint 9) for the verification behind
 * each lookup, including the honest limit: any lookup can return null, and an app that fails to
 * resolve is simply not protected.
 */
object ProtectedPackages {

    fun resolve(context: Context): Set<String> = buildSet {
        add(context.packageName)
        launcherPackage(context)?.let(::add)
        settingsPackage(context)?.let(::add)
        addAll(dialerPackages(context))
    }

    private fun launcherPackage(context: Context): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager
            .resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
    }

    private fun settingsPackage(context: Context): String? =
        context.packageManager
            .resolveActivity(Intent(Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName

    /** The user's chosen dialer and the preloaded system one — both are public SDK methods
     * (verified with javap against android.jar of API 36, not assumed), and both can return
     * null. ACTION_DIAL is resolved too as a last resort: this set is what stands between
     * allowlist mode and a phone that can't dial emergency services, so a little redundancy
     * here is worth more than minimalism.
     */
    private fun dialerPackages(context: Context): List<String> {
        val telecom = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
        val dialIntentHandler = context.packageManager
            .resolveActivity(Intent(Intent.ACTION_DIAL), PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo
            ?.packageName
        return listOfNotNull(
            telecom?.defaultDialerPackage,
            telecom?.systemDialerPackage,
            dialIntentHandler,
        )
    }
}
