package com.netprotect.app.core.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** SYSTEM_ALERT_WINDOW ("mostrar sobre otras apps") is a special-access permission, same shape
 * as PACKAGE_USAGE_STATS: declaring it in the manifest isn't enough, there's no runtime dialog,
 * and the settings screen doesn't reliably return a result across OEMs — Settings.canDrawOverlays
 * is the only trustworthy way to check it, same "verify again" pattern as UsageAccessPermission.
 *
 * This is what lets RuleEnforcementService draw the block screen as a window overlay instead of
 * starting an Activity — see BlockOverlayController's docstring for why that distinction matters
 * on Android 10+ (verified live, 17/09/2026: a plain startActivity() from this service is
 * rejected by ActivityTaskManager as a background activity launch).
 */
object OverlayPermission {
    fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Verified live (17/09/2026): FLAG_ACTIVITY_NEW_TASK here silently breaks the
     * ActivityResultLauncher callback in SupervisedScreen — the settings screen still opens and
     * the permission still gets granted, but the card never refreshes on its own afterwards
     * (only the manual "verificar de nuevo" fallback catches it). Launched through a result
     * launcher tied to the current Activity, so no new task is needed here in the first place.
     */
    fun requestIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
}
