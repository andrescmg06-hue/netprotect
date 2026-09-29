package com.netprotect.app.feature.tutor.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.SectionHeader
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import java.time.Instant

/** Lista de dispositivos del tutor. */
@Composable
fun DevicesScreen(
    devices: LoadState<List<DeviceSummary>>,
    now: Instant,
    onRefresh: () -> Unit,
    onOpenDevice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            BrandHeader(subtitle = "Panel del tutor")
            Spacer(Modifier.height(24.dp))
            Text(text = "Dispositivos", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Toca un dispositivo para ver sus datos.",
                style = NpText.BodyLead,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(16.dp))
            SectionHeader(
                title = "Dispositivos vinculados",
                actionLabel = "Actualizar",
                actionIcon = NpIcons.RefreshCw,
                onAction = onRefresh,
            )
            Spacer(Modifier.height(8.dp))
            DeviceList(devices = devices, now = now, onOpenDevice = onOpenDevice, onRetry = onRefresh)
            Spacer(Modifier.height(24.dp))
        }
    }
}
