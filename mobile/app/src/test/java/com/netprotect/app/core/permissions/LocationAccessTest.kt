package com.netprotect.app.core.permissions

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationAccessTest {

    @Test
    fun `fine granted is precise`() {
        assertEquals(LocationAccess.PRECISE, LocationPermission.accessFrom(fineGranted = true, coarseGranted = true))
    }

    @Test
    fun `only coarse granted, e g approximate chosen in the Android 12 dialog, is approximate`() {
        assertEquals(LocationAccess.APPROXIMATE, LocationPermission.accessFrom(fineGranted = false, coarseGranted = true))
    }

    @Test
    fun `nothing granted is none`() {
        assertEquals(LocationAccess.NONE, LocationPermission.accessFrom(fineGranted = false, coarseGranted = false))
    }
}

/** Sprint 73 (T6): ACCESS_BACKGROUND_LOCATION is needed only from Android 11 (API 30) and decides what the
 * "Ubicación" permission asks for next. Pure functions: no Context. */
class BackgroundLocationTest {

    @Test
    fun `the background permission is needed from API 30 and not below`() {
        assertEquals(false, LocationPermission.backgroundNeeded(26))
        assertEquals(false, LocationPermission.backgroundNeeded(29))
        assertEquals(true, LocationPermission.backgroundNeeded(30))
        assertEquals(true, LocationPermission.backgroundNeeded(34))
        assertEquals(true, LocationPermission.backgroundNeeded(36))
    }

    @Test
    fun `below API 30 the background step is ready whether or not the permission is granted`() {
        assertEquals(true, LocationPermission.backgroundReady(sdkInt = 29, backgroundGranted = false))
        assertEquals(true, LocationPermission.backgroundReady(sdkInt = 26, backgroundGranted = true))
    }

    @Test
    fun `from API 30 the background step is ready only when the permission is granted`() {
        assertEquals(false, LocationPermission.backgroundReady(sdkInt = 30, backgroundGranted = false))
        assertEquals(false, LocationPermission.backgroundReady(sdkInt = 36, backgroundGranted = false))
        assertEquals(true, LocationPermission.backgroundReady(sdkInt = 30, backgroundGranted = true))
        assertEquals(true, LocationPermission.backgroundReady(sdkInt = 36, backgroundGranted = true))
    }

    @Test
    fun `nothing or approximate granted asks for the foreground dialog first`() {
        listOf(LocationAccess.NONE, LocationAccess.APPROXIMATE).forEach { access ->
            listOf(false, true).forEach { ready ->
                assertEquals(LocationRequest.FOREGROUND_DIALOG, LocationPermission.nextRequest(access, ready))
            }
        }
    }

    @Test
    fun `precise without the background step asks for it behind its explanation`() {
        assertEquals(
            LocationRequest.BACKGROUND_EXPLANATION,
            LocationPermission.nextRequest(LocationAccess.PRECISE, backgroundReady = false),
        )
    }

    @Test
    fun `precise with the background step ready has nothing left to ask`() {
        assertEquals(
            LocationRequest.FOREGROUND_DIALOG,
            LocationPermission.nextRequest(LocationAccess.PRECISE, backgroundReady = true),
        )
    }
}
