package com.netprotect.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Diálogo de confirmación: tarjeta blanca de radio 4 dp con borde; icono `TriangleAlert` cuando `danger`;
 * el botón de confirmar es `Danger` si `danger`, si no `Primary`. Cerrar tocando fuera = `onDismiss`. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = NpShapes.Card,
            color = NpColors.Paper,
            border = BorderStroke(1.dp, NpColors.Hairline),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                if (danger) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(NpColors.DangerWash, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(NpIcons.TriangleAlert),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = NpColors.Danger,
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(text = title, style = NpText.Subtitle, color = NpColors.Ink)
                Spacer(Modifier.height(8.dp))
                Text(text = message, style = NpText.Body, color = NpColors.InkMuted)
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    NpButton(text = dismissLabel, onClick = onDismiss, variant = NpButtonVariant.Secondary)
                    Spacer(Modifier.width(8.dp))
                    NpButton(
                        text = confirmLabel,
                        onClick = onConfirm,
                        variant = if (danger) NpButtonVariant.Danger else NpButtonVariant.Primary,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun ConfirmDialogPreview() {
    NetProtectTheme {
        ConfirmDialog(
            title = "Desvincular dispositivo",
            message = "¿Seguro que quieres desvincularlo?",
            confirmLabel = "Desvincular",
            dismissLabel = "Cancelar",
            onConfirm = {},
            onDismiss = {},
            danger = true,
        )
    }
}
