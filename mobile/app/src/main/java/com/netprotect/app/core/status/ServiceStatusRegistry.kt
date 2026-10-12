package com.netprotect.app.core.status

import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the services themselves last said about being alive. In-process memory only: the screen
 * that reads it lives in the same process, and nothing here is worth persisting. */
data class ServiceStatus(
    val lastHeartbeatOk: Instant? = null,
    val enforcementRunning: Boolean = false,
    val locationRunning: Boolean = false,
    val screenShareActive: Boolean = false,
    val screenShareSince: Instant? = null,
    val realtimeConnected: Boolean = false,
)

/**
 * Sprint 50: one place that knows what is really running on this phone, for "Estado de NetProtect"
 * and the in-app banner during a screen share. The services report their own start and stop
 * (`onStartCommand` / `onDestroy`) and the heartbeat loop reports each successful beat; nothing here
 * changes what any service does.
 *
 * A service killed without `onDestroy` would leave a stale "running" here, so the screen never
 * trusts this alone for app control: it also needs EnforcementLiveness (stamped every ~3 s by the
 * service itself) — see [servicesView].
 */
object ServiceStatusRegistry {
    private val mutable = MutableStateFlow(ServiceStatus())
    val status: StateFlow<ServiceStatus> = mutable.asStateFlow()

    fun heartbeatSucceeded(at: Instant) = mutable.update { it.copy(lastHeartbeatOk = at) }
    fun enforcementRunning(running: Boolean) = mutable.update { it.copy(enforcementRunning = running) }
    fun locationRunning(running: Boolean) = mutable.update { it.copy(locationRunning = running) }
    fun realtimeConnected(connected: Boolean) = mutable.update { it.copy(realtimeConnected = connected) }

    fun screenShareStarted(at: Instant) = mutable.update { it.copy(screenShareActive = true, screenShareSince = at) }
    fun screenShareStopped() = mutable.update { it.copy(screenShareActive = false, screenShareSince = null) }

    /** Tests only. */
    internal fun reset() = mutable.update { ServiceStatus() }
}

/** What a row of "Estado de NetProtect" may claim. [Unconfirmed] is preferred over an "Activo"
 * that can't be backed up (plan S50, technical decision). */
enum class ServiceState { Active, Inactive, Unconfirmed }

data class ServicesView(
    val report: ServiceState,
    val lastReport: Instant?,
    val appControl: ServiceState,
    val location: ServiceState,
    /** Shown only while a screen share is really running. */
    val screenShareActive: Boolean,
    val screenShareSince: Instant?,
)

/** A heartbeat is sent every minute while the app is open; three missed beats is no longer "active". */
const val REPORT_STALE_AFTER_SECONDS = 3 * 60L

fun servicesView(status: ServiceStatus, enforcementRecentlyActive: Boolean?, now: Instant): ServicesView {
    val last = status.lastHeartbeatOk
    val report = when {
        last == null -> ServiceState.Unconfirmed
        now.epochSecond - last.epochSecond <= REPORT_STALE_AFTER_SECONDS -> ServiceState.Active
        else -> ServiceState.Unconfirmed
    }
    val appControl = when {
        !status.enforcementRunning -> ServiceState.Inactive
        enforcementRecentlyActive == true -> ServiceState.Active
        // Started but not yet (or no longer) stamping its liveness mark: can't say it works.
        else -> ServiceState.Unconfirmed
    }
    return ServicesView(
        report = report,
        lastReport = last,
        appControl = appControl,
        location = if (status.locationRunning) ServiceState.Active else ServiceState.Inactive,
        screenShareActive = status.screenShareActive,
        screenShareSince = status.screenShareSince,
    )
}
