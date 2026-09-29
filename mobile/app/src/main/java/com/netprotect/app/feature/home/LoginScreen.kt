package com.netprotect.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Inicio de sesión con Google. Solo presentación: recibe datos y lambdas, no toca red ni sesión. */
@Composable
fun LoginScreen(
    error: String?,
    service: ServiceStatus,
    onSignIn: () -> Unit,
    onRetryService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NpColors.SkyGround, NpColors.SkyGroundEnd))),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHeader()
            Spacer(Modifier.height(32.dp))
            Text(
                text = "Control parental",
                style = NpText.Display,
                color = NpColors.ShieldNavy,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Inicia sesión con tu cuenta de Google para continuar como tutor o como dispositivo supervisado.",
                style = NpText.BodyLead,
                color = NpColors.SlateMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            NpCard {
                if (error != null) {
                    Text(
                        text = error,
                        style = NpText.Body,
                        color = NpColors.DangerText,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                NpButton(
                    text = "Iniciar sesión con Google",
                    onClick = onSignIn,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.GoogleG,
                    tintIcon = false,
                    trailingIcon = NpIcons.ArrowRight,
                )
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(NpColors.Hairline),
                )
                Spacer(Modifier.height(16.dp))
                ServiceStatusLine(service = service, onRetry = onRetryService)
            }
        }

        // Pie fijo fuera del bloque centrado: alineado abajo, con su propio margen inferior.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(NpIcons.Info),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = NpColors.SlateMuted,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Solo puedes iniciar sesión con Google.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
        }
    }
}

@Composable
private fun ServiceStatusLine(service: ServiceStatus, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val dotColor = when (service) {
                ServiceStatus.Checking -> NpColors.SlateSubtle
                ServiceStatus.Ready -> NpColors.Success
                ServiceStatus.Unavailable -> NpColors.Danger
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(dotColor, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = when (service) {
                    ServiceStatus.Checking -> "Comprobando servicio…"
                    ServiceStatus.Ready -> "Servicio listo"
                    ServiceStatus.Unavailable -> "Servicio no disponible"
                },
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
        }
        if (service == ServiceStatus.Unavailable) {
            Spacer(Modifier.height(8.dp))
            NpButton(
                text = "Reintentar",
                onClick = onRetry,
                variant = NpButtonVariant.Text,
                icon = NpIcons.RefreshCw,
                small = true,
            )
        }
    }
}
