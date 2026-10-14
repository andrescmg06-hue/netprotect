package com.netprotect.app.core.location

import com.netprotect.app.core.status.ServiceStatusRegistry
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAndRecordTest {

    private val earlier = Instant.parse("2026-10-01T14:45:00Z")
    private val now = Instant.parse("2026-10-01T15:00:00Z")

    @After
    fun clean() = ServiceStatusRegistry.reset()

    @Test
    fun aSuccessfulReportStampsItsInstant() = runBlocking {
        assertTrue(reportAndRecord(now = { now }) { })
        assertEquals(now, ServiceStatusRegistry.status.value.locationLastReportedAt)
    }

    @Test
    fun aFailedReportStampsNothing() = runBlocking {
        assertFalse(reportAndRecord(now = { now }) { throw IOException("HTTP 503") })
        assertNull(ServiceStatusRegistry.status.value.locationLastReportedAt)
    }

    @Test
    fun aFailureKeepsTheLastSuccess() = runBlocking {
        reportAndRecord(now = { earlier }) { }
        reportAndRecord(now = { now }) { throw IOException("timeout") }
        assertEquals(earlier, ServiceStatusRegistry.status.value.locationLastReportedAt)
    }
}
