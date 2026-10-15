package com.netprotect.app.core.network

/** The backend's uniform 404 for "this device does not exist / is not yours" (`backend/app/api/
 * deps.py`). Since Sprint 73 (T5) the supervised device's own routes also answer it once the
 * device has no active tutor link, so for those routes it means "the link ended". */
const val DEVICE_NOT_FOUND = "device_not_found"

/** Sprint 73 (T5): whether [this] is the backend saying this device is no longer linked to the
 * signed-in account. Matched on the detail, not on the status alone: a 404 for another reason (a
 * route an older backend does not have answers "Not Found") must never end the link. */
fun Throwable.isDeviceGone(): Boolean =
    this is ApiException && statusCode == 404 && message == DEVICE_NOT_FOUND

/** Sprint 73 (T5): what to do with a queued rule event whose report failed. */
enum class QueuedReportFailure {
    /** Offline, timeout, 5xx, or a 4xx that can succeed later (401/403/404/408/429): keep it. */
    RETRY_LATER,

    /** A 4xx the server will reject every time (422 and the like): retrying would block the queue
     * behind it forever, so the event is dropped. */
    DROP,

    /** The server says the device is no longer linked ([isDeviceGone]): the link ended. */
    DEVICE_GONE,
}

private val RETRYABLE_CLIENT_ERRORS = setOf(401, 403, 404, 408, 429)

/** Pure: classifies one failed report. Only [ApiException] carries an HTTP status; anything else
 * (IOException, a parse error) is treated as transient. */
fun classifyQueuedReportFailure(error: Throwable): QueuedReportFailure {
    if (error.isDeviceGone()) return QueuedReportFailure.DEVICE_GONE
    val status = (error as? ApiException)?.statusCode ?: return QueuedReportFailure.RETRY_LATER
    return if (status in 400..499 && status !in RETRYABLE_CLIENT_ERRORS) {
        QueuedReportFailure.DROP
    } else {
        QueuedReportFailure.RETRY_LATER
    }
}
