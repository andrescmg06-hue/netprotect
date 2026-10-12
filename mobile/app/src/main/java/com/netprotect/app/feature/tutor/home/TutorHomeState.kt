package com.netprotect.app.feature.tutor.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.PairingCode
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.ui.state.LoadState
import kotlin.coroutines.cancellation.CancellationException

/** What the pairing card shows. [Active] always carries the seconds left computed from elapsed
 * time, never from a counter that could drift while the app sleeps. */
sealed interface PairingUi {
    data object Idle : PairingUi
    data object Generating : PairingUi
    data class Active(
        val code: String,
        val remainingSeconds: Int,
        val totalSeconds: Int,
        val revoking: Boolean = false,
    ) : PairingUi
    data object Expired : PairingUi
    data class Failed(val message: String) : PairingUi
}

/** Seconds left of a code issued at [issuedAtMillis] (monotonic clock) that lives [totalSeconds];
 * never negative. Rounds up so the display reaches 0:00 exactly when the code dies. */
fun pairingSecondsLeft(issuedAtMillis: Long, totalSeconds: Int, nowMillis: Long): Int {
    val leftMillis = issuedAtMillis + totalSeconds * 1000L - nowMillis
    return if (leftMillis <= 0) 0 else ((leftMillis + 999) / 1000).toInt()
}

/** Sprint 44 (D-02): the tutor home's state and actions, without Compose UI and without
 * ViewModel. The screen reads the two fields and calls the suspend functions from its coroutine
 * scope; the network calls come in as lambdas so this is testable on the JVM. The pairing code
 * itself is kept only in memory here and never logged. */
class TutorHomeController(
    private val loadDevices: suspend () -> List<DeviceSummary>,
    private val createCode: suspend () -> PairingCode,
    private val revokeCode: suspend () -> Unit,
    private val clock: () -> Long,
) {
    var devices: LoadState<List<DeviceSummary>> by mutableStateOf(LoadState.Loading)
        private set
    var pairing: PairingUi by mutableStateOf(PairingUi.Idle)
        private set

    private var issuedAtMillis = 0L

    suspend fun refreshDevices() {
        devices = LoadState.Loading
        devices = try {
            LoadState.Loaded(loadDevices())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            LoadState.Failed(exception.toUiError().message)
        }
    }

    suspend fun generateCode() {
        if (pairing is PairingUi.Generating) return
        pairing = PairingUi.Generating
        pairing = try {
            val code = createCode()
            issuedAtMillis = clock()
            PairingUi.Active(code.code, code.expiresInSeconds, code.expiresInSeconds)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            PairingUi.Failed(exception.toUiError().message)
        }
    }

    suspend fun revoke() {
        val active = pairing as? PairingUi.Active ?: return
        if (active.revoking) return
        pairing = active.copy(revoking = true)
        pairing = try {
            revokeCode()
            PairingUi.Idle
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            // The code may still be valid on the backend: say so instead of pretending it's gone.
            PairingUi.Failed(exception.toUiError().message)
        }
    }

    /** Called about once a second by the screen while a code is showing. */
    fun tick() {
        val active = pairing as? PairingUi.Active ?: return
        val left = pairingSecondsLeft(issuedAtMillis, active.totalSeconds, clock())
        pairing = if (left == 0) PairingUi.Expired else active.copy(remainingSeconds = left)
    }

    /** "Genera uno nuevo" after an error or an expired code goes back to the idle card. */
    fun dismissPairing() {
        if (pairing is PairingUi.Expired || pairing is PairingUi.Failed) pairing = PairingUi.Idle
    }
}
