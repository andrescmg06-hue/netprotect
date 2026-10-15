package com.netprotect.app.feature.supervised.welcome

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.privacy.PRIVACY_NO_RECORDING
import com.netprotect.app.feature.supervised.privacy.PrivacyItem
import com.netprotect.app.ui.components.HeroPanelColors
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import java.util.Locale

/** Decorative icons for [welcomeTutorSees], in Privacidad's order; anything added later gets [NpIcons.Info]. */
private val tutorSeesIcons = listOf(NpIcons.Wifi, NpIcons.LayoutGrid, NpIcons.Lock, NpIcons.MapPin, NpIcons.Bell)

/**
 * Bienvenida tras vincular (láminas 13.1 y 13.2), one [step] at a time. It only shows text and answers
 * through the callbacks: [onNext] on the first step, [onBack] (arrow) and [onFinish] on the second. The
 * shell keeps the step, saves the flag and decides where [onFinish] goes; nothing here starts anything.
 */
@Composable
fun WelcomeScreen(
    step: WelcomeStep,
    pendingPermissions: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (step) {
        WelcomeStep.Linked -> LinkedStep(onNext = onNext, modifier = modifier)
        WelcomeStep.TutorCanSee -> TutorCanSeeStep(
            pendingPermissions = pendingPermissions,
            onBack = onBack,
            onFinish = onFinish,
            modifier = modifier,
        )
    }
}

@Composable
private fun LinkedStep(onNext: () -> Unit, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            // Navy behind the status bar too; when the share banner is up the shell consumes this inset.
            .background(NpColors.Navy)
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
            Spacer(Modifier.height(NpSpace.Xl))
            Text(text = WELCOME_EYEBROW.uppercase(Locale.ROOT), style = NpText.Eyebrow, color = HeroPanelColors.Eyebrow)
            Spacer(Modifier.height(NpSpace.Xxl))
            // Only the logo, not the seal: its arcs mean permissions, and none is configured yet.
            Image(
                painter = painterResource(NpIcons.LogoShield),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(96.dp),
            )
            Spacer(Modifier.height(NpSpace.Xxl))
            Text(
                text = WELCOME_TITLE,
                style = NpText.Display,
                color = HeroPanelColors.Headline,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.M))
            Text(text = WELCOME_BODY, style = NpText.Body, color = HeroPanelColors.Summary)
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
            NpButton(
                text = WELCOME_NEXT,
                onClick = onNext,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = NpIcons.ArrowRight,
            )
            Spacer(Modifier.height(NpSpace.M))
            StepDots(
                current = WelcomeStep.Linked,
                active = NpColors.Paper,
                inactive = HeroPanelColors.InactiveArc,
                // The first step has no «Paso 1 de 2» text, so the dots say it to TalkBack.
                description = welcomeStepLabel(WelcomeStep.Linked),
            )
        }
    }
}

@Composable
private fun TutorCanSeeStep(
    pendingPermissions: Int,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NpSpace.Gutter),
        ) {
            TopRow(onBack = onBack)
            Spacer(Modifier.height(NpSpace.S))
            Text(
                text = welcomeStepLabel(WelcomeStep.TutorCanSee).uppercase(Locale.ROOT),
                style = NpText.Eyebrow,
                color = NpColors.InkMuted,
            )
            Spacer(Modifier.height(NpSpace.S))
            Text(
                text = WELCOME_SEE_TITLE,
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.L))
            Rule(NpColors.Ink)
            welcomeTutorSees.forEachIndexed { index, item ->
                if (index > 0) Rule(NpColors.Hairline)
                ItemRow(icon = tutorSeesIcons.getOrElse(index) { NpIcons.Info }, item = item)
            }
            Rule(NpColors.Hairline)
            Spacer(Modifier.height(NpSpace.Xl))
            Text(
                text = WELCOME_DECIDE_HEADING.uppercase(Locale.ROOT),
                style = NpText.Eyebrow,
                color = NpColors.InkMuted,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.M))
            Column(verticalArrangement = Arrangement.spacedBy(NpSpace.L)) {
                welcomeYouDecide.forEach { item ->
                    FactLine(icon = NpIcons.Eye, title = item.title, text = item.body)
                }
                FactLine(icon = NpIcons.Ban, title = null, text = PRIVACY_NO_RECORDING)
            }
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Column(Modifier.fillMaxWidth()) {
            Rule(NpColors.Hairline)
            Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
                // «Paso 2 de 2» is already read above: here the dots are decoration only.
                StepDots(current = WelcomeStep.TutorCanSee, active = NpColors.Ink, inactive = NpColors.HairlineStrong)
                Spacer(Modifier.height(NpSpace.M))
                NpButton(
                    text = welcomeFinishLabel(pendingPermissions),
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = NpIcons.ArrowRight,
                )
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

/** One TalkBack node per item: title and explanation are read together, as in Privacidad. */
@Composable
private fun ItemRow(@DrawableRes icon: Int, item: PrivacyItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { }
            .padding(vertical = NpSpace.L),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .border(1.dp, NpColors.HairlineStrong, NpShapes.Field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painter = painterResource(icon), contentDescription = null, modifier = Modifier.size(22.dp), tint = NpColors.Ink)
        }
        Spacer(Modifier.width(NpSpace.L))
        Column(Modifier.weight(1f)) {
            Text(text = item.title, style = NpText.BodyStrong, color = NpColors.Ink)
            Spacer(Modifier.height(NpSpace.Xs))
            Text(text = item.body, style = NpText.BodySm, color = NpColors.InkMuted)
        }
    }
}

@Composable
private fun FactLine(@DrawableRes icon: Int, title: String?, text: String) {
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp),
            tint = NpColors.BlueText,
        )
        Spacer(Modifier.width(NpSpace.M))
        Column(Modifier.weight(1f)) {
            if (title != null) {
                Text(text = title, style = NpText.BodyStrong, color = NpColors.Ink)
                Spacer(Modifier.height(NpSpace.Xs))
            }
            Text(text = text, style = NpText.Body, color = NpColors.Ink)
        }
    }
}

/** The page dots of láminas 13.1/13.2: one short bar per step, the current one in [active]. */
@Composable
private fun StepDots(current: WelcomeStep, active: Color, inactive: Color, description: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { if (description != null) contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(NpSpace.S, Alignment.CenterHorizontally),
    ) {
        WelcomeStep.entries.forEach { step ->
            Box(
                Modifier
                    .size(width = 22.dp, height = 4.dp)
                    .background(if (step == current) active else inactive, NpShapes.Pill),
            )
        }
    }
}

@Composable
private fun Rule(color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}
