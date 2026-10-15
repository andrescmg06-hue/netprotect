package com.netprotect.app.feature.supervised.linked

import com.netprotect.app.core.status.REPORT_STALE_AFTER_SECONDS
import com.netprotect.app.core.status.isReportRecent
import com.netprotect.app.ui.components.SEAL_ARC_COUNT
import com.netprotect.app.ui.components.SealArcState
import com.netprotect.app.ui.components.sealStatesFor
import java.time.Instant

/** Headline state of Inicio (lámina 03), computed by [homeStatus]. */
sealed interface HomeStatus {
    data object AllWorking : HomeStatus
    data class MissingStep(val pending: Int) : HomeStatus
    data object NoRecentReport : HomeStatus
}

/**
 * Rule §2.3 of the redesign plan, in priority order: (1) any pending permission → "Falta un paso";
 * (2) otherwise no successful heartbeat within [staleAfterSeconds], or none yet → "Sin reporte reciente";
 * (3) otherwise "Todo funciona". A missing report never hides a pending permission: fixing the
 * permission is the one thing the child can act on. [now] is injected so the rule never reads a clock.
 */
fun homeStatus(
    pendingPermissions: Int,
    lastHeartbeatOk: Instant?,
    now: Instant,
    staleAfterSeconds: Long = REPORT_STALE_AFTER_SECONDS,
): HomeStatus = when {
    pendingPermissions > 0 -> HomeStatus.MissingStep(pendingPermissions)
    !isReportRecent(lastHeartbeatOk, now, staleAfterSeconds) -> HomeStatus.NoRecentReport
    else -> HomeStatus.AllWorking
}

/** Headline of the hero panel (02_PANTALLAS §03). */
fun homeStatusTitle(status: HomeStatus): String = when (status) {
    HomeStatus.AllWorking -> "Todo funciona"
    is HomeStatus.MissingStep -> "Falta un paso"
    HomeStatus.NoRecentReport -> "Sin reporte reciente"
}

/** Sentence under the headline (02_PANTALLAS §03), with the singular for one pending permission. */
fun homeStatusDescription(status: HomeStatus): String = when (status) {
    HomeStatus.AllWorking ->
        "Los $SEAL_ARC_COUNT permisos están configurados y el dispositivo se comunicó hace un momento."
    is HomeStatus.MissingStep ->
        if (status.pending == 1) {
            "Falta 1 permiso para que NetProtect funcione bien en este teléfono."
        } else {
            "Faltan ${status.pending} permisos para que NetProtect funcione bien en este teléfono."
        }
    HomeStatus.NoRecentReport ->
        "No recibimos una actualización reciente del dispositivo. El vínculo sigue activo."
}

/** The hero button: only "Falta un paso" offers one. */
fun homeStatusAction(status: HomeStatus): String? = when (status) {
    is HomeStatus.MissingStep -> "Revisar permisos"
    HomeStatus.AllWorking, HomeStatus.NoRecentReport -> null
}

/**
 * Seal arcs for Inicio. Only "Sin reporte reciente" greys every arc; with a pending permission and no
 * report the headline is "Falta un paso", so the arcs keep showing which permissions are pending.
 */
fun homeSealStates(status: HomeStatus, readyCount: Int): List<SealArcState> =
    sealStatesFor(readyCount, reporting = status != HomeStatus.NoRecentReport)
