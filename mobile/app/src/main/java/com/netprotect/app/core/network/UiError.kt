package com.netprotect.app.core.network

import com.netprotect.app.core.auth.SessionExpiredException
import com.netprotect.app.core.auth.isAccessTokenRejected
import java.io.IOException

/** Sprint 41: what a screen shows when a request fails — a category with a Spanish message,
 * never a raw `HTTP 401` nor a backend code like `invalid_or_expired_code`. */
sealed class UiError(val message: String) {
    data object Offline : UiError("Sin conexión con el servidor. Revisa tu conexión e intenta de nuevo.")
    data object SessionExpired : UiError("Tu sesión expiró. Vuelve a iniciar sesión.")
    // The backend answers 404 both for "doesn't exist" and "not yours" on purpose (Sprint 25).
    data object NotFound : UiError("No existe o no tienes acceso.")
    data object RateLimited : UiError("Demasiados intentos. Espera un momento e intenta de nuevo.")
    data object Server : UiError("El servidor tuvo un problema. Intenta de nuevo en unos minutos.")
    class Rejected(message: String) : UiError(message)
    data object Unknown : UiError("No se pudo completar la acción. Intenta de nuevo.")
}

/** Request-specific backend codes that deserve their own wording. */
private val KNOWN_DETAILS = mapOf(
    "invalid_or_expired_code" to "El código no es válido o ya venció.",
    "no_active_code" to "No hay un código activo.",
    "could_not_allocate_code" to "No se pudo generar un código. Intenta de nuevo.",
)

fun Throwable.toUiError(): UiError = when (this) {
    is SessionExpiredException -> UiError.SessionExpired
    is IOException -> UiError.Offline
    is ApiException -> if (isAccessTokenRejected()) UiError.SessionExpired else httpError(statusCode, message)
    is AuthApiException -> if (statusCode == 401) UiError.SessionExpired else httpError(statusCode, message)
    else -> UiError.Unknown
}

private fun httpError(statusCode: Int?, detail: String?): UiError {
    KNOWN_DETAILS[detail]?.let { return UiError.Rejected(it) }
    return when {
        statusCode == 404 -> UiError.NotFound
        statusCode == 429 -> UiError.RateLimited
        statusCode != null && statusCode >= 500 -> UiError.Server
        else -> UiError.Unknown
    }
}
