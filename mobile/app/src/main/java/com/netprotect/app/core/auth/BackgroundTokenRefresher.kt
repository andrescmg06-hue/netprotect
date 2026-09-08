package com.netprotect.app.core.auth

import android.content.Context
import com.netprotect.app.core.network.AuthClient

/** Sprint 19: a self-contained way for background work (RuleEnforcementService's long-running
 * poll loop, SyncWorker) to obtain a fresh access token on its own, independent of
 * AuthRepository's foreground session state.
 *
 * Why this exists: access tokens live 15 minutes (backend's `access_token_ttl_minutes`), but
 * RuleEnforcementService's foreground service and WorkManager's periodic work both run far
 * longer than that between UI interactions — without this, every request they make would start
 * silently 401ing after the first 15 minutes of any session, which would have made this sprint's
 * whole "keeps working across an outage" story false for the far more common case of "the app
 * has just been running a while". Reads the same Keystore-encrypted refresh token AuthRepository
 * uses (TokenStore) and persists its rotation the same way (the backend rotates refresh tokens on
 * every use — reusing a stale one would fail).
 */
object BackgroundTokenRefresher {
    suspend fun refresh(context: Context, baseUrl: String): String? {
        val tokenStore = TokenStore(context)
        val storedRefreshToken = tokenStore.readRefreshToken() ?: return null
        return try {
            val tokens = AuthClient(baseUrl).refresh(storedRefreshToken)
            tokenStore.saveRefreshToken(tokens.refreshToken)
            tokens.accessToken
        } catch (_: Exception) {
            // Offline, or the refresh token was already revoked (signed out elsewhere): the
            // caller falls back to its last known access token and simply retries next cycle,
            // same as every other network failure in this app.
            null
        }
    }
}
