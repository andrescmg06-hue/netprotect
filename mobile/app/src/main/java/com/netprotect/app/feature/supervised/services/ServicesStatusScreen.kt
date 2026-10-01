package com.netprotect.app.feature.supervised.services

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.status.ServiceState
import com.netprotect.app.core.status.ServicesView
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant
import java.time.ZoneId

/** INTERIM (Sprint 50, Claude): plain but complete. DeepSeek replaces this body with the mockup 17
 * design; the signature is final. Everything shown comes from [ServicesView], which never claims
 * "Activo" without evidence. */
@Composable
fun ServicesStatusScreen(
    view: ServicesView,
    now: Instant,
    onStopScreenShare: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Estado de NetProtect", style = NpText.Display, color = NpColors.ShieldNavy)
        Text("Reporte del dispositivo: ${label(view.report)}", style = NpText.Body)
        Text(
            "Último reporte: " + (view.lastReport?.let { RelativeTime.format(it, now, ZoneId.systemDefault()) } ?: "todavía no"),
            style = NpText.Body,
        )
        Text("Control de apps: ${label(view.appControl)}", style = NpText.Body)
        Text("Ubicación: ${label(view.location)}", style = NpText.Body)
        if (view.screenShareActive) {
            Text("Tu tutor está viendo la pantalla con tu consentimiento.", style = NpText.BodyStrong)
            NpButton("Detener", onStopScreenShare)
        }
        NpButton("Volver al inicio", onBack)
    }
}

private fun label(state: ServiceState) = when (state) {
    ServiceState.Active -> "Activo"
    ServiceState.Inactive -> "Inactivo"
    ServiceState.Unconfirmed -> "Sin confirmar"
}
