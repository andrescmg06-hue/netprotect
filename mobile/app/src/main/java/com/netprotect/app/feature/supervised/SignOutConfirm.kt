package com.netprotect.app.feature.supervised

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Test tag of the dialog's confirm button: its label repeats the "Cerrar sesión" that opened it. */
const val SIGN_OUT_CONFIRM_TAG = "signOutConfirm"

/**
 * Sprint 63 (lámina 10, «¿Cerrar sesión?»): signing out turns the protection off, so where the
 * supervised person can tap it with the protection on (Vincular, «Más ajustes») it asks first.
 * Cancelling only closes the dialog. Signing out never unlinks the device.
 */
@Composable
fun SignOutConfirmDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        SignOutConfirmContent(onConfirm = onConfirm, onCancel = onDismiss)
    }
}

/** The dialog's card on its own, so the debug gallery can show it without a modal window. The two
 * buttons are stacked at full width: side by side they would be squeezed at a 200 % font. */
@Composable
fun SignOutConfirmContent(onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = NpShapes.Card,
        color = NpColors.Paper,
        border = BorderStroke(1.dp, NpColors.Hairline),
    ) {
        // Scrolls if a 200 % font makes the card taller than the window.
        Column(modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text(
                text = "¿Cerrar sesión?",
                style = NpText.Title,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Saldrás de tu cuenta en este teléfono. Para volver a entrar tendrás que iniciar sesión otra vez.",
                style = NpText.BodySm,
                color = NpColors.InkMuted,
            )
            Spacer(Modifier.height(20.dp))
            NpButton(
                text = "Cerrar sesión",
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SIGN_OUT_CONFIRM_TAG),
                small = true,
            )
            Spacer(Modifier.height(8.dp))
            NpButton(
                text = "Cancelar",
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
                variant = NpButtonVariant.Secondary,
                small = true,
            )
        }
    }
}
