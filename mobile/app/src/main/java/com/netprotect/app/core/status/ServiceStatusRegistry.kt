package com.netprotect.app.core.status

import com.netprotect.app.core.location.LocationReportingService
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
    /** Instant of the last location report the backend accepted (B2). Only the instant: never the
     * location itself. Null until the first success of the current run. */
    val locationLastReportedAt: Instant? = null,
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
    /** Stopping drops the last report too, so each start begins without one (B2). */
    fun locationRunning(running: Boolean) = mutable.update {
        if (running) it.copy(locationRunning = true) else it.copy(locationRunning = false, locationLastReportedAt = null)
    }
    /** Call only after the backend accepted a report; a failure must leave the last success as is. */
    fun locationReported(at: Instant) = mutable.update { it.copy(locationLastReportedAt = at) }
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

/**
 * A heartbeat is sent every minute while the app is open. 300 s matches the backend, which marks the
 * device OFFLINE after 5 min, so the child (Inicio, Estado) and the tutor see the same thing (S64,
 * decision 3; it was 180 s). The single threshold for both screens.
 */
const val REPORT_STALE_AFTER_SECONDS = 5 * 60L

/** True when a successful heartbeat happened at most [staleAfterSeconds] before [now]. */
fun isReportRecent(
    lastHeartbeatOk: Instant?,
    now: Instant,
    staleAfterSeconds: Long = REPORT_STALE_AFTER_SECONDS,
): Boolean = lastHeartbeatOk != null && now.epochSecond - lastHeartbeatOk.epochSecond <= staleAfterSeconds

/**
 * Location is reported every 15 min ([LocationReportingService.REPORT_INTERVAL_MS]). Two intervals
 * plus 5 min of margin (the 30 s fix timeout, token refresh and network) tolerates one missed cycle
 * — no fix, a network blip — before "Estado" stops claiming "Activo" (B2). 35 min.
 */
const val LOCATION_STALE_AFTER_SECONDS = 2 * LocationReportingService.REPORT_INTERVAL_MS / 1000 + 5 * 60L

/** True when a location report succeeded at most [LOCATION_STALE_AFTER_SECONDS] before [now]. */
fun isLocationReportRecent(lastReportedAt: Instant?, now: Instant): Boolean =
    lastReportedAt != null && now.epochSecond - lastReportedAt.epochSecond <= LOCATION_STALE_AFTER_SECONDS

fun servicesView(status: ServiceStatus, enforcementRecentlyActive: Boolean?, now: Instant): ServicesView {
    val last = status.lastHeartbeatOk
    val report = if (isReportRecent(last, now)) ServiceState.Active else ServiceState.Unconfirmed
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
        location = when {
            !status.locationRunning -> ServiceState.Inactive
            isLocationReportRecent(status.locationLastReportedAt, now) -> ServiceState.Active
            // Running, but nothing accepted yet (or not lately): "Sin confirmar", not "Activo".
            else -> ServiceState.Unconfirmed
        },
        screenShareActive = status.screenShareActive,
        screenShareSince = status.screenShareSince,
    )
}
