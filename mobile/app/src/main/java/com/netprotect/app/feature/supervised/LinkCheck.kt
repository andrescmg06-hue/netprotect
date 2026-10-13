package com.netprotect.app.feature.supervised

import com.netprotect.app.core.network.MyDeviceInfo

/** Sprint 64 (B1): what one `/devices/me` answer means for the supervised shell. */
sealed interface LinkCheck {
    /** Linked to at least one tutor. [tutorLabel] is presentation data only. */
    data class Linked(val info: MyDeviceInfo, val tutorLabel: String) : LinkCheck {
        val deviceId: String get() = info.deviceId
    }

    /** The server answered and this device is no longer linked: 404, or a row with no tutors left. */
    data object Unlinked : LinkCheck

    /** No usable answer (offline, timeout, 5xx, unreadable body). Never treated as an unlink:
     * the state and the local cache stay as they are. */
    data object Inconclusive : LinkCheck
}

/** Classifies the outcome of `DeviceClient.getMyDevice`: a success holding null is the 404, and any
 * failure — network or otherwise — is [LinkCheck.Inconclusive]. The heartbeat's `status` is not an
 * input on purpose: ALERT hides UNLINKED there. */
fun classifyLinkCheck(result: Result<MyDeviceInfo?>): LinkCheck {
    if (result.isFailure) return LinkCheck.Inconclusive
    val info = result.getOrNull() ?: return LinkCheck.Unlinked
    val tutorLabel = info.tutorLabel?.takeIf { it.isNotBlank() && info.tutors.isNotEmpty() }
        ?: return LinkCheck.Unlinked
    return LinkCheck.Linked(info, tutorLabel)
}

/** Whether [check] must replace the shell's `Linked(currentDeviceId)` state. Only a real unlink or a
 * different device identity does; a change in tutors, names or last contact never does, so a B1
 * refresh cannot restart any service (G-19). */
fun changesLinkIdentity(currentDeviceId: String, check: LinkCheck): Boolean = when (check) {
    is LinkCheck.Linked -> check.deviceId != currentDeviceId
    LinkCheck.Unlinked -> true
    LinkCheck.Inconclusive -> false
}
