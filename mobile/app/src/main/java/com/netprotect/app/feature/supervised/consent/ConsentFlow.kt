package com.netprotect.app.feature.supervised.consent

import java.time.Instant

/**
 * Sprint 65 (B5): the `reason` of the `screen_share_stop` the phone sends when a request closes
 * without an answer, so the tutor's page stops waiting. Fixed texts, never data; the backend accepts
 * up to 64 characters. `screen_share_stop` (not `granted=false`) because only a stop frees the
 * tutor's slot on the backend.
 */
const val SCREEN_SHARE_STOP_DISMISSED = "dismissed"
const val SCREEN_SHARE_STOP_EXPIRED = "expired"

/** What can happen to a pending screen-share request. */
sealed interface ConsentEvent {
    /** The tutor's `screen_share_request` reached the phone at [at]. */
    data class Requested(val at: Instant) : ConsentEvent

    /** The child pressed «Continuar» or «Ahora no»; the screen already sent its answer. */
    data object Answered : ConsentEvent

    /** The child left the consent screen with Back: not an answer. */
    data object Back : ConsentEvent

    /** The presentation clock moved to [now]. */
    data class Tick(val now: Instant) : ConsentEvent

    /** The tutor cancelled (`screen_share_stop`) or the link ended. */
    data object Stopped : ConsentEvent
}

/**
 * The result of one event: when the pending request arrived (null once there is none) and, if the
 * tutor must be told the request closed unanswered, the stop reason to send.
 */
data class ConsentStep(val requestedAt: Instant?, val sendStopReason: String? = null)

/**
 * Pure transition for the pending request, so the shell only applies the result. A duplicate
 * request keeps the original time: re-sending must not stretch the child's window.
 */
fun consentStep(requestedAt: Instant?, event: ConsentEvent): ConsentStep = when (event) {
    is ConsentEvent.Requested -> ConsentStep(requestedAt ?: event.at)
    ConsentEvent.Answered, ConsentEvent.Stopped -> ConsentStep(null)
    ConsentEvent.Back ->
        if (requestedAt == null) ConsentStep(null) else ConsentStep(null, SCREEN_SHARE_STOP_DISMISSED)
    is ConsentEvent.Tick -> when {
        requestedAt == null -> ConsentStep(null)
        isConsentExpired(requestedAt, event.now) -> ConsentStep(null, SCREEN_SHARE_STOP_EXPIRED)
        else -> ConsentStep(requestedAt)
    }
}
