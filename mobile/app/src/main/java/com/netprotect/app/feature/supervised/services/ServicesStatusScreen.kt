package com.netprotect.app.feature.supervised.services

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.ui.components.HeroPanelColors
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/**
 * Estado de NetProtect del modo supervisado (lámina 05). Todo lo que muestra viene de [view], ya
 * calculado; «Detener ahora» y «Volver al inicio» solo llaman a los callbacks.
 */
@Composable
fun ServicesStatusScreen(
    view: ServicesView,
    now: Instant,
    onStopScreenShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Only this part scrolls: the footer stays fixed, also at a 200 % font.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Column(Modifier.padding(horizontal = NpSpace.Gutter)) {
                TopRow(onBack = onBack)
                Spacer(Modifier.height(NpSpace.S))
                Text(
                    text = "Estado de NetProtect",
                    style = NpText.Display,
                    color = NpColors.Ink,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(NpSpace.S))
                Text(text = "Qué funciones están activas en este teléfono.", style = NpText.BodySm, color = NpColors.InkMuted)
                Spacer(Modifier.height(NpSpace.L))
            }
            if (view.screenShareActive) {
                ScreenSharePanel(onStopScreenShare = onStopScreenShare)
            }
            Column(Modifier.padding(horizontal = NpSpace.Gutter)) {
                ServiceRow(
                    icon = NpIcons.Wifi,
                    title = "Reporte del dispositivo",
                    state = view.report,
                    description = "Envía el estado del dispositivo a tu tutor.",
                ) {
                    Spacer(Modifier.height(NpSpace.S))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(NpIcons.Clock),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = NpColors.Ink,
                        )
                        Spacer(Modifier.width(NpSpace.S))
                        Text(
                            text = lastReportLabel(view.lastReport, now, ZoneId.systemDefault()),
                            style = NpText.BodySm.copy(fontWeight = FontWeight.SemiBold),
                            color = NpColors.Ink,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Text(text = "Cada minuto mientras NetProtect esté en marcha.", style = NpText.BodySm, color = NpColors.InkMuted)
                }
                Hairline()
                ServiceRow(
                    icon = NpIcons.LayoutGrid,
                    title = "Control de apps",
                    state = view.appControl,
                    description = "Aplica las reglas de apps de tu tutor.",
                )
                Hairline()
                ServiceRow(
                    icon = NpIcons.MapPin,
                    title = "Ubicación aproximada",
                    state = view.location,
                    description = locationDescription(view.location),
                )
                Hairline()
                ServiceRow(
                    icon = NpIcons.Bell,
                    title = "Notificaciones permanentes",
                    state = null,
                    description = "Android muestra un aviso mientras una función está activa.",
                )
                Spacer(Modifier.height(NpSpace.L))
            }
        }
        Footer(onBack = onBack)
    }
}

/** Label of a row's chip; the state is always said in words, never only by colour. */
internal fun serviceStateLabel(state: ServiceState): String = when (state) {
    ServiceState.Active -> "Activo"
    ServiceState.Inactive -> "Inactivo"
    ServiceState.Unconfirmed -> "Sin confirmar"
}

/** Location is sent about every 15 min (LocationReportingService.REPORT_INTERVAL_MS), never "en tiempo real". */
internal fun locationDescription(state: ServiceState): String = when (state) {
    ServiceState.Unconfirmed -> "No hay una ubicación reciente confirmada. Llega a tu tutor aproximadamente cada 15 minutos."
    ServiceState.Active, ServiceState.Inactive -> "Comparte la ubicación aproximada. Llega a tu tutor aproximadamente cada 15 minutos."
}

internal fun lastReportLabel(lastReport: Instant?, now: Instant, zone: ZoneId): String =
    lastReport?.let { "Último reporte: ${RelativeTime.format(it, now, zone)}" } ?: "Último reporte: todavía no"

private fun serviceStateTone(state: ServiceState): NpTone = when (state) {
    ServiceState.Active -> NpTone.Success
    ServiceState.Inactive -> NpTone.Neutral
    ServiceState.Unconfirmed -> NpTone.Warning
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(painter = painterResource(NpIcons.ArrowLeft), contentDescription = "Atrás", tint = NpColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        Image(painter = painterResource(NpIcons.LogoShield), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

/** Navy, full-width panel shown only while a screen share really runs (lámina 05, estado 2). */
@Composable
private fun ScreenSharePanel(onStopScreenShare: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NpColors.Navy)
            .padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(NpColors.Paper.copy(alpha = 0.08f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(NpIcons.Monitor),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = NpColors.Paper,
                )
            }
            Spacer(Modifier.width(NpSpace.M))
            Column(Modifier.weight(1f)) {
                Text(text = "VISUALIZACIÓN DE PANTALLA", style = NpText.Eyebrow, color = HeroPanelColors.Eyebrow)
                Text(text = "En curso", style = NpText.Subtitle, color = NpColors.Paper)
            }
        }
        Spacer(Modifier.height(NpSpace.M))
        Text(text = "Tu tutor está viendo tu pantalla ahora.", style = NpText.Body, color = HeroPanelColors.Summary)
        Spacer(Modifier.height(NpSpace.L))
        NpButton(
            text = "Detener ahora",
            onClick = onStopScreenShare,
            modifier = Modifier.fillMaxWidth(),
            variant = NpButtonVariant.Secondary,
            icon = NpIcons.CircleStop,
        )
    }
}

/** One function of the list: icon tile, title with its chip (the chip drops under it at a large font). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ServiceRow(
    @DrawableRes icon: Int,
    title: String,
    state: ServiceState?,
    description: String,
    extra: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = NpSpace.L),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(NpColors.Stone, NpShapes.Field)
                .border(1.dp, NpColors.Hairline, NpShapes.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = NpColors.Ink)
        }
        Spacer(Modifier.width(NpSpace.L))
        Column(Modifier.weight(1f)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(NpSpace.Xs),
            ) {
                Text(
                    text = title,
                    style = NpText.Subtitle,
                    color = NpColors.Ink,
                    modifier = Modifier.padding(end = NpSpace.S),
                )
                if (state != null) {
                    StatusPill(
                        text = serviceStateLabel(state),
                        tone = serviceStateTone(state),
                        showDot = state != ServiceState.Unconfirmed,
                    )
                }
            }
            Spacer(Modifier.height(NpSpace.Xs))
            Text(text = description, style = NpText.BodySm, color = NpColors.InkMuted)
            extra?.invoke()
        }
    }
}

@Composable
private fun Footer(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
            // Not "ni controla el dispositivo": NetProtect does block apps (PLAN §2.1).
            Text(
                text = "NetProtect no graba ni guarda lo que se ve en tu pantalla.",
                style = NpText.BodySm,
                color = NpColors.InkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(NpSpace.M))
            NpButton(
                text = "Volver al inicio",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = NpIcons.Home,
            )
        }
    }
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NpColors.Hairline),
    )
}
