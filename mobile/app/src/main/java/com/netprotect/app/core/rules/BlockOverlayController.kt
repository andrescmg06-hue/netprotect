package com.netprotect.app.core.rules

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.netprotect.app.feature.supervised.BlockScreenContent

/** Draws the block screen as a window overlay (TYPE_APPLICATION_OVERLAY) instead of starting
 * BlockScreenActivity directly.
 *
 * Verified live (17/09/2026, API 36 emulator): a foreground service with no visible Activity
 * cannot open one with a plain startActivity() any more — ActivityTaskManager rejects it as a
 * background activity launch. A full-screen-intent notification (the exemption used for
 * incoming calls) only auto-launches while the device is locked; while unlocked and in active
 * use — the exact moment a block needs to actually happen — it degrades to a heads-up banner the
 * person has to tap, which defeats the point.
 *
 * SYSTEM_ALERT_WINDOW is the one mechanism that is not subject to either restriction: an app
 * holding it can add a window above everything else regardless of lock state or which
 * app/foreground-service is calling, which is exactly why it needs its own special-access grant
 * (OverlayPermission) — same shape as PACKAGE_USAGE_STATS. Where this permission isn't granted,
 * the caller falls back to the notification/Activity path, which still works while locked.
 *
 * A Service has no Activity to host a ComposeView's lifecycle/saved-state/view-model owners, so
 * this supplies a minimal owner of its own — the standard shape for "Compose in a bubble/overlay"
 * outside any Activity.
 */
object BlockOverlayController {
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /** WindowManager and LifecycleRegistry both require the main thread — RuleEnforcementService
     * calls this from its polling coroutine (Dispatchers.Default), which crashed the process the
     * first time this was tried (verified live: IllegalStateException "Method addObserver must
     * be called on the main thread"). Runs synchronously if already on the main thread so callers
     * that are (SupervisedShell, some day) don't pay a post() round-trip for nothing.
     */
    fun show(context: Context, appLabel: String, reason: BlockReason, onGoHome: () -> Unit) {
        runOnMainThread { showOnMainThread(context, appLabel, reason, onGoHome) }
    }

    fun hide(context: Context) {
        runOnMainThread { hideOnMainThread(context) }
    }

    private fun runOnMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun showOnMainThread(context: Context, appLabel: String, reason: BlockReason, onGoHome: () -> Unit) {
        hideOnMainThread(context)

        val owner = OverlayLifecycleOwner()
        owner.create()
        owner.start()
        lifecycleOwner = owner

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    BlockScreenContent(appLabel = appLabel, reason = reason, onGoHome = onGoHome)
                }
            }
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.addView(view, params) }
            .onSuccess { composeView = view }
            .onFailure { owner.destroy(); lifecycleOwner = null }
    }

    private fun hideOnMainThread(context: Context) {
        val view = composeView ?: return
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        runCatching { windowManager.removeView(view) }
        lifecycleOwner?.destroy()
        lifecycleOwner = null
        composeView = null
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    fun create() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun start() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        viewModelStore.clear()
    }
}
