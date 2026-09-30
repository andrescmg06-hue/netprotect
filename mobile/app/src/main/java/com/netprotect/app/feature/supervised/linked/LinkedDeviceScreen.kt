package com.netprotect.app.feature.supervised.linked

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
import com.netprotect.app.feature.supervised.pendingPermissionsLabel
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant
import java.time.ZoneId

/** INTERIM (Sprint 48, Claude): plain but complete — including the Sprint 23 screen-share consent,
 * which must keep working until Sprint 50. DeepSeek replaces this body; the signature is final. */
@Composable
fun LinkedDeviceScreen(
    deviceName: String?,
    androidVersion: String,
    tutors: List<String>,
    lastContact: Instant?,
    reachable: Boolean,
    now: Instant,
    pendingPermissions: Int,
    screenShareRequested: Boolean,
    onAcceptScreenShare: () -> Unit,
    onDeclineScreenShare: () -> Unit,
    onOpenPermissions: () -> Unit,
    onSwitchMode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Dispositivo vinculado", style = NpText.Display, color = NpColors.ShieldNavy)
        if (screenShareRequested) {
            Text("Tu tutor quiere ver esta pantalla", style = NpText.Title)
            NpButton("Aceptar", onAcceptScreenShare)
            NpButton("Ahora no", onDeclineScreenShare, variant = NpButtonVariant.Text)
        }
        Text("${deviceName ?: "Este dispositivo"} · Android $androidVersion", style = NpText.Body)
        Text("Supervisado por ${tutors.joinToString(", ")}", style = NpText.Body)
        Text(
            "Última comunicación: " +
                (lastContact?.let { RelativeTime.format(it, now, ZoneId.systemDefault()) } ?: "todavía no") +
                if (reachable) "" else " · Sin conexión",
            style = NpText.Body,
        )
        NpButton(pendingPermissionsLabel(pendingPermissions), onOpenPermissions, variant = NpButtonVariant.Secondary)
        NpButton("Cambiar de modo", onSwitchMode, variant = NpButtonVariant.Text)
        NpButton("Cerrar sesión", onSignOut, variant = NpButtonVariant.Text)
    }
}
