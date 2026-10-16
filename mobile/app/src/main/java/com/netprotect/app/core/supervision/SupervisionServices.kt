package com.netprotect.app.core.supervision

import android.content.Context
import android.util.Log
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.LinkedDeviceStore
import com.netprotect.app.core.auth.TokenProvider
import com.netprotect.app.core.location.LocationReportingService
import com.netprotect.app.core.permissions.LocationPermission
import com.netprotect.app.core.permissions.UsageAccessPermission
import com.netprotect.app.core.rules.EnforcementLiveness
import com.netprotect.app.core.rules.RuleEnforcementService
import com.netprotect.app.core.storage.NetProtectDatabase
import com.netprotect.app.core.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

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
     * session ended (callers that also forget the link use [forgetDevice] / [endLinkFromBackground]). RuleEnforcementService.stop also clears EnforcementLiveness, because a
     * deliberate stop is not tampering. Stopping something that is not running is a no-op.
     */
    fun stopAll(context: Context) {
        RuleEnforcementService.stop(context)
        LocationReportingService.stop(context)
        SyncWorker.cancel(context)
    }

    /** Sprint 73 (T5): sign-out, an ended session, or another account taking over this phone.
     * Stops everything, forgets the link (so no restart — sticky, boot, watch — can run the old
     * device's protection under someone else's session) and deletes this phone's copy of the
     * device's data: the rule cache and the queue of block events not yet reported. Deleting the
     * queue is deliberate: those events can only be sent with this device's own session (any
     * other account gets the uniform 404), and a minor's activity should not stay on a phone
     * nobody supervising it is signed into. The cost: if the same person signs back in, blocks
     * that happened offline before the sign-out never reach the tutor.
     */
    suspend fun forgetDevice(context: Context) {
        stopAll(context)
        LinkedDeviceStore.clear(context)
        clearDeviceData(context)
    }

    /** Sprint 73 (T5): the backend answered `device_not_found` to the background work of
     * [deviceId] — the tutor unlinked it (or it is not this account's device), possibly with the
     * app closed, where the shell's own check never runs. Same outcome as
     * SupervisedShell.enterUnlinked, without the UI: forget the link, delete its local data, stop
     * everything. Ignored if the stored link is already a different device (a stale worker or loop
     * from before a new pairing must not end the new link). Safe to call from the loop of a
     * service it stops: everything that must happen runs before the stop cancels that loop.
     */
    suspend fun endLinkFromBackground(context: Context, deviceId: String) {
        val linked = LinkedDeviceStore.read(context)?.deviceId
        if (linked != null && linked != deviceId) return
        Log.i(TAG, "The server ended this device's link; stopping the protection")
        if (linked != null) LinkedDeviceStore.markEndedByServer(context)
        clearDeviceData(context)
        stopAll(context)
    }

    /** Deletes the Room data (rule cache, pending rule events). Never on the main thread, never
     * cancelled half-way, never throws (logs the exception type only). */
    suspend fun clearDeviceData(context: Context) {
        withContext(NonCancellable + Dispatchers.IO) {
            runCatching { NetProtectDatabase.getInstance(context).clearAllTables() }
                .onFailure { Log.w(TAG, "Local data clear failed: ${it.javaClass.simpleName}") }
        }
    }

    /** Sprint 73 (T5): the watch, shared by EnforcementWatchWorker (no network constraint) and
     * SyncWorker. If RuleEnforcementService stopped stamping EnforcementLiveness while the link,
     * the session and usage access are still there, start it again. Local only: no network. From a
     * worker the app is usually in the background, where Android 12+ only allows a
     * foreground-service start with an exemption (battery optimization off for NetProtect); without
     * it the start is refused and logged. Returns what the liveness marker said before any restart.
     */
    fun restartEnforcementIfDead(context: Context): Boolean? {
        val wasActive = EnforcementLiveness.isRecentlyActive(context)
        if (wasActive != true) {
            restorableDeviceId(context)?.let { linked -> tryStartEnforcement(context, linked) }
        }
        return wasActive
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
        // started from the background cannot read the location (and from API 34 it is refused)
        // unless ACCESS_BACKGROUND_LOCATION is granted (Sprint 73, T6); without it, it is left for
        // the next time the app is opened.
        if (LocationReportingService.canStartFromBackground(context) && LocationPermission.isGranted(context)) {
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
