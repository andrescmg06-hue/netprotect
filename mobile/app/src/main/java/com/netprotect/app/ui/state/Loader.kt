package com.netprotect.app.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import kotlin.coroutines.cancellation.CancellationException

/** Sprint 45 (D-02): the state holder of a read-only screen — one load, reloadable, errors already
 * in Spanish, a 404 flagged as `notFound`. */
class Loader<T>(private val load: suspend () -> T) {
    var state: LoadState<T> by mutableStateOf(LoadState.Loading)
        private set

    suspend fun refresh() {
        state = LoadState.Loading
        state = try {
            LoadState.Loaded(load())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = exception.toUiError()
            LoadState.Failed(error.message, notFound = error == UiError.NotFound)
        }
    }
}
