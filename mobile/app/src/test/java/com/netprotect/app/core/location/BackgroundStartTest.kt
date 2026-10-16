package com.netprotect.app.core.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 73 (T6): whether the location service may be restarted with no visible Activity depends on the
 * granted ACCESS_BACKGROUND_LOCATION from Android 11 on, and on nothing below it. */
class BackgroundStartTest {

    @Test
    fun `Android 10 and older can always start from the background`() {
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 26, backgroundGranted = false))
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 29, backgroundGranted = false))
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 29, backgroundGranted = true))
    }

    @Test
    fun `Android 11 and newer cannot without the background permission`() {
        assertFalse(LocationReportingService.canStartFromBackground(sdkInt = 30, backgroundGranted = false))
        assertFalse(LocationReportingService.canStartFromBackground(sdkInt = 34, backgroundGranted = false))
        assertFalse(LocationReportingService.canStartFromBackground(sdkInt = 36, backgroundGranted = false))
    }

    @Test
    fun `Android 11 and newer can once the background permission is granted`() {
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 30, backgroundGranted = true))
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 34, backgroundGranted = true))
        assertTrue(LocationReportingService.canStartFromBackground(sdkInt = 36, backgroundGranted = true))
    }
}
