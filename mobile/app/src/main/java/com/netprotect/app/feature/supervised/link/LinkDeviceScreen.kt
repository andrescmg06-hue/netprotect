package com.netprotect.app.feature.supervised.link

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.SignOutConfirmDialog
import com.netprotect.app.feature.supervised.isCompletePairingCode
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCodeField
import com.netprotect.app.ui.components.NpCodeFieldState
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Pantalla de vinculación del modo supervisado (lámina 02): solo muestra el código y llama a los
 * callbacks. «Cerrar sesión» pide confirmación antes de llamar a [onSignOut]. */
@OptIn(ExperimentalLayoutApi::class)
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
    val phase = linkPhase(linking, error)
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream),
    ) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // At least one screen tall, so the status line sits at the bottom; with a large font it
            // simply follows the content.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Spacer(Modifier.height(16.dp))
                    // FlowRow: at a large font "Cerrar sesión" moves under the eyebrow instead of squeezing it.
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Row(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Image(
                                painter = painterResource(NpIcons.LogoShield),
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(text = "DISPOSITIVO SUPERVISADO", style = NpText.Eyebrow, color = NpColors.InkMuted)
                        }
                        NpButton(
                            text = "Cerrar sesión",
                            onClick = { confirmingSignOut = true },
                            variant = NpButtonVariant.Text,
                            icon = NpIcons.LogOut,
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    PairingTrack(phase)
                    Spacer(Modifier.height(24.dp))
                    Text(text = "PASO 1 DE 1 · VINCULACIÓN", style = NpText.Eyebrow, color = NpColors.InkMuted)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Vincula este dispositivo",
                        style = NpText.Display,
                        color = NpColors.Ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Escribe el código de 6 dígitos que te dio tu tutor. Lo genera en el panel web y caduca pronto.",
                        style = NpText.Body,
                        color = NpColors.InkMuted,
                    )
                    Spacer(Modifier.height(20.dp))
                    NpCodeField(
                        value = code,
                        onValueChange = onCodeChange,
                        state = when (phase) {
                            LinkPhase.Waiting -> NpCodeFieldState.Editing
                            LinkPhase.Linking -> NpCodeFieldState.Sending
                            LinkPhase.Failed -> NpCodeFieldState.Error
                        },
                    )
                    Spacer(Modifier.height(12.dp))
                    when (phase) {
                        LinkPhase.Waiting -> Text(
                            text = "Puedes pegar los 6 dígitos de una vez. El botón se activa con los 6 dígitos.",
                            style = NpText.BodySm,
                            color = NpColors.InkMuted,
                        )
                        LinkPhase.Linking -> Text(
                            text = "No cierres la app.",
                            style = NpText.BodySm,
                            color = NpColors.InkMuted,
                        )
                        LinkPhase.Failed -> ErrorBanner(error.orEmpty())
                    }
                    Spacer(Modifier.height(20.dp))
                    NpButton(
                        text = if (linking) "Vinculando…" else "Vincular dispositivo",
                        onClick = onLink,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (linking) null else NpIcons.Check,
                        enabled = isCompletePairingCode(code),
                        loading = linking,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = NpColors.Ink, fontWeight = FontWeight.Medium)) {
                                append("¿No tienes un código?")
                            }
                            append(" Pídeselo a tu tutor.")
                        },
                        style = NpText.BodySm,
                        color = NpColors.InkMuted,
                    )
                }
                LinkStatusFooter(phase = phase, rechecking = rechecking, onCheckLink = onCheckLink)
            }
        }
    }

    if (confirmingSignOut) {
        SignOutConfirmDialog(
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false },
        )
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NpColors.DangerWash, NpShapes.Card)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(NpIcons.CircleAlert),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = NpColors.DangerText,
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = NpText.BodySm, color = NpColors.DangerText, modifier = Modifier.weight(1f))
    }
}

/** Hairline, the phase's dot + text and, except while the code is being sent, "Comprobar vínculo". */
@Composable
private fun LinkStatusFooter(phase: LinkPhase, rechecking: Boolean, onCheckLink: () -> Unit) {
    Column(modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Hairline),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(if (phase == LinkPhase.Failed) NpColors.DangerText else NpColors.Blue, CircleShape),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = linkStatusLabel(phase), style = NpText.BodySm, color = NpColors.Ink)
        }
        if (phase != LinkPhase.Linking) {
            NpButton(
                text = "Comprobar vínculo",
                onClick = onCheckLink,
                variant = NpButtonVariant.Text,
                icon = NpIcons.RefreshCw,
                loading = rechecking,
            )
        }
    }
}

/** This phone, a dashed line and the tutor (lámina 02). Grey while waiting, blue with a dot while the
 * code is sent, red with a cross when it was refused. Decorative: the status line says the same. */
@Composable
private fun PairingTrack(phase: LinkPhase) {
    val lineColor = when (phase) {
        LinkPhase.Waiting -> NpColors.HairlineStrong
        LinkPhase.Linking -> NpColors.Blue
        LinkPhase.Failed -> NpColors.DangerText
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(NpColors.Paper, NpShapes.Field)
                .border(1.dp, NpColors.Hairline, NpShapes.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(NpIcons.Smartphone),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = NpColors.Ink,
            )
        }
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .padding(horizontal = 10.dp),
        ) {
            val y = size.height / 2f
            val stroke = 1.5.dp.toPx()
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = stroke,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            when (phase) {
                LinkPhase.Waiting -> Unit
                LinkPhase.Linking -> drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(size.width * 0.78f, y))
                LinkPhase.Failed -> drawCross(lineColor, Offset(size.width / 2f, y), 4.dp.toPx(), stroke)
            }
        }
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(NpColors.Navy, NpShapes.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(NpIcons.User),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = NpColors.Paper,
            )
        }
    }
}

private fun DrawScope.drawCross(
    color: Color,
    center: Offset,
    half: Float,
    stroke: Float,
) {
    drawLine(color, Offset(center.x - half, center.y - half), Offset(center.x + half, center.y + half), stroke)
    drawLine(color, Offset(center.x - half, center.y + half), Offset(center.x + half, center.y - half), stroke)
}
