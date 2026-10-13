package com.netprotect.app.feature.home

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.AuthApiException
import com.netprotect.app.core.network.UiError
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sprint 63: the login names the cause of a network or backend failure and keeps the rest. */
class SignInErrorTest {

    @Test
    fun networkFailuresAreTyped() {
        assertEquals(UiError.NoInternet.message, signInErrorMessage(IOException(), online = { false }))
        assertEquals(UiError.ServiceUnavailable.message, signInErrorMessage(IOException(), online = { true }))
    }

    @Test
    fun serverErrorAndTooManyAttemptsAreTyped() {
        assertEquals(UiError.ServiceUnavailable.message, signInErrorMessage(AuthApiException("HTTP 503", 503)))
        assertEquals(UiError.RateLimited.message, signInErrorMessage(AuthApiException("too_many_attempts", 429)))
        assertEquals(UiError.ServiceUnavailable.message, signInErrorMessage(ApiException("HTTP 500", 500)))
    }

    @Test
    fun aRejectedGoogleCredentialIsNotAnExpiredSession() {
        assertEquals("invalid_google_token", signInErrorMessage(AuthApiException("invalid_google_token", 401)))
    }

    @Test
    fun otherFailuresKeepTheirMessage() {
        val noAccount = "No hay una cuenta de Google en este teléfono. Agrégala en Ajustes e intenta de nuevo."
        assertEquals(noAccount, signInErrorMessage(IllegalStateException(noAccount)))
        assertEquals("No se pudo iniciar sesión", signInErrorMessage(RuntimeException()))
    }
}
