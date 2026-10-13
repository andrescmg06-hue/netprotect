package com.netprotect.app.feature.supervised.permissions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.feature.supervised.pendingPermissionsLabel
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpRail
import com.netprotect.app.ui.components.NpRailStep
import com.netprotect.app.ui.components.RailStatus
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/**
 * Permisos del modo supervisado (lámina 04): a rail with the pending permissions first and the next one
 * highlighted with its button. Only calls the callbacks; the shell launches every Intent and permission.
 */
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
    val allConfigured = permissions.pending == 0
    // Device admin has no recheck: the shell reads it again from the activation screen's result.
    val recheckAll = {
        onRecheckUsageAccess()
        onRecheckLocation()
        onRecheckOverlay()
    }
    val steps = permissionSteps(permissions).map { step ->
        val texts = permissionTexts(step.permission)
        NpRailStep(
            title = texts.title,
            description = texts.description,
            status = step.status,
            action = if (step.status == RailStatus.Next) {
                {
                    NpButton(
                        text = texts.actionLabel,
                        onClick = when (step.permission) {
                            SupervisedPermission.UsageAccess -> onOpenUsageAccessSettings
                            SupervisedPermission.Location -> onRequestLocation
                            SupervisedPermission.Overlay -> onRequestOverlay
                            SupervisedPermission.DeviceAdmin -> onRequestDeviceAdmin
                        },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = NpIcons.ExternalLink,
                    )
                }
            } else {
                null
            },
        )
    }

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
            // At least one screen tall so the buttons sit at the bottom; with a large font they follow the rail.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.padding(horizontal = NpSpace.Gutter)) {
                    TopRow(onBack = onBack)
                    Spacer(Modifier.height(NpSpace.S))
                    Text(
                        text = "Permisos",
                        style = NpText.Display,
                        color = NpColors.Ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(NpSpace.S))
                    Text(
                        text = "NetProtect necesita estos cuatro permisos en este teléfono.",
                        style = NpText.Body,
                        color = NpColors.InkMuted,
                    )
                    Spacer(Modifier.height(NpSpace.Xl))
                    ProgressHeader(permissions)
                    Spacer(Modifier.height(NpSpace.Xl))
                    NpRail(steps = steps)
                    if (allConfigured) {
                        Spacer(Modifier.height(NpSpace.L))
                        Text(
                            text = "Si revocas un permiso en Ajustes, lo comprobamos al volver a la app.",
                            style = NpText.BodySm,
                            color = NpColors.InkMuted,
                            // Aligned with the rail's text column.
                            modifier = Modifier.padding(start = 48.dp),
                        )
                    }
                    Spacer(Modifier.height(NpSpace.Xl))
                }
                Footer(allConfigured = allConfigured, onRecheck = recheckAll, onBack = onBack)
            }
        }
    }
}

/** Texts of one permission, literal from 02_PANTALLAS §04. */
private class PermissionTexts(val title: String, val description: String, val actionLabel: String)

private fun permissionTexts(permission: SupervisedPermission): PermissionTexts = when (permission) {
    SupervisedPermission.UsageAccess -> PermissionTexts(
        title = "Acceso al uso de apps",
        description = "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.",
        actionLabel = "Activar en Ajustes",
    )
    // Location is Android's own dialog, not a Settings screen: "Activar en Ajustes" would be false.
    SupervisedPermission.Location -> PermissionTexts(
        title = "Ubicación aproximada",
        description = "Permite reportar la última ubicación conocida. No se usa la ubicación precisa.",
        actionLabel = "Permitir ubicación aproximada",
    )
    SupervisedPermission.Overlay -> PermissionTexts(
        title = "Mostrar sobre otras apps",
        description = "Necesario para mostrar la pantalla de bloqueo cuando corresponda.",
        actionLabel = "Activar en Ajustes",
    )
    // Pending question 7: the text says it warns the tutor, never that it prevents uninstalling.
    SupervisedPermission.DeviceAdmin -> PermissionTexts(
        title = "Protección contra desinstalación",
        description = "Avisa a tu tutor si alguien intenta desinstalar NetProtect.",
        actionLabel = "Activar en Ajustes",
    )
}

@Composable
private fun TopRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.CenterStart,
        ) {
            Icon(painter = painterResource(NpIcons.ArrowLeft), contentDescription = "Atrás", tint = NpColors.Ink)
        }
        Spacer(Modifier.weight(1f))
        Image(painter = painterResource(NpIcons.LogoShield), contentDescription = null, modifier = Modifier.size(24.dp))
    }
}

/** "2 de 4 configurados" (amber) or "Todos los permisos están configurados" (green), over a 4-segment bar. */
@Composable
private fun ProgressHeader(permissions: PermissionsUi) {
    val allConfigured = permissions.pending == 0
    val color = if (allConfigured) NpColors.SuccessText else NpColors.WarningText
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (allConfigured) {
            Icon(painter = painterResource(NpIcons.Check), contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
            Spacer(Modifier.width(NpSpace.M))
        }
        Text(
            text = if (allConfigured) pendingPermissionsLabel(0) else configuredCaption(permissions),
            style = NpText.Body.copy(fontWeight = FontWeight.SemiBold),
            color = color,
        )
    }
    Spacer(Modifier.height(NpSpace.M))
    val configured = SupervisedPermission.entries.size - permissions.pending
    // Decorative: the line above says the same.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(NpSpace.S),
    ) {
        repeat(SupervisedPermission.entries.size) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(if (index < configured) NpColors.SuccessText else NpColors.Hairline),
            )
        }
    }
}

@Composable
private fun Footer(allConfigured: Boolean, onRecheck: () -> Unit, onBack: () -> Unit) {
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Hairline),
        )
        Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
            if (allConfigured) {
                NpButton(
                    text = "Volver al inicio",
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = NpIcons.Home,
                )
                Spacer(Modifier.height(NpSpace.S))
                NpButton(
                    text = "Verificar de nuevo",
                    onClick = onRecheck,
                    variant = NpButtonVariant.Text,
                    icon = NpIcons.RefreshCw,
                )
            } else {
                NpButton(
                    text = "Ya lo activé, verificar de nuevo",
                    onClick = onRecheck,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                    trailingIcon = NpIcons.RefreshCw,
                )
            }
        }
    }
}
