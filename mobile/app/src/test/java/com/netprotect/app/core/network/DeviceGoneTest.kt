package com.netprotect.app.core.network

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceGoneTest {

    @Test
    fun onlyTheUniform404EndsTheLink() {
        assertTrue(ApiException(DEVICE_NOT_FOUND, 404).isDeviceGone())
        // A 404 for another reason (an older backend without the route) is not an unlink.
        assertFalse(ApiException("Not Found", 404).isDeviceGone())
        assertFalse(ApiException("not_linked", 404).isDeviceGone())
        assertFalse(ApiException(DEVICE_NOT_FOUND, 403).isDeviceGone())
        assertFalse(IOException(DEVICE_NOT_FOUND).isDeviceGone())
    }

    @Test
    fun aPermanentRejectionIsDroppedSoItDoesNotBlockTheQueue() {
        assertEquals(QueuedReportFailure.DROP, classifyQueuedReportFailure(ApiException("validation", 422)))
        assertEquals(QueuedReportFailure.DROP, classifyQueuedReportFailure(ApiException("HTTP 400", 400)))
    }

    @Test
    fun transientFailuresStayQueued() {
        for (status in listOf(401, 403, 404, 408, 429, 500, 503)) {
            assertEquals(
                "HTTP $status",
                QueuedReportFailure.RETRY_LATER,
                classifyQueuedReportFailure(ApiException("HTTP $status", status)),
            )
        }
        assertEquals(QueuedReportFailure.RETRY_LATER, classifyQueuedReportFailure(IOException("offline")))
        assertEquals(QueuedReportFailure.RETRY_LATER, classifyQueuedReportFailure(ApiException("?", null)))
    }

    @Test
    fun theUniform404MeansTheLinkEnded() {
        assertEquals(
            QueuedReportFailure.DEVICE_GONE,
            classifyQueuedReportFailure(ApiException(DEVICE_NOT_FOUND, 404)),
        )
    }
}
