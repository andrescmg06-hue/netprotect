package com.netprotect.app.core.auth

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.AuthApiException
import com.netprotect.app.core.network.TokenPair
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Sprint 41: TokenSession without Android — an in-memory store and a fake refresh endpoint that
 * rotates like the real one (`backend/app/api/v1/endpoints/auth.py`: each refresh token works once). */
class TokenSessionTest {

    private class MemoryStore(var token: String?) : RefreshTokenStore {
        override fun read() = token
        override fun save(token: String) { this.token = token }
        override fun clear() { token = null }
    }

    /** Accepts only the latest refresh token it issued, and counts network calls. */
    private class FakeBackend(initial: String) {
        val calls = AtomicInteger(0)
        @Volatile var valid: String? = initial
        var failWith: Exception? = null
        var gate: CompletableDeferred<Unit>? = null

        suspend fun refresh(refreshToken: String): TokenPair {
            val n = calls.incrementAndGet()
            gate?.await()
            delay(20)
            failWith?.let { throw it }
            synchronized(this) {
                if (refreshToken != valid) throw AuthApiException("invalid_refresh_token", 401)
                valid = "r$n"
            }
            return TokenPair(accessToken = "a$n", refreshToken = "r$n", expiresIn = 900)
        }
    }

    private fun session(store: MemoryStore, backend: FakeBackend, now: () -> Long = { 0L }) =
        TokenSession(store, backend::refresh, now)

    @Test
    fun concurrentCallersWithTheSameStaleTokenCauseOneRefresh() = runBlocking {
        val backend = FakeBackend("r0")
        val store = MemoryStore(null)
        val tokens = session(store, backend)
        tokens.install(TokenPair("a0", "r0", 900))

        val results = (1..20).map {
            async(Dispatchers.Default) { tokens.refreshAfterUnauthorized("a0") }
        }.awaitAll()

        assertEquals(1, backend.calls.get())
        assertTrue(results.all { it == "a1" })
        assertEquals("r1", store.token)
    }

    @Test
    fun validAccessTokenDoesNotHitTheNetworkUntilNearExpiry() = runBlocking {
        var now = 0L
        val backend = FakeBackend("r0")
        val tokens = session(MemoryStore(null), backend) { now }
        tokens.install(TokenPair("a0", "r0", 900))

        now = 800_000 // 13 min 20 s: still more than 60 s left
        assertEquals("a0", tokens.validAccessToken())
        assertEquals(0, backend.calls.get())

        now = 850_000 // inside the 60 s margin
        assertEquals("a1", tokens.validAccessToken())
        assertEquals(1, backend.calls.get())
    }

    @Test
    fun oneMinuteTokenIsRenewedHalfwayThrough() = runBlocking {
        var now = 0L
        val backend = FakeBackend("r0")
        val tokens = session(MemoryStore(null), backend) { now }
        tokens.install(TokenPair("a0", "r0", 60))

        now = 29_000
        assertEquals("a0", tokens.validAccessToken())
        now = 31_000
        assertEquals("a1", tokens.validAccessToken())
    }

    @Test
    fun authorizedRetriesOnceAfterRejectedAccessToken() = runBlocking {
        val backend = FakeBackend("r0")
        val tokens = session(MemoryStore(null), backend)
        tokens.install(TokenPair("a0", "r0", 900))
        val seen = mutableListOf<String>()

        val result = tokens.authorized { token ->
            seen += token
            if (token == "a0") throw ApiException("invalid_access_token", 401)
            "ok"
        }

        assertEquals("ok", result)
        assertEquals(listOf("a0", "a1"), seen)
        assertEquals(1, backend.calls.get())
    }

    @Test
    fun authorizedDoesNotLoopWhenTheRenewedTokenIsAlsoRejected() = runBlocking {
        val backend = FakeBackend("r0")
        val tokens = session(MemoryStore(null), backend)
        tokens.install(TokenPair("a0", "r0", 900))
        var attempts = 0

        try {
            tokens.authorized<Unit> {
                attempts++
                throw ApiException("invalid_access_token", 401)
            }
            fail("expected ApiException")
        } catch (exception: ApiException) {
            assertEquals(401, exception.statusCode)
        }
        assertEquals(2, attempts)
        assertEquals(1, backend.calls.get())
    }

    @Test
    fun a401ThatIsNotAboutTheTokenIsNotRetried() = runBlocking {
        val backend = FakeBackend("r0")
        val tokens = session(MemoryStore(null), backend)
        tokens.install(TokenPair("a0", "r0", 900))
        var attempts = 0

        try {
            tokens.authorized<Unit> {
                attempts++
                throw ApiException("invalid_or_expired_code", 401) // pairing: a wrong code
            }
            fail("expected ApiException")
        } catch (exception: ApiException) {
            assertEquals(UiError.Rejected::class, exception.toUiError()::class)
        }
        assertEquals(1, attempts)
        assertEquals(0, backend.calls.get())
    }

    @Test
    fun rejectedRefreshTokenEndsTheSession() = runBlocking {
        val backend = FakeBackend("somebody-else")
        val store = MemoryStore(null)
        val tokens = session(store, backend)
        tokens.install(TokenPair("a0", "r0", 900))

        try {
            tokens.refreshAfterUnauthorized("a0")
            fail("expected SessionExpiredException")
        } catch (_: SessionExpiredException) {
        }
        assertTrue(tokens.expired.value)
        assertNull(store.token)
    }

    @Test
    fun networkFailureKeepsTheStoredSession() = runBlocking {
        val backend = FakeBackend("r0").apply { failWith = SocketTimeoutException("timeout") }
        val store = MemoryStore(null)
        val tokens = session(store, backend)
        tokens.install(TokenPair("a0", "r0", 900))

        try {
            tokens.refreshAfterUnauthorized("a0")
            fail("expected IOException")
        } catch (_: IOException) {
        }
        assertFalse(tokens.expired.value)
        assertEquals("r0", store.token)
        assertTrue(tokens.hasStoredSession())
    }

    @Test
    fun signOutDuringARenewalDoesNotResurrectTheSession() = runBlocking {
        val backend = FakeBackend("r0").apply { gate = CompletableDeferred() }
        val store = MemoryStore(null)
        val tokens = session(store, backend)
        tokens.install(TokenPair("a0", "r0", 900))

        val renewal = async(Dispatchers.Default) { runCatching { tokens.refreshAfterUnauthorized("a0") } }
        while (backend.calls.get() == 0) delay(5)
        assertEquals("r0", tokens.signOut())
        backend.gate!!.complete(Unit)

        assertTrue(renewal.await().exceptionOrNull() is SessionExpiredException)
        assertNull(store.token)
        assertNull(tokens.currentAccessToken())
        assertFalse("signing out is not an expiry", tokens.expired.value)
    }

    @Test
    fun aCancelledCallerDoesNotLoseTheRotatedToken() = runBlocking {
        val backend = FakeBackend("r0").apply { gate = CompletableDeferred() }
        val store = MemoryStore(null)
        val tokens = session(store, backend)
        tokens.install(TokenPair("a0", "r0", 900))

        val caller = launch(Dispatchers.Default) { tokens.refreshAfterUnauthorized("a0") }
        while (backend.calls.get() == 0) delay(5)
        caller.cancel() // e.g. the screen that triggered the renewal is disposed mid-request
        backend.gate!!.complete(Unit)
        caller.join()
        delay(100)

        assertEquals("the backend already rotated r0; r1 must be kept", "r1", store.token)
        assertEquals("a1", tokens.currentAccessToken())
    }

    @Test
    fun failedRenewalBacksOffInsteadOfHammeringTheBackend() = runBlocking {
        var now = 0L
        val backend = FakeBackend("r0").apply { failWith = AuthApiException("too_many_attempts", 429) }
        val tokens = session(MemoryStore(null), backend) { now }
        tokens.install(TokenPair("a0", "r0", 60))
        now = 40_000 // due for renewal

        repeat(10) { runCatching { tokens.validAccessToken() } }
        assertEquals("one attempt, then backoff", 1, backend.calls.get())

        now += 61_000 // a 429 waits at least a minute
        backend.failWith = null
        assertEquals("a2", tokens.validAccessToken())
        assertEquals(2, backend.calls.get())
    }

    @Test
    fun aRenewalDiscardedBySignOutIsRevokedOnTheBackend() = runBlocking {
        val backend = FakeBackend("r0").apply { gate = CompletableDeferred() }
        val revoked = mutableListOf<String>()
        val tokens = TokenSession(MemoryStore(null), backend::refresh, { 0L }) { revoked += it }
        tokens.install(TokenPair("a0", "r0", 900))

        val renewal = async(Dispatchers.Default) { runCatching { tokens.refreshAfterUnauthorized("a0") } }
        while (backend.calls.get() == 0) delay(5)
        tokens.signOut()
        backend.gate!!.complete(Unit)
        renewal.await()

        assertEquals(listOf("r1"), revoked)
    }

    @Test
    fun errorsMapToSpanishCategories() {
        assertEquals(UiError.Offline, SocketTimeoutException().toUiError())
        assertEquals(UiError.SessionExpired, SessionExpiredException().toUiError())
        assertEquals(UiError.SessionExpired, ApiException("invalid_access_token", 401).toUiError())
        assertEquals(UiError.NotFound, ApiException("device_not_found", 404).toUiError())
        assertEquals(UiError.RateLimited, ApiException("rate_limited", 429).toUiError())
        assertEquals(UiError.Server, ApiException("HTTP 503", 503).toUiError())
        assertEquals(UiError.Unknown, ApiException("validation", 422).toUiError())
        assertEquals("El código no es válido o ya venció.", ApiException("invalid_or_expired_code", 401).toUiError().message)
        assertFalse(UiError.Offline.message.contains("HTTP"))
    }
}
