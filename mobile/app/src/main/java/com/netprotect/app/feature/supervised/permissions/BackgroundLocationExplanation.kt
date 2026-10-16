package com.netprotect.app.feature.supervised.permissions

import androidx.compose.runtime.Composable
import com.netprotect.app.ui.components.ConfirmDialog

/**
 * Sprint 73 (T6): what the supervised person reads BEFORE Android's "Permitir todo el tiempo" step. From Android 11
 * that permission is not a dialog: the system sends the person to Settings, so the app explains first what is shared
 * and what is not (the texts are in PermissionTexts.kt). The reusable [ConfirmDialog], not a new design. «Ahora no»
 * only closes it: location keeps working as before and the permission stays pending in Permisos.
 */
@Composable
fun BackgroundLocationExplanationDialog(onContinue: () -> Unit, onDismiss: () -> Unit) {
    ConfirmDialog(
        title = BACKGROUND_LOCATION_EXPLANATION_TITLE,
        message = BACKGROUND_LOCATION_EXPLANATION_BODY,
        confirmLabel = BACKGROUND_LOCATION_EXPLANATION_CONTINUE,
        dismissLabel = BACKGROUND_LOCATION_EXPLANATION_DISMISS,
        onConfirm = onContinue,
        onDismiss = onDismiss,
    )
}
