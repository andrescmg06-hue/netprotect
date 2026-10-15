package com.netprotect.app.core.boot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.netprotect.app.core.supervision.SupervisionServices

/** Sprint 73: restores the protection after a reboot (BOOT_COMPLETED) or an app update
 * (MY_PACKAGE_REPLACED — an update kills the process and nothing would start it again), without
 * waiting for the supervised person to open NetProtect.
 *
 * Both broadcasts are exemptions from Android's background foreground-service start restriction.
 * Since Android 15 a BOOT_COMPLETED receiver still may NOT start these types: dataSync, camera,
 * mediaPlayback, phoneCall, mediaProjection and microphone (ForegroundServiceStartNotAllowed-
 * Exception) — RuleEnforcementService is specialUse, which is allowed. The location service is a
 * different matter: location is a while-in-use permission and BOOT_COMPLETED is not one of its
 * exemptions, so SupervisionServices only tries it where it can work (API < 30). See
 * docs/android/capability-matrix.md (Sprint 73).
 *
 * BOOT_COMPLETED arrives only after the user unlocks the device, so the credential-encrypted
 * stores (link, session) are readable here. Nothing is started without a stored link and session:
 * after sign-out or unlink this receiver does nothing. onReceive only reads local stores and asks
 * the system to start services or schedule work — no network on the main thread.
 *
 * MIUI/HyperOS does not deliver BOOT_COMPLETED to apps without «Inicio automático».
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED ->
                SupervisionServices.restoreInBackground(context.applicationContext)
        }
    }
}
