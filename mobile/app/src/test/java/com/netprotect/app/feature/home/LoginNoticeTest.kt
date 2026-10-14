package com.netprotect.app.feature.home

import com.netprotect.app.core.network.UiError
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 63: each typed login error selects its variant of lámina 01; the rest stay generic. */
class LoginNoticeTest {

    @Test
    fun noErrorIsNoNotice() {
        assertEquals(LoginNotice.None, loginNotice(null))
    }

    @Test
    fun typedCausesSelectTheirVariant() {
        assertEquals(LoginNotice.NoInternet, loginNotice(UiError.NoInternet.message))
        assertEquals(LoginNotice.ServiceUnavailable, loginNotice(UiError.ServiceUnavailable.message))
        assertEquals(LoginNotice.SessionExpired, loginNotice(UiError.SessionExpired.message))
    }

    @Test
    fun theSignInMappingReachesTheVariants() {
        assertEquals(LoginNotice.NoInternet, loginNotice(signInErrorMessage(IOException(), online = { false })))
        assertEquals(LoginNotice.ServiceUnavailable, loginNotice(signInErrorMessage(IOException(), online = { true })))
    }

    @Test
    fun anyOtherMessageIsKeptAsIs() {
        assertEquals(LoginNotice.Other("No se pudo iniciar sesión"), loginNotice("No se pudo iniciar sesión"))
        assertEquals(LoginNotice.Other(UiError.RateLimited.message), loginNotice(UiError.RateLimited.message))
    }

    @Test
    fun theServiceLineIsHiddenOnlyWhenTheBannerAlreadySaysTheServiceIsDown() {
        assertFalse(showsServiceLine(ServiceStatus.Unavailable, LoginNotice.ServiceUnavailable))
        assertFalse(showsServiceLine(ServiceStatus.Unavailable, LoginNotice.NoInternet))
        assertTrue(showsServiceLine(ServiceStatus.Unavailable, LoginNotice.None))
        assertTrue(showsServiceLine(ServiceStatus.Unavailable, LoginNotice.Other("No se pudo iniciar sesión")))
        assertTrue(showsServiceLine(ServiceStatus.Ready, LoginNotice.SessionExpired))
        assertTrue(showsServiceLine(ServiceStatus.Checking, LoginNotice.NoInternet))
    }
}
