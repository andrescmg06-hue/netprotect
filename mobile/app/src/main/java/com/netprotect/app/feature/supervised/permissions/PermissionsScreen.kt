package com.netprotect.app.feature.supervised.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.PermissionCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Pantalla «Permisos del dispositivo» del modo supervisado. Solo llama a los callbacks. */
@Composable
fun PermissionsScreen(
    permissions: PermissionsUi,
    onOpenUsageAccessSettings: () -> Unit,
    onRecheckUsageAccess: () -> Unit,
    onRequestLocation: () -> Unit,
    onRecheckLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRecheckOverlay: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onBack: () -> Unit,
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
            NpTopBar(onBack = onBack)
            Text(text = "Permisos del dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Para que NetProtect funcione correctamente en este dispositivo, se requieren los siguientes permisos.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PermissionCard(
                    icon = NpIcons.LayoutGrid,
                    title = "Acceso al uso de apps",
                    description = "Permite conocer cuánto tiempo se usan las aplicaciones y aplicar las reglas de tu tutor. Android exige activarlo en Ajustes.",
                    granted = permissions.usageAccess,
                    actionLabel = "Abrir Ajustes",
                    onAction = onOpenUsageAccessSettings,
                    secondaryLabel = "Ya lo activé, verificar de nuevo",
                    onSecondary = onRecheckUsageAccess,
                )
                PermissionCard(
                    icon = NpIcons.MapPin,
                    title = "Ubicación aproximada",
                    description = "Permite reportar la última ubicación conocida y detectar geocercas con información aproximada. No se usa la ubicación precisa.",
                    granted = permissions.location,
                    actionLabel = "Permitir ubicación aproximada",
                    onAction = onRequestLocation,
                    secondaryLabel = "Ya lo activé, verificar de nuevo",
                    onSecondary = onRecheckLocation,
                )
                PermissionCard(
                    icon = NpIcons.Monitor,
                    title = "Mostrar sobre otras apps",
                    description = "Necesario para mostrar la pantalla de bloqueo cuando corresponda, también con el teléfono en uso.",
                    granted = permissions.overlay,
                    actionLabel = "Abrir Ajustes",
                    onAction = onRequestOverlay,
                    secondaryLabel = "Ya lo activé, verificar de nuevo",
                    onSecondary = onRecheckOverlay,
                )
                PermissionCard(
                    icon = NpIcons.Shield,
                    title = "Protección contra desinstalación",
                    description = "Permite avisar al tutor si se intenta desinstalar NetProtect; no elimina el dispositivo ni cambia contraseñas.",
                    granted = permissions.deviceAdmin,
                    actionLabel = "Activar protección",
                    onAction = onRequestDeviceAdmin,
                )
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Solo solicitamos los permisos necesarios para las funciones de NetProtect. Puedes revocarlos en cualquier momento desde los ajustes del dispositivo.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
