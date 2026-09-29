package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Tarjeta de permiso: `IconTile` + título + descripción + `StatusPill` ("Concedido"/"Pendiente") y,
 * si no está concedido, botón Primary y acción secundaria opcional. */
@Composable
fun PermissionCard(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    NpCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon = icon, tone = NpTone.Info)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = NpText.BodyStrong,
                        color = NpColors.ShieldNavy,
                        modifier = Modifier.weight(1f),
                    )
                    StatusPill(
                        text = if (granted) "Concedido" else "Pendiente",
                        tone = if (granted) NpTone.Success else NpTone.Warning,
                        showDot = false,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
                if (!granted) {
                    Spacer(Modifier.height(12.dp))
                    NpButton(text = actionLabel, onClick = onAction)
                    if (secondaryLabel != null && onSecondary != null) {
                        Spacer(Modifier.height(4.dp))
                        NpButton(
                            text = secondaryLabel,
                            onClick = onSecondary,
                            variant = NpButtonVariant.Text,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun PermissionCardPreview() {
    NetProtectTheme {
        PermissionCard(
            icon = NpIcons.Eye,
            title = "Acceso a uso de apps",
            description = "Necesario para aplicar reglas de bloqueo.",
            granted = false,
            actionLabel = "Conceder permiso",
            onAction = {},
        )
    }
}
