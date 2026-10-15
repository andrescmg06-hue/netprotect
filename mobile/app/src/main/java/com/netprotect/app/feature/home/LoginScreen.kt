package com.netprotect.app.feature.home

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.R
import com.netprotect.app.core.network.UiError
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Inicio de sesión con Google (lámina 01). Solo presentación: recibe datos y lambdas, no toca red ni
 * sesión. La variante (sin Internet, servicio no disponible, «Vuelve a entrar») sale de [loginNotice]. */
@Composable
fun LoginScreen(
    error: String?,
    service: ServiceStatus,
    onSignIn: () -> Unit,
    onRetryService: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val notice = loginNotice(error)
    val expired = notice == LoginNotice.SessionExpired
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
            // SpaceBetween over at least one screen of height keeps the footer at the bottom, and lets it
            // follow the content (instead of covering it) when a large font makes the page scroll.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    LoginPhoto()
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (expired) "Vuelve a entrar" else "Inicia sesión",
                            style = NpText.Display,
                            color = NpColors.Ink,
                            modifier = Modifier.semantics { heading() },
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (expired) {
                                "Por seguridad cerramos tu sesión después de un tiempo. Inicia sesión de nuevo para continuar."
                            } else {
                                "Usa tu cuenta de Google para conectar este teléfono con tu tutor."
                            },
                            style = NpText.Body,
                            color = NpColors.InkMuted,
                        )
                        Spacer(Modifier.height(20.dp))
                        NoticeBanner(notice)
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
                        ServiceStatusLine(service = service, notice = notice, onRetry = onRetryService)
                    }
                }
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(NpIcons.Info),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = NpColors.InkMuted,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Solo puedes iniciar sesión con Google.",
                        style = NpText.BodySm,
                        color = NpColors.InkMuted,
                    )
                }
            }
        }
    }
}

/** The photo of lámina 01, edge to edge, with the navy eyebrow tab on its lower left corner. */
@Composable
private fun LoginPhoto() {
    Box(modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(R.drawable.login_study),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.36f),
        )
        Text(
            text = "NETPROTECT · DISPOSITIVO SUPERVISADO",
            style = NpText.Eyebrow,
            color = NpColors.Paper,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .background(NpColors.Navy)
                .padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun NoticeBanner(notice: LoginNotice) {
    when (notice) {
        LoginNotice.None -> return
        LoginNotice.NoInternet ->
            Banner(UiError.NoInternet.message, NpIcons.WifiOff, NpColors.DangerWash, NpColors.DangerText)
        LoginNotice.ServiceUnavailable ->
            Banner(UiError.ServiceUnavailable.message, NpIcons.CircleAlert, NpColors.DangerWash, NpColors.DangerText)
        LoginNotice.SessionExpired ->
            Banner(UiError.SessionExpired.message, NpIcons.Clock, NpColors.WarningWash, NpColors.WarningText)
        is LoginNotice.Other ->
            Banner(notice.message, NpIcons.CircleAlert, NpColors.DangerWash, NpColors.DangerText)
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun Banner(text: String, @DrawableRes icon: Int, background: Color, content: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, NpShapes.Card)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp),
            tint = content,
        )
        Spacer(Modifier.width(12.dp))
        Text(text = text, style = NpText.BodySm, color = content, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ServiceStatusLine(service: ServiceStatus, notice: LoginNotice, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        if (showsServiceLine(service, notice)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NpColors.Hairline),
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (dotColor, textColor) = when (service) {
                    ServiceStatus.Checking -> NpColors.InkSubtle to NpColors.InkMuted
                    ServiceStatus.Ready -> NpColors.Success to NpColors.SuccessText
                    ServiceStatus.Unavailable -> NpColors.Danger to NpColors.DangerText
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
                    style = NpText.BodySm,
                    color = textColor,
                )
            }
        }
        if (service == ServiceStatus.Unavailable) {
            Spacer(Modifier.height(4.dp))
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
