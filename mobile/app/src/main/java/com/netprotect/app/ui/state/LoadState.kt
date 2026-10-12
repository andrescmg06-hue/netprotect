package com.netprotect.app.ui.state

/** Sprint 44 (D-02): the shape every redesigned data screen uses for what it loads. There is no
 * "loaded but also failed" or "loading with stale data" state on purpose: a screen shows exactly one
 * of these, and a reload starts again from [Loading]. The error is already the Spanish message from
 * `UiError`, never a raw exception. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Loaded<T>(val value: T) : LoadState<T>
    data class Failed(val message: String, val notFound: Boolean = false) : LoadState<Nothing>
}
