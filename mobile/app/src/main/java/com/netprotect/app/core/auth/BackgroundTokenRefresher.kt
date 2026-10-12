package com.netprotect.app.core.auth

import android.content.Context
import kotlin.coroutines.cancellation.CancellationException

/** Sprint 19: how background work (RuleEnforcementService's poll loop, LocationReportingService,
 * ScreenShareService, SyncWorker, TamperReportWorker) obtains a usable access token on its own —
 * they all run far longer than the 15-minute token lifetime.
 *
 * Sprint 41: no longer renews by itself. It used to read the stored refresh token and rotate it
 * independently of every other caller, so two of them renewing at once meant one got 401 (the
 * backend rotates refresh tokens on every use). It now asks the process-wide [TokenProvider],
 * which renews only when the token is near expiry and only once for everyone.
 */
object BackgroundTokenRefresher {
    /** Null when there is no usable token right now (offline, or the session ended): the caller
     * falls back to its last known token and simply retries next cycle, same as every other
     * network failure in this app. */
    suspend fun refresh(context: Context): String? =
        try {
            TokenProvider.get(context).validAccessToken()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
}
