package com.netprotect.app.feature.supervised.consent

import java.time.Duration
import java.time.Instant

/**
 * How long a screen-share request waits for the child's answer, counted from when it reached the
 * phone. The limit exists only on the phone (plan, technical question 9); the shell's timeout is to
 * be derived from this value so the countdown and the real expiry can't drift apart.
 */
const val CONSENT_WINDOW_SECONDS = 120L

private const val CONSENT_WINDOW_MS = CONSENT_WINDOW_SECONDS * 1_000L

private fun remainingMillis(requestedAt: Instant, now: Instant): Long {
    val elapsed = Duration.between(requestedAt, now).toMillis().coerceAtLeast(0L)
    return (CONSENT_WINDOW_MS - elapsed).coerceIn(0L, CONSENT_WINDOW_MS)
}

/**
 * Whole seconds left, rounded up so the counter reads "2 min" when the request arrives and only
 * reaches 0 at the moment it expires. Never negative; a [now] before [requestedAt] (clock change)
 * counts as the full window.
 */
fun consentRemainingSeconds(requestedAt: Instant, now: Instant): Long =
    (remainingMillis(requestedAt, now) + 999L) / 1_000L

fun isConsentExpired(requestedAt: Instant, now: Instant): Boolean =
    remainingMillis(requestedAt, now) == 0L

/** Share of the window still left, 1 when the request arrives and 0 once it expired (the bar drains). */
fun consentProgress(requestedAt: Instant, now: Instant): Float =
    remainingMillis(requestedAt, now).toFloat() / CONSENT_WINDOW_MS

/** "2 min", "1 min 48 s", "1 min", "45 s", "0 s" — the format of sheet 06. */
fun formatConsentRemaining(remainingSeconds: Long): String {
    val seconds = remainingSeconds.coerceAtLeast(0L)
    val minutes = seconds / 60
    val rest = seconds % 60
    return when {
        minutes == 0L -> "$rest s"
        rest == 0L -> "$minutes min"
        else -> "$minutes min $rest s"
    }
}

fun consentCountdownText(requestedAt: Instant, now: Instant): String =
    "Esta solicitud caduca en ${formatConsentRemaining(consentRemainingSeconds(requestedAt, now))}"
