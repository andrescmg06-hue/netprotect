package com.netprotect.app.feature.tutor.device

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.feature.tutor.DeviceSection
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.state.LoadState
import java.time.Instant

/** INTERIM (Sprint 44, Claude): plain but complete, so no tutor function disappears between
 * commits. DeepSeek replaces this body with the redesigned screen; the signature is final. */
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        when (device) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(device.message, style = NpText.Body, color = NpColors.DangerText)
                if (!device.notFound) NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                val d = device.value
                Text(d.name, style = NpText.Display, color = NpColors.ShieldNavy)
                Text("Android ${d.osVersion ?: ""} · ${d.status}".replace("  ", " "), style = NpText.Body)
                NpButton("Actualizar", onRefresh, variant = NpButtonVariant.Text)
                if (rename == null) {
                    NpButton("Renombrar", onStartRename, variant = NpButtonVariant.Secondary)
                } else {
                    OutlinedTextField(value = rename.text, onValueChange = onEditRename, label = { Text("Nombre") })
                    rename.error?.let { Text(it, style = NpText.Body, color = NpColors.DangerText) }
                    NpButton("Guardar", onSaveRename, enabled = rename.canSave, loading = rename.saving)
                    NpButton("Cancelar", onCancelRename, variant = NpButtonVariant.Text)
                }
                NpButton("Desvincular dispositivo", { confirming = true }, variant = NpButtonVariant.Danger, loading = unlinking)
                unlinkError?.let { Text(it, style = NpText.Body, color = NpColors.DangerText) }
                DeviceSection.entries.forEach { section ->
                    NpButton(section.name, { onOpenSection(section) }, variant = NpButtonVariant.Text)
                }
                if (confirming) {
                    ConfirmDialog(
                        title = "¿Desvincular ${d.name}?",
                        message = "Se eliminará la conexión de este dispositivo con tu cuenta de tutor. Para volver a supervisarlo habrá que vincularlo de nuevo.",
                        confirmLabel = "Desvincular",
                        dismissLabel = "Cancelar",
                        onConfirm = { confirming = false; onConfirmUnlink() },
                        onDismiss = { confirming = false },
                        danger = true,
                    )
                }
            }
        }
    }
}
