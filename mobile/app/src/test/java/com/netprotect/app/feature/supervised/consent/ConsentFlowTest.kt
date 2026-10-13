package com.netprotect.app.feature.supervised.consent

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentFlowTest {

    private val requestedAt = Instant.parse("2026-10-01T15:00:00Z")

    @Test
    fun aNewRequestStoresItsArrivalTime() {
        assertEquals(ConsentStep(requestedAt), consentStep(null, ConsentEvent.Requested(requestedAt)))
    }

    @Test
    fun aDuplicateRequestDoesNotRestartTheWindow() {
        val later = requestedAt.plusSeconds(90)
        assertEquals(ConsentStep(requestedAt), consentStep(requestedAt, ConsentEvent.Requested(later)))
    }

    @Test
    fun backClosesTheRequestAndSendsDismissed() {
        assertEquals(
            ConsentStep(null, SCREEN_SHARE_STOP_DISMISSED),
            consentStep(requestedAt, ConsentEvent.Back),
        )
        assertEquals("dismissed", SCREEN_SHARE_STOP_DISMISSED)
    }

    @Test
    fun aTickJustBeforeTheWindowEndsKeepsTheRequest() {
        val justBefore = requestedAt.plusSeconds(CONSENT_WINDOW_SECONDS).minusMillis(1)
        assertEquals(ConsentStep(requestedAt), consentStep(requestedAt, ConsentEvent.Tick(justBefore)))
        assertEquals(ConsentStep(requestedAt), consentStep(requestedAt, ConsentEvent.Tick(requestedAt.plusSeconds(119))))
    }

    @Test
    fun aTickAtTheEndOfTheWindowExpiresAndSendsExpired() {
        val atEnd = requestedAt.plusSeconds(CONSENT_WINDOW_SECONDS)
        assertEquals(
            ConsentStep(null, SCREEN_SHARE_STOP_EXPIRED),
            consentStep(requestedAt, ConsentEvent.Tick(atEnd)),
        )
        assertEquals(
            ConsentStep(null, SCREEN_SHARE_STOP_EXPIRED),
            consentStep(requestedAt, ConsentEvent.Tick(atEnd.plusSeconds(30))),
        )
        assertEquals("expired", SCREEN_SHARE_STOP_EXPIRED)
    }

    @Test
    fun answeringOrStoppingClearsWithoutSendingAnything() {
        assertEquals(ConsentStep(null), consentStep(requestedAt, ConsentEvent.Answered))
        assertEquals(ConsentStep(null), consentStep(requestedAt, ConsentEvent.Stopped))
    }

    @Test
    fun withoutAPendingRequestNothingIsSent() {
        for (event in listOf(
            ConsentEvent.Back,
            ConsentEvent.Tick(requestedAt.plusSeconds(500)),
            ConsentEvent.Answered,
            ConsentEvent.Stopped,
        )) {
            val step = consentStep(null, event)
            assertNull(step.requestedAt)
            assertNull(step.sendStopReason)
        }
    }

    @Test
    fun stopReasonsFitTheBackendLimit() {
        assertTrue(SCREEN_SHARE_STOP_DISMISSED.length <= 64)
        assertTrue(SCREEN_SHARE_STOP_EXPIRED.length <= 64)
    }
}
