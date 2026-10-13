package com.netprotect.app.feature.home

import com.netprotect.app.core.network.UiError

/** Sprint 63: which variant of the sign-in screen (lámina 01) a login error selects. */
sealed interface LoginNotice {
    data object None : LoginNotice
    data object NoInternet : LoginNotice
    data object ServiceUnavailable : LoginNotice
    /** "Vuelve a entrar": the session ended by itself and the user has to sign in again. */
    data object SessionExpired : LoginNotice
    data class Other(val message: String) : LoginNotice
}

/** The login receives its error as the text [HomeScreen] already built from a [UiError] (sign-in,
 * session restore or health check), so the variant is recovered from that same text. Anything that is
 * not one of the typed causes keeps its own message, shown with the generic error style. */
fun loginNotice(error: String?): LoginNotice = when (error) {
    null -> LoginNotice.None
    UiError.NoInternet.message -> LoginNotice.NoInternet
    UiError.ServiceUnavailable.message -> LoginNotice.ServiceUnavailable
    UiError.SessionExpired.message -> LoginNotice.SessionExpired
    else -> LoginNotice.Other(error)
}

/** With the service down and an error that already says so, the screen shows only "Reintentar";
 * the "Servicio no disponible" line is kept for any other case, so the state is never left unsaid. */
fun showsServiceLine(service: ServiceStatus, notice: LoginNotice): Boolean =
    service != ServiceStatus.Unavailable ||
        (notice != LoginNotice.NoInternet && notice != LoginNotice.ServiceUnavailable)
