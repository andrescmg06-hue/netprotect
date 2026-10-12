package com.netprotect.app.feature.tutor.device

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.ui.state.LoadState
import kotlin.coroutines.cancellation.CancellationException

const val DEVICE_GONE_MESSAGE = "Este dispositivo ya no existe o no tienes acceso."

/** The backend's rule for a device name (DeviceRenameRequest: 1–255 characters), after trimming
 * the spaces at both ends. Returns the name to send, or null if it can't be saved. */
fun validDeviceName(raw: String): String? = raw.trim().takeIf { it.length in 1..255 }

/** Inline rename editor. [error] is shown under the field; [saving] disables Guardar. */
data class RenameUi(val text: String, val error: String? = null, val saving: Boolean = false) {
    val canSave: Boolean get() = !saving && validDeviceName(text) != null
}

/** Sprint 44 (D-02): the device detail's state and actions. The screen decides nothing about
 * permissions: a 404 from the backend (not yours, or unlinked elsewhere) becomes [LoadState.Failed]
 * with `notFound = true` and the screen offers to go back to the list. */
class DeviceDetailController(
    private val loadDevice: suspend () -> DeviceSummary,
    private val renameDevice: suspend (String) -> DeviceSummary,
    private val unlinkDevice: suspend () -> Unit,
) {
    var device: LoadState<DeviceSummary> by mutableStateOf(LoadState.Loading)
        private set
    var rename: RenameUi? by mutableStateOf(null)
        private set
    var unlinking: Boolean by mutableStateOf(false)
        private set
    /** Error of the last unlink attempt, shown in the danger card. */
    var unlinkError: String? by mutableStateOf(null)
        private set

    suspend fun refresh() {
        device = LoadState.Loading
        device = try {
            LoadState.Loaded(loadDevice())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            failed(exception)
        }
    }

    fun startRename() {
        val current = (device as? LoadState.Loaded)?.value ?: return
        rename = RenameUi(current.name)
    }

    fun editRename(text: String) {
        rename = rename?.copy(text = text, error = null)
    }

    fun cancelRename() {
        if (rename?.saving != true) rename = null
    }

    suspend fun saveRename() {
        val editing = rename ?: return
        val name = validDeviceName(editing.text) ?: return
        if (editing.saving) return
        rename = editing.copy(saving = true, error = null)
        try {
            val updated = renameDevice(name)
            device = LoadState.Loaded(updated)
            rename = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = exception.toUiError()
            if (error == UiError.NotFound) {
                rename = null
                device = LoadState.Failed(DEVICE_GONE_MESSAGE, notFound = true)
            } else {
                rename = editing.copy(saving = false, error = error.message)
            }
        }
    }

    /** True when the device was unlinked: the screen then goes back to the list. Asking for
     * confirmation first is the screen's job (ConfirmDialog). */
    suspend fun unlink(): Boolean {
        if (unlinking) return false
        unlinking = true
        unlinkError = null
        return try {
            unlinkDevice()
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = exception.toUiError()
            if (error == UiError.NotFound) {
                device = LoadState.Failed(DEVICE_GONE_MESSAGE, notFound = true)
            } else {
                unlinkError = error.message
            }
            false
        } finally {
            unlinking = false
        }
    }

    private fun failed(exception: Exception): LoadState.Failed {
        val error = exception.toUiError()
        return if (error == UiError.NotFound) {
            LoadState.Failed(DEVICE_GONE_MESSAGE, notFound = true)
        } else {
            LoadState.Failed(error.message)
        }
    }
}
