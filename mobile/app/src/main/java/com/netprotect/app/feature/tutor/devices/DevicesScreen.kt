package com.netprotect.app.feature.tutor.devices

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
fun DevicesScreen(
    devices: LoadState<List<DeviceSummary>>,
    now: Instant,
    onRefresh: () -> Unit,
    onOpenDevice: (String) -> Unit,
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
        Text("Dispositivos vinculados", style = NpText.Display, color = NpColors.ShieldNavy)
        NpButton("Actualizar", onRefresh, variant = NpButtonVariant.Text)
        InterimDeviceList(devices, onOpenDevice)
    }
}

@Composable
internal fun InterimDeviceList(
    devices: LoadState<List<DeviceSummary>>,
    onOpenDevice: (String) -> Unit,
) {
    when (devices) {
        LoadState.Loading -> Text("Cargando…", style = NpText.Body, color = NpColors.SlateMuted)
        is LoadState.Failed -> Text(devices.message, style = NpText.Body, color = NpColors.DangerText)
        is LoadState.Loaded -> if (devices.value.isEmpty()) {
            Text("Todavía no hay dispositivos vinculados.", style = NpText.Body, color = NpColors.SlateMuted)
        } else {
            devices.value.forEach { device ->
                Text(
                    "${device.name} · ${device.status}",
                    style = NpText.BodyStrong,
                    color = NpColors.ShieldNavy,
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDevice(device.id) }.padding(vertical = 12.dp),
                )
            }
        }
    }
}
