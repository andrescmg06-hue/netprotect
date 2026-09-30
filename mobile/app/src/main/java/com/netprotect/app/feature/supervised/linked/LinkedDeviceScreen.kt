package com.netprotect.app.feature.supervised.linked

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.pendingPermissionsLabel
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.ListRow
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Pantalla «Dispositivo vinculado» del modo supervisado. Solo lectura: llama a los callbacks. */
@Composable
fun LinkedDeviceScreen(
    deviceName: String?,
    androidVersion: String,
    tutors: List<String>,
    lastContact: Instant?,
    reachable: Boolean,
    now: Instant,
    pendingPermissions: Int,
    screenShareRequested: Boolean,
    onAcceptScreenShare: () -> Unit,
    onDeclineScreenShare: () -> Unit,
    onOpenPermissions: () -> Unit,
    onSwitchMode: () -> Unit,
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
            BrandHeader(subtitle = "Dispositivo supervisado")
            Spacer(Modifier.height(24.dp))
            Text(text = "Dispositivo vinculado", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Este dispositivo está vinculado a un tutor y ya forma parte de NetProtect.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(20.dp))
            if (screenShareRequested) {
                NpCard(containerColor = NpColors.BlueWash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconTile(icon = NpIcons.Monitor, tone = NpTone.Info, size = 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Tu tutor quiere ver esta pantalla",
                            style = NpText.Title,
                            color = NpColors.ShieldNavy,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Si aceptas, Android te pedirá confirmarlo otra vez y verás un aviso permanente mientras dure la transmisión. Puedes detenerla en cualquier momento desde ese aviso.",
                        style = NpText.Body,
                        color = NpColors.Ink,
                    )
                    Spacer(Modifier.height(12.dp))
                    NpButton(
                        text = "Aceptar",
                        onClick = onAcceptScreenShare,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    NpButton(
                        text = "Ahora no",
                        onClick = onDeclineScreenShare,
                        modifier = Modifier.fillMaxWidth(),
                        variant = NpButtonVariant.Secondary,
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
            NpCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = NpIcons.Smartphone, tone = NpTone.Info, size = 72.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = deviceName ?: "Este dispositivo",
                            style = NpText.Title,
                            color = NpColors.ShieldNavy,
                        )
                        Text(text = "Android $androidVersion", style = NpText.Body, color = NpColors.SlateMuted)
                        Spacer(Modifier.height(8.dp))
                        StatusPill(text = "Vinculado", tone = NpTone.Success)
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = NpColors.Hairline)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(NpIcons.User),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = NpColors.SlateMuted,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Supervisado por",
                            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                            color = NpColors.SlateMuted,
                        )
                        tutors.forEach { tutor ->
                            Text(text = tutor, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = NpColors.Hairline)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(NpIcons.Clock),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = NpColors.SlateMuted,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Última comunicación",
                            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                            color = NpColors.SlateMuted,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = lastContact
                                    ?.let { RelativeTime.format(it, now, ZoneId.systemDefault()).replaceFirstChar { c -> c.uppercase() } }
                                    ?: "Todavía no",
                                style = NpText.Body,
                                color = NpColors.Ink,
                            )
                            Text(text = " · ", style = NpText.Body, color = NpColors.Ink)
                            Text(
                                text = if (reachable) "Conectado" else "Sin conexión",
                                style = NpText.Body,
                                color = if (reachable) NpColors.SuccessText else NpColors.DangerText,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            InfoBanner(
                title = "Información importante",
                text = "Este dispositivo reporta su estado cada minuto mientras la app esté abierta.",
            )
            Spacer(Modifier.height(12.dp))
            val permissionTone = if (pendingPermissions > 0) NpTone.Warning else NpTone.Success
            NpCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = NpIcons.Shield, tone = permissionTone, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Permisos del dispositivo", style = NpText.Title, color = NpColors.ShieldNavy)
                        Text(
                            text = "Para que NetProtect funcione correctamente, se requieren algunos permisos.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                NpCard(containerColor = permissionTone.wash, borderColor = permissionTone.wash) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(if (pendingPermissions > 0) NpIcons.CircleAlert else NpIcons.CircleCheck),
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = permissionTone.text,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = pendingPermissionsLabel(pendingPermissions),
                                style = NpText.BodyStrong,
                                color = permissionTone.text,
                            )
                            if (pendingPermissions > 0) {
                                Text(
                                    text = "Revisa y concede los permisos necesarios para completar la configuración.",
                                    style = NpText.Body,
                                    color = NpColors.SlateMuted,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                NpButton(
                    text = "Revisar permisos",
                    onClick = onOpenPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    icon = NpIcons.Settings,
                )
            }
            Spacer(Modifier.height(12.dp))
            NpCard(onClick = onSwitchMode) {
                ListRow(
                    title = "Cambiar de modo",
                    subtitle = "Salir del modo supervisado en este dispositivo.",
                    leading = { IconTile(icon = NpIcons.ArrowLeftRight, tone = NpTone.Info, size = 44.dp) },
                    trailing = { Chevron() },
                )
            }
            Spacer(Modifier.height(12.dp))
            NpCard(onClick = onSignOut) {
                ListRow(
                    title = "Cerrar sesión",
                    subtitle = "Cierra la sesión en este dispositivo.",
                    leading = { IconTile(icon = NpIcons.LogOut, tone = NpTone.Danger, size = 44.dp) },
                    trailing = { Chevron() },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        painter = painterResource(NpIcons.ChevronRight),
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = NpColors.SlateMuted,
    )
}
