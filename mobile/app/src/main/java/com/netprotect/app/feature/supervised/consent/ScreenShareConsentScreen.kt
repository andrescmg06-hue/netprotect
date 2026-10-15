package com.netprotect.app.feature.supervised.consent

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCountdownBar
import com.netprotect.app.ui.components.NpStepIndicator
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

/** The three steps of lámina 06; the third is Android's own dialog, which has no screen here. */
private val consentSteps = listOf("1 · Leer", "2 · Autorizar", "3 · Android")

/**
 * Consentimiento de vista remota del modo supervisado (lámina 06, estados 1 y 2). Solo muestra la petición
 * y responde por los callbacks; el shell envía la respuesta y abre el diálogo de Android.
 *
 * [requestedAt] is when the pending request reached the phone; with it the screen shows the countdown
 * and its draining bar, computed against [now]. It is null once the request was answered («Continuar»
 * leaves this screen behind Android's dialog) and then no countdown is shown. [onBack] draws the back
 * arrow; it must mean the same as the system Back (not an answer).
 */
@Composable
fun ScreenShareConsentScreen(
    onContinue: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    requestedAt: Instant? = null,
    now: Instant = Instant.now(),
    onBack: (() -> Unit)? = null,
) {
    // Keyed on the request: a new one arriving while this screen is still shown starts unchecked.
    var authorized by rememberSaveable(requestedAt) { mutableStateOf(false) }
    ScreenShareConsentContent(
        authorized = authorized,
        onAuthorizedChange = { authorized = it },
        onContinue = onContinue,
        onDecline = onDecline,
        modifier = modifier,
        requestedAt = requestedAt,
        now = now,
        onBack = onBack,
    )
}

/** Stateless body of [ScreenShareConsentScreen], so the gallery can show the authorized state. */
@Composable
internal fun ScreenShareConsentContent(
    authorized: Boolean,
    onAuthorizedChange: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
    requestedAt: Instant? = null,
    now: Instant = Instant.now(),
    onBack: (() -> Unit)? = null,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NpSpace.Gutter),
        ) {
            TopRow(onBack = onBack)
            Spacer(Modifier.height(NpSpace.S))
            NpStepIndicator(labels = consentSteps, filledCount = 2)
            Spacer(Modifier.height(NpSpace.Xl))
            Text(text = "SOLICITUD DE TU TUTOR", style = NpText.Eyebrow, color = NpColors.InkMuted)
            Spacer(Modifier.height(NpSpace.S))
            Text(
                text = "Quiere ver tu pantalla",
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.S))
            Text(text = "Solo se mostrará mientras tú lo permitas.", style = NpText.Body, color = NpColors.InkMuted)
            Spacer(Modifier.height(NpSpace.Xl))
            Column(verticalArrangement = Arrangement.spacedBy(NpSpace.L)) {
                FactLine(icon = NpIcons.Eye, text = "Tu tutor verá en vivo lo que haces en este teléfono.")
                // Not "ni controla el dispositivo": NetProtect does block apps (PLAN §2.1).
                FactLine(icon = NpIcons.Ban, text = "NetProtect no graba ni guarda lo que se ve en tu pantalla.")
                FactLine(
                    icon = NpIcons.CircleStop,
                    text = "Puedes detenerlo en cualquier momento desde “Estado de NetProtect”.",
                )
            }
            Spacer(Modifier.height(NpSpace.Xl))
            Hairline()
            AuthorizeRow(authorized = authorized, onAuthorizedChange = onAuthorizedChange)
            Hairline()
            if (requestedAt != null) {
                Spacer(Modifier.height(NpSpace.L))
                Countdown(requestedAt = requestedAt, now = now)
            }
            Spacer(Modifier.height(NpSpace.L))
            Text(
                text = "Después, Android mostrará su propio aviso de captura de pantalla. Atrás no cuenta como aceptar.",
                style = NpText.BodySm,
                color = NpColors.InkMuted,
            )
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Footer(authorized = authorized, onContinue = onContinue, onDecline = onDecline)
    }
}

@Composable
private fun TopRow(onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(painter = painterResource(NpIcons.ArrowLeft), contentDescription = "Atrás", tint = NpColors.Ink)
            }
        }
        Spacer(Modifier.weight(1f))
        Image(painter = painterResource(NpIcons.LogoShield), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun FactLine(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            tint = NpColors.BlueText,
        )
        Spacer(Modifier.width(NpSpace.M))
        Text(text = text, style = NpText.Body, color = NpColors.Ink, modifier = Modifier.weight(1f))
    }
}

/** The whole row toggles (one TalkBack node with the checkbox role), not only the square. */
@Composable
private fun AuthorizeRow(authorized: Boolean, onAuthorizedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = authorized, role = Role.Checkbox, onValueChange = onAuthorizedChange)
            .padding(vertical = NpSpace.L),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (authorized) {
                        Modifier.background(NpColors.Blue, NpShapes.Field)
                    } else {
                        Modifier
                            .background(NpColors.Paper, NpShapes.Field)
                            .border(1.5.dp, NpColors.Ink, NpShapes.Field)
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (authorized) {
                Icon(
                    painter = painterResource(NpIcons.Check),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NpColors.Paper,
                )
            }
        }
        Spacer(Modifier.width(NpSpace.L))
        Text(
            text = "Autorizo a mi tutor a ver la pantalla de este dispositivo ahora",
            style = NpText.BodyStrong,
            color = NpColors.Ink,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Countdown(requestedAt: Instant, now: Instant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(NpIcons.Clock),
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = NpColors.InkMuted,
        )
        Spacer(Modifier.width(NpSpace.S))
        Text(
            text = consentCountdownText(requestedAt, now),
            style = NpText.BodySm,
            color = NpColors.InkMuted,
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(Modifier.height(NpSpace.S))
    NpCountdownBar(progress = consentProgress(requestedAt, now))
}

@Composable
private fun Footer(authorized: Boolean, onContinue: () -> Unit, onDecline: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Hairline()
        Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
            if (!authorized) {
                Text(
                    text = "Marca la casilla para continuar.",
                    style = NpText.BodySm,
                    color = NpColors.InkMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(NpSpace.M))
            }
            NpButton(
                text = "Continuar",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = authorized,
                trailingIcon = NpIcons.ArrowRight,
            )
            Spacer(Modifier.height(NpSpace.S))
            NpButton(
                text = "Ahora no",
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth(),
                variant = NpButtonVariant.Secondary,
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
