package com.netprotect.app.core.network

import com.netprotect.app.core.auth.SessionExpiredException
import com.netprotect.app.core.auth.isAccessTokenRejected
import java.io.IOException

/** Sprint 41: what a screen shows when a request fails — a category with a Spanish message,
 * never a raw `HTTP 401` nor a backend code like `invalid_or_expired_code`.
 *
 * Sprint 63: errors by cause. [NoInternet] (the phone has no network) and [ServiceUnavailable]
 * (there is network, but the backend refuses, times out or answers 5xx) replace the old single
 * "offline" and "server" categories, so a 5xx is never shown as "sin conexión". */
sealed class UiError(val message: String) {
    data object NoInternet : UiError("No hay conexión a Internet. Comprueba tu conexión e inténtalo de nuevo.")
    data object ServiceUnavailable :
        UiError("NetProtect no puede conectarse al servicio ahora. Inténtalo de nuevo en unos momentos.")
    // The login screen shows its "Vuelve a entrar" variant when it gets this error.
    data object SessionExpired : UiError("Tu sesión caducó.")
    // The backend answers 404 both for "doesn't exist" and "not yours" on purpose (Sprint 25).
    data object NotFound : UiError("No existe o no tienes acceso.")
    data object RateLimited : UiError("Hiciste demasiados intentos. Espera unos minutos y vuelve a intentarlo.")
    class Rejected(message: String) : UiError(message)
    data object Unknown : UiError("No se pudo completar la acción. Intenta de nuevo.")
}

/** Request-specific backend codes that deserve their own wording. */
private val KNOWN_DETAILS = mapOf(
    // Sprint 63: the supervised child redeems it; lámina 02 tells them who can give a new one.
    "invalid_or_expired_code" to "El código no es válido o ya venció. Pídele uno nuevo a tu tutor.",
    "no_active_code" to "No hay un código activo.",
    "could_not_allocate_code" to "No se pudo generar un código. Intenta de nuevo.",
)

/** An I/O failure (refused, timed out, DNS) means "no Internet" only when the phone has no
 * network; with a network, it is the service that does not answer. */
fun classifyIoFailure(online: Boolean): UiError =
    if (online) UiError.ServiceUnavailable else UiError.NoInternet

/** [online] is injectable so JVM tests run without Android's ConnectivityManager. */
fun Throwable.toUiError(online: () -> Boolean = NetworkState::isOnline): UiError = when (this) {
    is SessionExpiredException -> UiError.SessionExpired
    is IOException -> classifyIoFailure(online())
    // A 401 with a request-specific detail (pairing's invalid_or_expired_code) is not a session
    // expiry: only a rejected access token is (isAccessTokenRejected).
    is ApiException -> if (isAccessTokenRejected()) UiError.SessionExpired else httpError(statusCode, message)
    is AuthApiException -> if (statusCode == 401) UiError.SessionExpired else httpError(statusCode, message)
    else -> UiError.Unknown
}

private fun httpError(statusCode: Int?, detail: String?): UiError {
    KNOWN_DETAILS[detail]?.let { return UiError.Rejected(it) }
    return when {
        statusCode == 404 -> UiError.NotFound
        statusCode == 429 -> UiError.RateLimited
        statusCode != null && statusCode >= 500 -> UiError.ServiceUnavailable
        else -> UiError.Unknown
    }
}
