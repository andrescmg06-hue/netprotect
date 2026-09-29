package com.netprotect.app.feature.tutor.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.ui.state.LoadState
import java.time.Instant

/** INTERIM (Sprint 44, Claude): plain but complete, so no tutor function disappears between
 * commits. DeepSeek replaces this body with the redesigned screen; the signature is final. */
@Composable
fun TutorHomeScreen(
    userName: String?,
    userEmail: String,
    pairing: PairingUi,
    devices: LoadState<List<DeviceSummary>>,
    now: Instant,
    onGenerateCode: () -> Unit,
    onRevokeCode: () -> Unit,
    onDismissPairing: () -> Unit,
    onRefreshDevices: () -> Unit,
    onOpenDevice: (String) -> Unit,
    onOpenActivity: () -> Unit,
    onSwitchMode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Modo Tutor", style = NpText.Display, color = NpColors.ShieldNavy)
        NpButton("Cambiar de modo", onSwitchMode, variant = NpButtonVariant.Secondary)
        when (pairing) {
            PairingUi.Idle -> NpButton("Generar código de vinculación", onGenerateCode)
            PairingUi.Generating -> NpButton("Generar código de vinculación", {}, loading = true)
            is PairingUi.Active -> {
                Text(pairing.code, style = NpText.Numeric, color = NpColors.ShieldNavy)
                Text("Vence en ${pairing.remainingSeconds / 60}:${"%02d".format(pairing.remainingSeconds % 60)}. Uso único.", style = NpText.Body)
                NpButton("Revocar", onRevokeCode, variant = NpButtonVariant.Danger, loading = pairing.revoking)
            }
            PairingUi.Expired -> NpButton("El código venció. Genera uno nuevo.", onDismissPairing, variant = NpButtonVariant.Secondary)
            is PairingUi.Failed -> {
                Text(pairing.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Aceptar", onDismissPairing, variant = NpButtonVariant.Secondary)
            }
        }
        NpButton("¿Ya se vinculó? Actualizar lista", onRefreshDevices, variant = NpButtonVariant.Text)
        Text("Dispositivos vinculados", style = NpText.Title, color = NpColors.ShieldNavy)
        com.netprotect.app.feature.tutor.devices.InterimDeviceList(devices, onOpenDevice)
        NpButton("Mi actividad (auditoría)", onOpenActivity, variant = NpButtonVariant.Text)
        NpButton("Cerrar sesión", onSignOut, variant = NpButtonVariant.Text)
    }
}
