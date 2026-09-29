package com.netprotect.app.feature.tutor.sections

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.AlertSilence
import com.netprotect.app.core.network.DeviceAlert
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.ui.state.LoadState
import kotlin.coroutines.cancellation.CancellationException

/** What the Alertas screen loads in one go: the inbox, the active silences and the app names. */
data class AlertsData(
    val alerts: List<DeviceAlert>,
    val silences: List<AlertSilence>,
    val labels: Map<String, String>,
)

/**
 * Sprint 46 (D-02): the state of the Alertas screen and its two write actions.
 *
 * - One action at a time: while one is sent, [busyId] is set and a second tap (on any alert) is
 *   ignored — a double tap on "Silenciar" must not send two requests.
 * - After an action the list is reloaded **without** going back to Loading, so the alert the tutor
 *   just touched stays under their finger (a deliberate exception to LoadState's "reload starts
 *   from Loading"). If that reload fails the previous list stays and the error goes to [actionError].
 * - Authorization is the backend's (a foreign alert is a 404); nothing is decided here.
 */
class AlertsController(
    private val load: suspend () -> AlertsData,
    private val markReadRequest: suspend (alertId: String) -> Unit,
    private val silenceRequest: suspend (alertId: String) -> Unit,
) {
    var state: LoadState<AlertsData> by mutableStateOf(LoadState.Loading)
        private set
    var filter: AlertFilter by mutableStateOf(AlertFilter.All)
    var busyId: String? by mutableStateOf(null)
        private set
    var actionError: String? by mutableStateOf(null)
        private set
    /** The alert whose "¿Silenciar?" dialog is open, or null. */
    var silenceTarget: String? by mutableStateOf(null)
        private set

    /** Full reload (first load, "Reintentar", "Actualizar"). */
    suspend fun refresh() {
        state = LoadState.Loading
        state = loadOrFailed()
    }

    suspend fun markRead(alertId: String) = act(alertId) { markReadRequest(alertId) }

    fun askSilence(alertId: String) {
        if (busyId == null) silenceTarget = alertId
    }

    fun dismissSilence() {
        silenceTarget = null
    }

    suspend fun confirmSilence() {
        val alertId = silenceTarget ?: return
        silenceTarget = null
        act(alertId) { silenceRequest(alertId) }
    }

    fun clearActionError() {
        actionError = null
    }

    private suspend fun act(alertId: String, request: suspend () -> Unit) {
        if (busyId != null) return
        busyId = alertId
        actionError = null
        try {
            request()
        } catch (cancelled: CancellationException) {
            busyId = null
            throw cancelled
        } catch (exception: Exception) {
            actionError = actionMessage(exception)
            busyId = null
            return
        }
        when (val reloaded = loadOrFailed()) {
            is LoadState.Loaded -> state = reloaded
            is LoadState.Failed -> actionError = "Se guardó, pero no se pudo actualizar la lista. ${reloaded.message}"
            LoadState.Loading -> Unit
        }
        busyId = null
    }

    private suspend fun loadOrFailed(): LoadState<AlertsData> = try {
        LoadState.Loaded(load())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (exception: Exception) {
        val error = exception.toUiError()
        LoadState.Failed(error.message, notFound = error == UiError.NotFound)
    }

    private fun actionMessage(exception: Exception): String {
        val error = exception.toUiError()
        return if (error == UiError.NotFound) ALERT_GONE_MESSAGE else error.message
    }
}

const val ALERT_GONE_MESSAGE = "Esta alerta ya no existe o no tienes acceso. Actualiza la lista."

/**
 * Sprint 46 (D-02): Estadísticas with its period selector. Each period is a fresh load; a slow
 * answer for a period the tutor already left is discarded (tapping Hoy → 7 días quickly must not
 * end up showing "Hoy" under the "7 días" tab).
 */
class StatisticsController(private val load: suspend (StatsPeriod) -> StatisticsView) {
    var period: StatsPeriod by mutableStateOf(StatsPeriod.Today)
        private set
    var state: LoadState<StatisticsView> by mutableStateOf(LoadState.Loading)
        private set
    private var generation = 0

    suspend fun select(newPeriod: StatsPeriod) {
        period = newPeriod
        refresh()
    }

    suspend fun refresh() {
        val mine = ++generation
        val requested = period
        state = LoadState.Loading
        val result = try {
            LoadState.Loaded(load(requested))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = exception.toUiError()
            LoadState.Failed(error.message, notFound = error == UiError.NotFound)
        }
        if (mine == generation) state = result
    }
}
