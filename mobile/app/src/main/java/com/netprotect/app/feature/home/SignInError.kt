package com.netprotect.app.feature.home

import com.netprotect.app.core.network.ApiException
import com.netprotect.app.core.network.AuthApiException
import com.netprotect.app.core.network.NetworkState
import com.netprotect.app.core.network.toUiError
import java.io.IOException

internal const val SIGN_IN_FAILED = "No se pudo iniciar sesión"

/** Sprint 63: the login's error text for a failed sign-in (the Credential Manager cancel path is
 * handled before and stays silent). Network failures, 5xx and 429 get their typed cause from
 * [toUiError]; any other failure keeps its own message (e.g. "no Google account on this phone"),
 * as before. A 401 at login is a rejected Google credential, not an expired session, so it is not
 * sent through [toUiError]. */
internal fun signInErrorMessage(exception: Throwable, online: () -> Boolean = NetworkState::isOnline): String {
    val status = (exception as? AuthApiException)?.statusCode ?: (exception as? ApiException)?.statusCode
    val typed = exception is IOException || status == 429 || (status != null && status >= 500)
    return if (typed) exception.toUiError(online).message else exception.message ?: SIGN_IN_FAILED
}
