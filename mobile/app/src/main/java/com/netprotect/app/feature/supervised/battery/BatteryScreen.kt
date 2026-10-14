package com.netprotect.app.feature.supervised.battery

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/**
 * Aviso de batería (lámina 13.3). [ignoring] is the shell's live value, re-read on every ON_RESUME, so coming
 * back from Settings with the optimization off turns this screen into its «revisada» state by itself. It blocks
 * nothing and only calls the callbacks: the shell opens Settings ([onOpenSettings]) and goes back to Inicio ([onBack]).
 */
@Composable
fun BatteryScreen(
    ignoring: Boolean,
    onOpenSettings: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NpSpace.Gutter),
        ) {
            TopRow(onBack = onBack)
            Spacer(Modifier.height(NpSpace.L))
            IconTile(icon = NpIcons.Battery, tone = if (ignoring) NpTone.Success else NpTone.Warning, size = 64.dp)
            Spacer(Modifier.height(NpSpace.L))
            Text(
                text = if (ignoring) BATTERY_DONE_TITLE else BATTERY_TITLE,
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.M))
            if (ignoring) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, NpColors.SuccessText, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(NpIcons.Check),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = NpColors.SuccessText,
                        )
                    }
                    Spacer(Modifier.width(NpSpace.L))
                    Text(
                        text = BATTERY_DONE_TEXT,
                        style = NpText.Body,
                        color = NpColors.InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Text(text = BATTERY_BODY, style = NpText.Body, color = NpColors.InkMuted)
                Spacer(Modifier.height(NpSpace.Xl))
                SectionHeader(BATTERY_STEPS_HEADING)
                Spacer(Modifier.height(NpSpace.L))
                Column(verticalArrangement = Arrangement.spacedBy(NpSpace.L)) {
                    batterySteps().forEachIndexed { index, step -> StepLine(number = index + 1, text = step) }
                }
                Spacer(Modifier.height(NpSpace.L))
                Text(text = BATTERY_NOTE, style = NpText.BodySm, color = NpColors.InkMuted)
            }
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NpColors.Hairline),
            )
            Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
                if (ignoring) {
                    NpButton(
                        text = BATTERY_BACK_HOME,
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = NpIcons.ArrowLeft,
                    )
                } else {
                    NpButton(
                        text = BATTERY_OPEN_SETTINGS,
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = NpIcons.ExternalLink,
                    )
                    Spacer(Modifier.height(NpSpace.S))
                    NpButton(
                        text = BATTERY_LATER,
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        variant = NpButtonVariant.Secondary,
                    )
                }
            }
        }
    }
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

@Composable
private fun SectionHeader(text: String) {
    Column(Modifier.semantics { heading() }) {
        Text(text = text.uppercase(), style = NpText.Eyebrow, color = NpColors.InkMuted)
        Spacer(Modifier.height(NpSpace.S))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Ink),
        )
    }
}

@Composable
private fun StepLine(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, NpColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = NpText.BodySm, color = NpColors.Ink, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(NpSpace.L))
        Text(
            text = text,
            style = NpText.Body,
            color = NpColors.Ink,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        )
    }
}
