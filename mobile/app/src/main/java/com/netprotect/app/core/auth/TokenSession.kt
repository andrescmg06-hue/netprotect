package com.netprotect.app.core.auth

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.AuthApiException
import com.netprotect.app.core.network.TokenPair
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Where the rotating refresh token is persisted (TokenStore in the app, a map in tests). */
interface RefreshTokenStore {
    fun read(): String?
    fun save(token: String)
    fun clear()
}

/** The session can no longer be renewed: the refresh token was rejected, is missing, or the
 * user signed out while a renewal was in flight. Carries no token material in its message. */
class SessionExpiredException : Exception("session_expired")

/** Sprint 41: the single owner of this process's tokens — the UI, the foreground services and
 * the workers all ask it for an access token instead of each keeping (or renewing) their own.
 *
 * Why one owner: the backend rotates the refresh token on every use (`auth.py` `refresh_tokens`
 * revokes the row it was given), so two callers renewing from the same stored token at once means
 * one of them gets 401. Before this, `AuthRepository.restoreSession()` treated that 401 — and any
 * network error — as "signed out" and wiped the token the winner had just saved, leaving the whole
 * device signed out. Here every renewal runs under one [Mutex] (all components share one process:
 * the manifest declares no `android:process`), and a caller whose token was already replaced gets
 * the new one without touching the network (single-flight).
 *
 * Only a rejected refresh (HTTP 401) ends the session; a network failure propagates as-is and
 * leaves the stored token untouched, so being offline is never mistaken for being signed out.
 */
class TokenSession(
    private val store: RefreshTokenStore,
    private val refreshCall: suspend (refreshToken: String) -> TokenPair,
    private val clock: () -> Long = System::currentTimeMillis,
    // Best-effort revocation of a refresh token this session received but must not keep (the
    // user signed out while that renewal was in flight).
    private val revokeCall: suspend (refreshToken: String) -> Unit = {},
) {
    private val mutex = Mutex()

    // Short, non-suspending lock for the state below: install()/signOut()/expire() and the write
    // of a renewal's result must not interleave, or a sign-out could be undone by a renewal that
    // passed its generation check a microsecond earlier. The Mutex above serializes renewals;
    // this one never waits on the network.
    private val stateLock = Any()

    @Volatile private var accessToken: String? = null
    @Volatile private var renewAtMillis = 0L

    // Bumped by install()/signOut(): a renewal that started before either must not write its
    // result back (it would resurrect a session the user just ended).
    private val generation = AtomicInteger(0)

    // Backoff after a renewal that failed for any reason other than a rejected refresh token
    // (offline, 429, 5xx): until then no caller touches the network again. Without it every
    // caller retried at once, and the backend's refresh rate limit (30 per 15 min per IP, which
    // also counts failures) would lock out every phone behind the same IP. Read under [mutex];
    // install()/signOut() only reset it, so a racing reset costs at most one early retry.
    private var failures = 0
    private var retryNotBeforeMillis = 0L
    private var lastFailure: Exception? = null

    private val expiredFlag = MutableStateFlow(false)

    /** Becomes true when the session ends by itself (not by signOut); the UI returns to login. */
    val expired: StateFlow<Boolean> = expiredFlag.asStateFlow()

    fun hasStoredSession(): Boolean = store.read() != null

    /** Last known access token, possibly expired — only for callers that cannot suspend. */
    fun currentAccessToken(): String? = accessToken

    fun install(tokens: TokenPair) = synchronized(stateLock) {
        generation.incrementAndGet()
        store.save(tokens.refreshToken)
        remember(tokens)
        expiredFlag.value = false
    }

    /** A token that is not near expiry, renewing first if needed. */
    suspend fun validAccessToken(): String {
        val cached = accessToken
        if (cached != null && clock() < renewAtMillis) return cached
        return renewReplacing(cached)
    }

    /** Called after the backend rejected [staleToken]. Renews at most once for all callers that
     * saw the same stale token. */
    suspend fun refreshAfterUnauthorized(staleToken: String): String = renewReplacing(staleToken)

    /** Ends the session locally and returns the refresh token so the caller can revoke it on the
     * backend. Does not wait for an in-flight renewal: the generation check discards its result. */
    fun signOut(): String? = synchronized(stateLock) {
        generation.incrementAndGet()
        val refreshToken = store.read()
        store.clear()
        forget()
        refreshToken
    }

    private suspend fun renewReplacing(stale: String?): String = mutex.withLock {
        val current = accessToken
        if (current != null && current != stale && clock() < renewAtMillis) return current
        lastFailure?.let { if (clock() < retryNotBeforeMillis) throw it }

        val (startedAt, refreshToken) = synchronized(stateLock) { generation.get() to store.read() }
        if (refreshToken == null) throw expire(startedAt)

        // NonCancellable: once the POST is sent the backend has already revoked the old refresh
        // token. If the caller (a screen being disposed, a worker being stopped) were cancelled
        // now, the new one would be lost and the next renewal would end the whole session.
        val renewed: String? = withContext(NonCancellable) {
            val tokens = try {
                refreshCall(refreshToken)
            } catch (exception: AuthApiException) {
                if (exception.statusCode == 401) throw expire(startedAt)
                throw backOff(exception)
            } catch (exception: IOException) {
                throw backOff(exception)
            }
            val kept = synchronized(stateLock) {
                if (generation.get() == startedAt) {
                    store.save(tokens.refreshToken)
                    remember(tokens)
                    true
                } else {
                    false
                }
            }
            if (kept) {
                tokens.accessToken
            } else {
                runCatching { revokeCall(tokens.refreshToken) }
                null
            }
        }
        renewed ?: throw SessionExpiredException()
    }

    private fun backOff(exception: Exception): Exception {
        failures += 1
        // 5 s, 10 s, 20 s … capped at 5 min; a 429 waits at least one minute.
        val delayMillis = minOf(5_000L shl minOf(failures - 1, 6), 300_000L)
        val floorMillis = if ((exception as? AuthApiException)?.statusCode == 429) 60_000L else 0L
        retryNotBeforeMillis = clock() + maxOf(delayMillis, floorMillis)
        lastFailure = exception
        return exception
    }

    private fun remember(tokens: TokenPair) {
        accessToken = tokens.accessToken
        // Renew a little before the backend's expiry: at most 60 s early, and never more than
        // half the lifetime (a 1-minute TTL in the Sprint 41 test still gets used for 30 s).
        val lifetimeMillis = tokens.expiresIn * 1000L
        val marginMillis = minOf(60_000L, lifetimeMillis / 2)
        renewAtMillis = clock() + lifetimeMillis - marginMillis
        failures = 0
        retryNotBeforeMillis = 0L
        lastFailure = null
    }

    private fun forget() {
        accessToken = null
        renewAtMillis = 0L
        failures = 0
        retryNotBeforeMillis = 0L
        lastFailure = null
    }

    private fun expire(startedAt: Int): SessionExpiredException {
        synchronized(stateLock) {
            if (generation.get() == startedAt) {
                store.clear()
                forget()
                expiredFlag.value = true
            }
        }
        return SessionExpiredException()
    }
}

/** The 401 details the backend uses for a missing/expired/invalid access token
 * (`backend/app/api/deps.py`). Other 401s — e.g. pairing's `invalid_or_expired_code` — are about
 * the request itself and must not trigger a token renewal. */
private val ACCESS_TOKEN_REJECTED = setOf("invalid_access_token", "not_authenticated")

fun ApiException.isAccessTokenRejected(): Boolean =
    statusCode == 401 && message in ACCESS_TOKEN_REJECTED

/** Runs [block] with a valid access token; if the backend rejects the token, renews once and
 * retries once. A second rejection propagates (no loop). */
suspend fun <T> TokenSession.authorized(block: suspend (accessToken: String) -> T): T {
    val token = validAccessToken()
    return try {
        block(token)
    } catch (exception: ApiException) {
        if (!exception.isAccessTokenRejected()) throw exception
        block(refreshAfterUnauthorized(token))
    }
}
