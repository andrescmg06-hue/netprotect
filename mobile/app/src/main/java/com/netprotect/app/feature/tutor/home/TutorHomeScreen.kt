package com.netprotect.app.feature.tutor.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.devices.DeviceList
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.CodeDisplay
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.SectionHeader
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant

/** Inicio del tutor: cabecera, tarjeta de vinculación, lista de dispositivos y accesos. */
@Composable
fun TutorHomeScreen(
    userName: String?,
    userEmail: String,
    pairing: PairingUi,
    devices: LoadState<List<DeviceSummary>>,
    now: Instant,
    onGenerateCode: () -> Unit,
    onRevokeCode: () -> Unit,
    onDismissPairing: () -> Unit,
    onRefreshDevices: () -> Unit,
    onOpenDevice: (String) -> Unit,
    onOpenActivity: () -> Unit,
    onSwitchMode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accountName = userName?.trim()?.takeIf { it.isNotBlank() } ?: userEmail
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandHeader(subtitle = "Panel del tutor", modifier = Modifier.weight(1f))
                Avatar(accountName = accountName)
            }
            Spacer(Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Modo Tutor",
                    style = NpText.Display,
                    color = NpColors.ShieldNavy,
                    modifier = Modifier.weight(1f),
                )
                NpButton(
                    text = "Cambiar de modo",
                    onClick = onSwitchMode,
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.ArrowLeftRight,
                    small = true,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Supervisa y gestiona los dispositivos vinculados.",
                style = NpText.BodyLead,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(20.dp))
            PairingCard(
                pairing = pairing,
                onGenerateCode = onGenerateCode,
                onRevokeCode = onRevokeCode,
                onDismissPairing = onDismissPairing,
                onRefreshDevices = onRefreshDevices,
            )
            Spacer(Modifier.height(24.dp))
            SectionHeader(
                title = "Dispositivos vinculados",
                actionLabel = "Actualizar",
                actionIcon = NpIcons.RefreshCw,
                onAction = onRefreshDevices,
            )
            Spacer(Modifier.height(8.dp))
            DeviceList(
                devices = devices,
                now = now,
                onOpenDevice = onOpenDevice,
                onRetry = onRefreshDevices,
            )
            Spacer(Modifier.height(16.dp))
            ActivityCard(onOpenActivity = onOpenActivity)
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Sobre la información de los dispositivos",
                text = "La ubicación, el uso de apps y las geocercas se sincronizan periódicamente y pueden tener un retraso de algunos minutos.",
            )
            Spacer(Modifier.height(16.dp))
            NpButton(
                text = "Cerrar sesión",
                onClick = onSignOut,
                variant = NpButtonVariant.Danger,
                icon = NpIcons.LogOut,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Avatar(accountName: String) {
    val initial = accountName.firstOrNull()?.uppercase() ?: "?"
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(NpColors.BlueWash, CircleShape)
            .semantics { contentDescription = "Cuenta: $accountName" },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = initial, style = NpText.Title, color = NpColors.ShieldNavy)
    }
}

@Composable
private fun PairingCard(
    pairing: PairingUi,
    onGenerateCode: () -> Unit,
    onRevokeCode: () -> Unit,
    onDismissPairing: () -> Unit,
    onRefreshDevices: () -> Unit,
) {
    NpCard {
        when (pairing) {
            PairingUi.Idle, PairingUi.Generating -> {
                Row(verticalAlignment = Alignment.Top) {
                    IconTile(icon = NpIcons.Link, tone = NpTone.Info, size = 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Vincula un dispositivo nuevo", style = NpText.Title, color = NpColors.ShieldNavy)
                        Text(
                            text = "Genera un código temporal para vincular un teléfono o tablet supervisado.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                NpButton(
                    text = "Generar código de vinculación",
                    onClick = onGenerateCode,
                    modifier = Modifier.fillMaxWidth(),
                    icon = NpIcons.QrCode,
                    loading = pairing == PairingUi.Generating,
                )
            }
            is PairingUi.Active -> {
                Text(text = "Código de vinculación", style = NpText.Label, color = NpColors.SlateMuted)
                Spacer(Modifier.height(8.dp))
                CodeDisplay(code = pairing.code, remainingSeconds = pairing.remainingSeconds, totalSeconds = pairing.totalSeconds)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Uso único. Introdúcelo en el teléfono que vas a supervisar.",
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
                Spacer(Modifier.height(16.dp))
                NpButton(
                    text = "Revocar",
                    onClick = onRevokeCode,
                    variant = NpButtonVariant.Danger,
                    loading = pairing.revoking,
                )
            }
            PairingUi.Expired -> {
                Text(text = "El código venció. Genera uno nuevo.", style = NpText.Body, color = NpColors.DangerText)
                Spacer(Modifier.height(12.dp))
                NpButton(
                    text = "Generar código de vinculación",
                    onClick = onGenerateCode,
                    modifier = Modifier.fillMaxWidth(),
                    icon = NpIcons.QrCode,
                )
            }
            is PairingUi.Failed -> {
                Text(
                    text = pairing.message,
                    style = NpText.Body,
                    color = NpColors.DangerText,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
                Spacer(Modifier.height(12.dp))
                NpButton(text = "Entendido", onClick = onDismissPairing, variant = NpButtonVariant.Secondary)
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Hairline),
        )
        Spacer(Modifier.height(16.dp))
        NpButton(
            text = "¿Ya se vinculó? Actualizar lista",
            onClick = onRefreshDevices,
            modifier = Modifier.fillMaxWidth(),
            variant = NpButtonVariant.Text,
            icon = NpIcons.RefreshCw,
        )
    }
}

@Composable
private fun ActivityCard(onOpenActivity: () -> Unit) {
    NpCard(onClick = onOpenActivity) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = NpIcons.FileText, tone = NpTone.Purple, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Mi actividad (auditoría)", style = NpText.Title, color = NpColors.ShieldNavy)
                Text(
                    text = "Revisa las acciones realizadas en tu cuenta.",
                    style = NpText.Body,
                    color = NpColors.SlateMuted,
                )
            }
            Icon(
                painter = painterResource(NpIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
        }
    }
}
