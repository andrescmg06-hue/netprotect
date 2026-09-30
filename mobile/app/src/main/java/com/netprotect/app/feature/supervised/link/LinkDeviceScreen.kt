package com.netprotect.app.feature.supervised.link

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.isCompletePairingCode
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.OtpInput
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Pantalla de vinculación del modo supervisado: solo muestra el código y llama a los callbacks. */
@Composable
fun LinkDeviceScreen(
    code: String,
    onCodeChange: (String) -> Unit,
    linking: Boolean,
    error: String?,
    rechecking: Boolean,
    onLink: () -> Unit,
    onCheckLink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            BrandHeader()
            Spacer(Modifier.height(24.dp))
            Text(text = "Vincular este dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Introduce el código que te dio tu tutor para vincular este dispositivo y comenzar a usar NetProtect.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(20.dp))
            NpCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = NpIcons.KeyRound, tone = NpTone.Info, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Código de vinculación", style = NpText.Title, color = NpColors.ShieldNavy)
                        Text(
                            text = "Ingresa el código de 6 dígitos que te dio tu tutor.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OtpInput(
                    value = code,
                    onValueChange = onCodeChange,
                    isError = error != null,
                    enabled = !linking,
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = error, style = NpText.Body, color = NpColors.DangerText)
                }
                Spacer(Modifier.height(16.dp))
                NpButton(
                    text = "Vincular dispositivo",
                    onClick = onLink,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isCompletePairingCode(code) && !linking,
                    loading = linking,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "¿No tienes un código? Pídeselo a tu tutor.",
                    style = NpText.Body,
                    color = NpColors.SignalBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(16.dp))
            NpCard(containerColor = NpColors.BlueWash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = NpIcons.Clock, tone = NpTone.Neutral, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Esperando código de vinculación",
                            style = NpText.BodyStrong,
                            color = NpColors.ShieldNavy,
                        )
                        Text(
                            text = "La vinculación se comprueba con el servicio de NetProtect.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                NpButton(
                    text = "Comprobar vínculo",
                    onClick = onCheckLink,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.RefreshCw,
                    loading = rechecking,
                )
            }
            Spacer(Modifier.height(24.dp))
            NpButton(
                text = "Cerrar sesión",
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                variant = NpButtonVariant.Text,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
