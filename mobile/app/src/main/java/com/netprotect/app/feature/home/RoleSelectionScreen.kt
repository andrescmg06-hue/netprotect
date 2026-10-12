package com.netprotect.app.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Elección de modo (tutor o supervisado). Solo presentación: recibe datos y lambdas. */
@Composable
fun RoleSelectionScreen(
    displayName: String?,
    email: String,
    error: String?,
    onSelectTutor: () -> Unit,
    onSelectSupervised: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Primera palabra del nombre (recortando espacios); si es null/en blanco, el correo completo.
    val name = displayName?.trim().orEmpty().substringBefore(' ').ifBlank { email }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NpColors.SkyGround, NpColors.SkyGroundEnd))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader(stacked = true)
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Hola, $name",
                style = NpText.Display,
                color = NpColors.ShieldNavy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "¿Cómo vas a usar este dispositivo?",
                style = NpText.Title,
                color = NpColors.SlateMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Selecciona el modo que mejor describa cómo vas a utilizar esta app.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            RoleCard(
                icon = NpIcons.Users,
                tone = NpTone.Info,
                title = "Soy tutor",
                description = "Superviso otros dispositivos desde este teléfono o tablet.",
                variant = NpButtonVariant.Primary,
                onClick = onSelectTutor,
            )
            Spacer(Modifier.height(16.dp))
            RoleCard(
                icon = NpIcons.Smartphone,
                tone = NpTone.Purple,
                title = "Este es el dispositivo supervisado",
                description = "Este teléfono es el que un tutor va a supervisar.",
                variant = NpButtonVariant.Secondary,
                onClick = onSelectSupervised,
            )
            if (error != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = error,
                    style = NpText.Body,
                    color = NpColors.DangerText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                )
            }
            Spacer(Modifier.height(24.dp))
            NpButton(
                text = "Cerrar sesión",
                onClick = onSignOut,
                variant = NpButtonVariant.Text,
                icon = NpIcons.LogOut,
            )
        }
    }
}

@Composable
private fun RoleCard(
    @DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    description: String,
    variant: NpButtonVariant,
    onClick: () -> Unit,
) {
    NpCard {
        Row(verticalAlignment = Alignment.Top) {
            IconTile(icon = icon, tone = tone, size = 56.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NpText.Title, color = NpColors.ShieldNavy)
                Spacer(Modifier.height(4.dp))
                Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        NpButton(
            text = "Elegir",
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            variant = variant,
            trailingIcon = NpIcons.ArrowRight,
        )
    }
}
