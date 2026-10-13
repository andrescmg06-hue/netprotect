package com.netprotect.app.feature.supervised.consent

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConsentWindowTest {

    private val requestedAt = Instant.parse("2026-10-01T15:00:00Z")
    private fun leftAt(remaining: Long) = requestedAt.plusSeconds(CONSENT_WINDOW_SECONDS - remaining)

    @Test
    fun windowIsTwoMinutes() {
        assertEquals(120L, CONSENT_WINDOW_SECONDS)
    }

    @Test
    fun remainingSecondsCountDownAndNeverGoNegative() {
        for (left in listOf(120L, 119L, 108L, 60L, 59L, 1L, 0L)) {
            assertEquals(left, consentRemainingSeconds(requestedAt, leftAt(left)))
        }
        assertEquals(0L, consentRemainingSeconds(requestedAt, requestedAt.plusSeconds(500)))
        // A clock that went backwards never shows more than the window.
        assertEquals(120L, consentRemainingSeconds(requestedAt, requestedAt.minusSeconds(30)))
    }

    @Test
    fun partialSecondsRoundUpSoZeroMeansExpired() {
        assertEquals(120L, consentRemainingSeconds(requestedAt, requestedAt.plusMillis(400)))
        assertEquals(1L, consentRemainingSeconds(requestedAt, requestedAt.plusMillis(119_999)))
        assertFalse(isConsentExpired(requestedAt, requestedAt.plusMillis(119_999)))
    }

    @Test
    fun expiresExactlyAtTheEndOfTheWindow() {
        assertFalse(isConsentExpired(requestedAt, requestedAt))
        assertFalse(isConsentExpired(requestedAt, leftAt(1)))
        assertTrue(isConsentExpired(requestedAt, leftAt(0)))
        assertTrue(isConsentExpired(requestedAt, requestedAt.plusSeconds(500)))
        assertFalse(isConsentExpired(requestedAt, requestedAt.minusSeconds(30)))
    }

    @Test
    fun progressDrainsFromOneToZero() {
        assertEquals(1f, consentProgress(requestedAt, requestedAt), 0f)
        assertEquals(0.5f, consentProgress(requestedAt, leftAt(60)), 1e-6f)
        assertEquals(0.9f, consentProgress(requestedAt, leftAt(108)), 1e-6f)
        assertEquals(0f, consentProgress(requestedAt, leftAt(0)), 0f)
        assertEquals(0f, consentProgress(requestedAt, requestedAt.plusSeconds(500)), 0f)
        assertEquals(1f, consentProgress(requestedAt, requestedAt.minusSeconds(30)), 0f)
    }

    @Test
    fun remainingTimeReadsLikeSheet06() {
        assertEquals("2 min", formatConsentRemaining(120))
        assertEquals("1 min 59 s", formatConsentRemaining(119))
        assertEquals("1 min 48 s", formatConsentRemaining(108))
        assertEquals("1 min", formatConsentRemaining(60))
        assertEquals("59 s", formatConsentRemaining(59))
        assertEquals("1 s", formatConsentRemaining(1))
        assertEquals("0 s", formatConsentRemaining(0))
        assertEquals("0 s", formatConsentRemaining(-5))
    }

    @Test
    fun countdownTextUsesTheSheetSentence() {
        assertEquals("Esta solicitud caduca en 2 min", consentCountdownText(requestedAt, requestedAt))
        assertEquals("Esta solicitud caduca en 1 min 48 s", consentCountdownText(requestedAt, leftAt(108)))
        assertEquals("Esta solicitud caduca en 45 s", consentCountdownText(requestedAt, leftAt(45)))
        assertEquals("Esta solicitud caduca en 0 s", consentCountdownText(requestedAt, requestedAt.plusSeconds(500)))
    }
}
