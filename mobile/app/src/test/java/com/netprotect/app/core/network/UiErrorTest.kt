package com.netprotect.app.core.network

import com.netprotect.app.core.auth.SessionExpiredException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 63: errors by cause — no Internet, service down, session expired, too many attempts. */
class UiErrorTest {

    @Test
    fun ioFailureDependsOnTheNetwork() {
        assertEquals(UiError.NoInternet, classifyIoFailure(online = false))
        assertEquals(UiError.ServiceUnavailable, classifyIoFailure(online = true))
    }

    @Test
    fun ioExceptionOfflineIsNoInternet() {
        assertEquals(UiError.NoInternet, IOException("unreachable").toUiError(online = { false }))
        assertEquals(UiError.NoInternet, SocketTimeoutException().toUiError(online = { false }))
    }

    @Test
    fun ioExceptionOnlineIsServiceUnavailable() {
        assertEquals(UiError.ServiceUnavailable, ConnectException("refused").toUiError(online = { true }))
        assertEquals(UiError.ServiceUnavailable, SocketTimeoutException().toUiError(online = { true }))
    }

    @Test
    fun serverErrorsAreServiceUnavailableNotOffline() {
        // B4: the network is never consulted for an HTTP answer.
        val neverAsked: () -> Boolean = { throw AssertionError("network state must not be read") }
        assertEquals(UiError.ServiceUnavailable, ApiException("HTTP 500", 500).toUiError(neverAsked))
        assertEquals(UiError.ServiceUnavailable, ApiException("HTTP 503", 503).toUiError(neverAsked))
        assertEquals(UiError.ServiceUnavailable, AuthApiException("HTTP 502", 502).toUiError(neverAsked))
    }

    @Test
    fun tooManyAttemptsIsRateLimited() {
        assertEquals(UiError.RateLimited, ApiException("too_many_attempts", 429).toUiError())
        assertEquals(UiError.RateLimited, AuthApiException("too_many_attempts", 429).toUiError())
    }

    @Test
    fun rejectedAccessTokenIsSessionExpiredButAWrongCodeIsNot() {
        assertEquals(UiError.SessionExpired, ApiException("invalid_access_token", 401).toUiError())
        assertEquals(UiError.SessionExpired, ApiException("not_authenticated", 401).toUiError())
        assertEquals(UiError.SessionExpired, SessionExpiredException().toUiError())
        val wrongCode = ApiException("invalid_or_expired_code", 401).toUiError()
        assertTrue(wrongCode is UiError.Rejected)
        assertEquals("El código no es válido o ya venció.", wrongCode.message)
    }

    @Test
    fun healthCheckFailureIsTyped() {
        assertEquals(UiError.NoInternet, IOException().toHealthUiError(online = { false }))
        assertEquals(UiError.ServiceUnavailable, IOException().toHealthUiError(online = { true }))
        assertEquals(UiError.ServiceUnavailable, IllegalStateException("HTTP 503").toHealthUiError { false })
        assertEquals(UiError.ServiceUnavailable, IllegalArgumentException("malformed answer").toHealthUiError { true })
    }

    @Test
    fun textsAreExact() {
        assertEquals(
            "No hay conexión a Internet. Comprueba tu conexión e inténtalo de nuevo.",
            UiError.NoInternet.message,
        )
        assertEquals(
            "NetProtect no puede conectarse al servicio ahora. Inténtalo de nuevo en unos momentos.",
            UiError.ServiceUnavailable.message,
        )
        assertEquals("Tu sesión caducó.", UiError.SessionExpired.message)
        assertEquals(
            "Hiciste demasiados intentos. Espera unos minutos y vuelve a intentarlo.",
            UiError.RateLimited.message,
        )
        assertEquals("No se pudo completar la acción. Intenta de nuevo.", UiError.Unknown.message)
    }
}
