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
