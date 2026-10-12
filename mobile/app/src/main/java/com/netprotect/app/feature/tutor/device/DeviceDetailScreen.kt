package com.netprotect.app.feature.tutor.device

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.DeviceSection
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.format.DeviceStatusLabels
import com.netprotect.app.ui.format.RelativeTime
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.ZoneId

/** Detalle de un dispositivo: cabecera, renombrado, desvinculación y las seis secciones. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeviceDetailScreen(
    device: LoadState<DeviceSummary>,
    now: Instant,
    rename: RenameUi?,
    unlinking: Boolean,
    unlinkError: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onStartRename: () -> Unit,
    onEditRename: (String) -> Unit,
    onSaveRename: () -> Unit,
    onCancelRename: () -> Unit,
    onConfirmUnlink: () -> Unit,
    onOpenSection: (DeviceSection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirming by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState()),
        ) {
            NpTopBar(onBack = onBack)
            when (device) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> {
                    if (device.notFound) {
                        EmptyState(
                            icon = NpIcons.CircleAlert,
                            title = device.message,
                            message = "Puede que se haya desvinculado desde otro lugar.",
                            actionLabel = "Volver a dispositivos",
                            onAction = onBack,
                        )
                    } else {
                        ErrorState(message = device.message, onRetry = onRefresh)
                    }
                }
                is LoadState.Loaded -> {
                    val d = device.value
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(text = d.name, style = NpText.Display, color = NpColors.ShieldNavy)
                        Spacer(Modifier.height(8.dp))
                        val (statusLabel, statusTone) = DeviceStatusLabels.label(d.status)
                        // FlowRow: at large font scales the pill moves to its own line instead of being clipped.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            itemVerticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = d.osVersion?.let { "Android $it" } ?: "Android",
                                style = NpText.Body,
                                color = NpColors.SlateMuted,
                            )
                            StatusPill(text = statusLabel, tone = statusTone, showDot = false)
                        }
                        Spacer(Modifier.height(8.dp))
                        val relative = d.lastSeenAt?.let { raw ->
                            runCatching { RelativeTime.format(Instant.parse(raw), now, ZoneId.systemDefault()) }.getOrNull()
                        }
                        Text(
                            text = relative?.let { "Última actividad: $it" } ?: "Sin actividad registrada todavía.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                        Text(
                            text = "Los datos se sincronizan periódicamente.",
                            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
                            color = NpColors.SlateMuted,
                        )
                        Spacer(Modifier.height(16.dp))
                        ManagementCard(
                            rename = rename,
                            unlinking = unlinking,
                            unlinkError = unlinkError,
                            onStartRename = onStartRename,
                            onRefresh = onRefresh,
                            onEditRename = onEditRename,
                            onSaveRename = onSaveRename,
                            onCancelRename = onCancelRename,
                            onAskUnlink = { confirming = true },
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionCard(DeviceSection.Apps, NpIcons.LayoutGrid, NpTone.Info, "Apps", "Revisa el uso de aplicaciones en este dispositivo.", onOpenSection)
                            SectionCard(DeviceSection.Location, NpIcons.MapPin, NpTone.Purple, "Ubicación", "Última ubicación conocida y registros.", onOpenSection)
                            SectionCard(DeviceSection.Geofences, NpIcons.Map, NpTone.Success, "Geocercas", "Consulta las geocercas y su historial.", onOpenSection)
                            SectionCard(DeviceSection.History, NpIcons.History, NpTone.Warning, "Historial", "Eventos de actividad, bloqueos y geocercas.", onOpenSection)
                            SectionCard(DeviceSection.Statistics, NpIcons.BarChart3, NpTone.Info, "Estadísticas", "Uso de apps, bloqueos y cumplimiento.", onOpenSection)
                            SectionCard(DeviceSection.Alerts, NpIcons.TriangleAlert, NpTone.Danger, "Alertas", "Eventos importantes de este dispositivo.", onOpenSection)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

        if (confirming && device is LoadState.Loaded) {
            ConfirmDialog(
                title = "¿Desvincular ${device.value.name}?",
                message = "Se eliminará la conexión de este dispositivo con tu cuenta de tutor. Para volver a supervisarlo habrá que vincularlo de nuevo.",
                confirmLabel = "Desvincular",
                dismissLabel = "Cancelar",
                onConfirm = {
                    confirming = false
                    onConfirmUnlink()
                },
                onDismiss = { confirming = false },
                danger = true,
            )
        }
    }
}

@Composable
private fun ManagementCard(
    rename: RenameUi?,
    unlinking: Boolean,
    unlinkError: String?,
    onStartRename: () -> Unit,
    onRefresh: () -> Unit,
    onEditRename: (String) -> Unit,
    onSaveRename: () -> Unit,
    onCancelRename: () -> Unit,
    // Opens the confirmation dialog; unlinking itself only happens from the dialog.
    onAskUnlink: () -> Unit,
) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (rename == null) {
                NpButton(
                    text = "Renombrar",
                    onClick = onStartRename,
                    variant = NpButtonVariant.Secondary,
                    icon = NpIcons.Pencil,
                    small = true,
                )
                Spacer(Modifier.width(8.dp))
            }
            NpButton(
                text = "Actualizar",
                onClick = onRefresh,
                variant = NpButtonVariant.Secondary,
                icon = NpIcons.RefreshCw,
                small = true,
            )
        }
        if (rename != null) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = rename.text,
                onValueChange = onEditRename,
                label = { Text("Nombre") },
                singleLine = true,
                isError = rename.error != null,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NpColors.SignalBlue,
                    unfocusedBorderColor = NpColors.HairlineStrong,
                    errorBorderColor = NpColors.Danger,
                ),
            )
            if (rename.error != null) {
                Text(text = rename.error, style = NpText.Body, color = NpColors.DangerText)
            }
            Spacer(Modifier.height(12.dp))
            Row {
                NpButton(
                    text = "Guardar",
                    onClick = onSaveRename,
                    modifier = Modifier.weight(1f),
                    enabled = rename.canSave,
                    loading = rename.saving,
                )
                Spacer(Modifier.width(8.dp))
                NpButton(
                    text = "Cancelar",
                    onClick = onCancelRename,
                    modifier = Modifier.weight(1f),
                    variant = NpButtonVariant.Secondary,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        NpCard(
            onClick = if (unlinking) null else onAskUnlink,
            containerColor = NpColors.DangerWash,
            borderColor = NpColors.Danger.copy(alpha = 0.3f),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = NpIcons.Trash2, tone = NpTone.Danger, size = 48.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Desvincular dispositivo", style = NpText.BodyStrong, color = NpColors.DangerText)
                    Text(
                        text = "Se eliminará la conexión de este dispositivo con tu cuenta de tutor.",
                        style = NpText.Body,
                        color = NpColors.SlateMuted,
                    )
                }
                if (unlinking) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NpColors.Danger)
                } else {
                    Icon(
                        painter = painterResource(NpIcons.ChevronRight),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = NpColors.SlateMuted,
                    )
                }
            }
        }
        if (unlinkError != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = unlinkError,
                style = NpText.Body,
                color = NpColors.DangerText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
    }
}

@Composable
private fun SectionCard(
    section: DeviceSection,
    @DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    description: String,
    onOpenSection: (DeviceSection) -> Unit,
) {
    NpCard(onClick = { onOpenSection(section) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = icon, tone = tone, size = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = NpText.Title, color = NpColors.ShieldNavy)
                Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
            }
            Icon(
                painter = painterResource(NpIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
        }
    }
}
