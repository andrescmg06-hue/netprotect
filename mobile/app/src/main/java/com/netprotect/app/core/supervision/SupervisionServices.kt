package com.netprotect.app.core.supervision

import android.content.Context
import android.util.Log
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.LinkedDeviceStore
import com.netprotect.app.core.auth.TokenProvider
import com.netprotect.app.core.location.LocationReportingService
import com.netprotect.app.core.permissions.LocationPermission
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.rules.RuleEnforcementService
import com.netprotect.app.core.sync.SyncWorker

/** Sprint 73: the protection (RuleEnforcementService, LocationReportingService, SyncWorker) no
 * longer lives and dies with SupervisedShell. Closing the app (Back, or swiping it from Recents)
 * destroys the Activity, and the shell used to stop everything in its effects' onDispose; now the
 * services are stopped only when it is really meant — sign-out, unlink, the session ending, a
 * permission withdrawn — and restarted from what is stored when the system brings them back
 * (START_STICKY), at boot or app update (BootReceiver) and from SyncWorker's watch.
 *
 * Starting from the background is restricted by Android; see docs/android/capability-matrix.md
 * (Sprint 73) for which start works from where. Every background start here is best effort.
 */
object SupervisionServices {
    private const val TAG = "SupervisionServices"

    /** The linked device this phone can restore protection for without the UI: a link stored by
     * the shell AND a stored session (sign-out and an ended session both clear the latter). Null
     * otherwise. Reads SharedPreferences and the Keystore-encrypted refresh token only; no network.
     */
    fun restorableDeviceId(context: Context): String? {
        val deviceId = LinkedDeviceStore.read(context)?.deviceId ?: return null
        val hasSession = runCatching { TokenProvider.get(context).hasStoredSession() }.getOrDefault(false)
        return deviceId.takeIf { hasSession }
    }

    /** Deliberate stop of everything that keeps protecting with the app closed: sign-out, unlink,
     * session ended. RuleEnforcementService.stop also clears EnforcementLiveness, because a
     * deliberate stop is not tampering. Stopping something that is not running is a no-op.
     */
    fun stopAll(context: Context) {
        RuleEnforcementService.stop(context)
        LocationReportingService.stop(context)
        SyncWorker.cancel(context)
    }

    /** From a background context (BootReceiver): schedules SyncWorker and starts whatever the
     * stored link, session and permissions allow. Never throws.
     */
    fun restoreInBackground(context: Context) {
        val deviceId = restorableDeviceId(context) ?: return
        runCatching { SyncWorker.schedule(context, BuildConfig.API_BASE_URL, deviceId) }
            .onFailure { Log.w(TAG, "Sync schedule failed: ${it.javaClass.simpleName}") }
        tryStartEnforcement(context, deviceId)
        // See LocationReportingService.canStartFromBackground: from API 30 a location service
        // started from the background cannot read the location (and from API 34 it is refused),
        // so it is left for the next time the app is opened.
        if (LocationReportingService.canStartFromBackground() && LocationPermission.isGranted(context)) {
            tryStart("location") {
                LocationReportingService.start(context, BuildConfig.API_BASE_URL, "", deviceId)
            }
        }
    }

    /** Starts RuleEnforcementService for [deviceId] if usage access is granted. From the
     * background this only works where Android allows it (BOOT_COMPLETED, battery optimization
     * off for the app, ...); otherwise the start is refused and false is returned. The empty
     * token is fine: the service asks the shared session for a valid one before its first use.
     */
    fun tryStartEnforcement(context: Context, deviceId: String): Boolean {
        if (!UsageAccessPermission.isGranted(context)) return false
        return tryStart("enforcement") {
            RuleEnforcementService.start(context, BuildConfig.API_BASE_URL, "", deviceId)
        }
    }

    /** ForegroundServiceStartNotAllowedException (API 31+) is an IllegalStateException; caught
     * by its supertype so this compiles and runs down to minSdk 26. Logs the exception type only.
     */
    private inline fun tryStart(what: String, start: () -> Unit): Boolean =
        try {
            start()
            true
        } catch (refused: IllegalStateException) {
            Log.w(TAG, "Background start of $what refused: ${refused.javaClass.simpleName}")
            false
        } catch (refused: SecurityException) {
            Log.w(TAG, "Background start of $what refused: ${refused.javaClass.simpleName}")
            false
        }
}
