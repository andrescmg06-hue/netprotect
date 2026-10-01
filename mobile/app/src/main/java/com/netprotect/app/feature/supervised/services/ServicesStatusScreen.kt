package com.netprotect.app.feature.supervised.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Estado de NetProtect del modo supervisado. Todo lo que muestra viene de [view], ya calculado. */
@Composable
fun ServicesStatusScreen(
    view: ServicesView,
    now: Instant,
    onStopScreenShare: () -> Unit,
    onBack: () -> Unit,
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
            NpTopBar(onBack = onBack)
            Text(text = "Estado de NetProtect", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Aquí puedes ver qué funciones de NetProtect están activas en este dispositivo.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ServiceCard(
                    icon = NpIcons.Wifi,
                    tone = NpTone.Success,
                    title = "Reporte del dispositivo",
                    description = "Envía el estado de este dispositivo a tu tutor.",
                    pill = { ServiceStatusPill(view.report) },
                    extra = {
                        InfoBox(
                            icon = NpIcons.Clock,
                            line = view.lastReport?.let { "Último reporte: ${RelativeTime.format(it, now, ZoneId.systemDefault())}" }
                                ?: "Último reporte: todavía no",
                            note = "El dispositivo envía su estado cada minuto mientras la app esté abierta.",
                        )
                    },
                )
                ServiceCard(
                    icon = NpIcons.LayoutGrid,
                    tone = NpTone.Purple,
                    title = "Control de apps",
                    description = "Aplica las reglas de apps de tu tutor.",
                    pill = { ServiceStatusPill(view.appControl) },
                )
                ServiceCard(
                    icon = NpIcons.MapPin,
                    tone = NpTone.Info,
                    title = "Ubicación aproximada",
                    description = "Comparte la ubicación aproximada de este dispositivo.",
                    pill = { ServiceStatusPill(view.location) },
                )
                if (view.screenShareActive) {
                    ServiceCard(
                        icon = NpIcons.Monitor,
                        tone = NpTone.Info,
                        title = "Visualización de pantalla",
                        description = "Tu tutor está viendo la pantalla con tu consentimiento.",
                        pill = { StatusPill(text = "En curso", tone = NpTone.Info) },
                        extra = {
                            ScreenShareBox(
                                line = view.screenShareSince?.let { "Empezó ${RelativeTime.format(it, now, ZoneId.systemDefault())}." },
                                note = "Se detiene cuando tu tutor la finaliza o cuando tú la detienes.",
                                onStopScreenShare = onStopScreenShare,
                            )
                        },
                    )
                }
                ServiceCard(
                    icon = NpIcons.Bell,
                    tone = NpTone.Info,
                    title = "Notificaciones permanentes",
                    description = "Mientras una función está activa, Android muestra una notificación para que sepas qué está haciendo NetProtect:",
                    extra = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "“NetProtect está activo” — control de apps.", style = NpText.Body, color = NpColors.Ink)
                            Text(text = "“NetProtect comparte la ubicación” — ubicación.", style = NpText.Body, color = NpColors.Ink)
                            Text(text = "“Tu tutor está viendo esta pantalla” — vista remota.", style = NpText.Body, color = NpColors.Ink)
                        }
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "NetProtect no graba la pantalla ni controla el dispositivo. La visualización de pantalla requiere tu consentimiento cada vez.",
            )
            Spacer(Modifier.height(16.dp))
            NpButton(
                text = "Volver al inicio",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                icon = NpIcons.Home,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ServiceStatusPill(state: ServiceState) {
    val (text, tone) = when (state) {
        ServiceState.Active -> "Activo" to NpTone.Success
        ServiceState.Inactive -> "Inactivo" to NpTone.Neutral
        ServiceState.Unconfirmed -> "Sin confirmar" to NpTone.Warning
    }
    StatusPill(text = text, tone = tone)
}

@Composable
private fun ServiceCard(
    @androidx.annotation.DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    description: String,
    pill: (@Composable () -> Unit)? = null,
    extra: (@Composable () -> Unit)? = null,
) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = icon, tone = tone, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NpText.Title, color = NpColors.ShieldNavy)
                Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
            }
            if (pill != null) {
                Spacer(Modifier.width(12.dp))
                pill()
            }
        }
        if (extra != null) {
            Spacer(Modifier.height(12.dp))
            extra()
        }
    }
}

@Composable
private fun InfoBox(
    @androidx.annotation.DrawableRes icon: Int,
    line: String,
    note: String,
) {
    NpCard(containerColor = NpColors.BlueWash, borderColor = NpColors.BlueWash) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = NpColors.SignalBlue,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = line, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
                Text(text = note, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
    }
}

@Composable
private fun ScreenShareBox(
    line: String?,
    note: String,
    onStopScreenShare: () -> Unit,
) {
    NpCard(containerColor = NpColors.BlueWash, borderColor = NpColors.BlueWash) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(NpIcons.Eye),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = NpColors.SignalBlue,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (line != null) {
                    Text(text = line, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
                }
                Text(text = note, style = NpText.Body, color = NpColors.SlateMuted)
                Spacer(Modifier.height(12.dp))
                NpButton(
                    text = "Detener",
                    onClick = onStopScreenShare,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.EyeOff,
                )
            }
        }
    }
}
