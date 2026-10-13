package com.netprotect.app.feature.supervised.permissions

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpChipStatus
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/**
 * Detalle de un permiso (lámina 11): what it enables, its state and, while pending, the real steps of that
 * permission ([permissionDetailSteps]). [permissions] is the shell's live state, re-read on every ON_RESUME,
 * so coming back from Android flips this screen to «Configurado» by itself; there is no «Comprobando…»
 * state because that read is instant. Only calls the callbacks: the shell launches every Intent and permission.
 */
@Composable
fun PermissionDetailScreen(
    permission: SupervisedPermission,
    permissions: PermissionsUi,
    onOpenUsageAccessSettings: () -> Unit,
    onRequestLocation: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestDeviceAdmin: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = permissionTexts(permission)
    val granted = permissions.isGranted(permission)
    val note = permissionDetailNote(permission)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Only this part scrolls: the footer stays fixed, also at a 200 % font.
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = NpSpace.Gutter),
        ) {
            TopRow(onBack = onBack)
            Spacer(Modifier.height(NpSpace.S))
            SegmentBar(current = permission, permissions = permissions)
            Spacer(Modifier.height(NpSpace.M))
            Text(text = permissionPositionLabel(permission), style = NpText.Eyebrow, color = NpColors.InkMuted)
            Spacer(Modifier.height(NpSpace.L))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(NpColors.Navy, NpShapes.Field),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(permissionIcon(permission)),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = NpColors.Paper,
                )
            }
            Spacer(Modifier.height(NpSpace.L))
            Text(
                text = texts.title,
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.M))
            if (granted) {
                StatusPill(status = NpChipStatus.Ready, text = "Configurado", showDot = false)
            } else {
                StatusPill(status = NpChipStatus.Pending, showDot = false)
            }
            Spacer(Modifier.height(NpSpace.Xl))
            SectionHeader("Qué habilita")
            Spacer(Modifier.height(NpSpace.M))
            Text(text = texts.description, style = NpText.Body, color = NpColors.Ink)
            if (note != null) {
                Spacer(Modifier.height(NpSpace.S))
                Text(text = note, style = NpText.BodySm, color = NpColors.InkMuted)
            }
            Spacer(Modifier.height(NpSpace.Xl))
            if (granted) {
                SectionHeader("Estado")
                Spacer(Modifier.height(NpSpace.L))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .border(2.dp, NpColors.SuccessText, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(NpIcons.Check),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = NpColors.SuccessText,
                        )
                    }
                    Spacer(Modifier.width(NpSpace.L))
                    Text(
                        text = PERMISSION_CONFIGURED_TEXT,
                        style = NpText.Body,
                        color = NpColors.InkMuted,
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(NpSpace.Xl))
                Text(text = PERMISSION_DISABLE_HINT, style = NpText.BodySm, color = NpColors.InkMuted)
            } else {
                SectionHeader("Cómo activarlo")
                Spacer(Modifier.height(NpSpace.L))
                Column(verticalArrangement = Arrangement.spacedBy(NpSpace.L)) {
                    permissionDetailSteps(permission).forEachIndexed { index, step ->
                        StepLine(number = index + 1, text = step)
                    }
                }
            }
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Footer(
            granted = granted,
            actionLabel = texts.actionLabel,
            // Location is Android's dialog, not a screen outside the app: no external-link icon.
            actionOpensScreen = permission != SupervisedPermission.Location,
            onActivate = when (permission) {
                SupervisedPermission.UsageAccess -> onOpenUsageAccessSettings
                SupervisedPermission.Location -> onRequestLocation
                SupervisedPermission.Overlay -> onRequestOverlay
                SupervisedPermission.DeviceAdmin -> onRequestDeviceAdmin
            },
            // Denied for good, the dialog never shows again; the app's Settings page is the only way left.
            onOpenAppSettings = if (permission == SupervisedPermission.Location) onOpenAppSettings else null,
            onBack = onBack,
        )
    }
}

@DrawableRes
private fun permissionIcon(permission: SupervisedPermission): Int = when (permission) {
    SupervisedPermission.UsageAccess -> NpIcons.Clock
    SupervisedPermission.Location -> NpIcons.MapPin
    SupervisedPermission.Overlay -> NpIcons.Monitor
    SupervisedPermission.DeviceAdmin -> NpIcons.ShieldCheck
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

/** One segment per permission in canonical order: green if configured, amber for this one while pending,
 * grey for the others still pending. Decorative: the chip says the same. */
@Composable
private fun SegmentBar(current: SupervisedPermission, permissions: PermissionsUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(NpSpace.Xs),
    ) {
        SupervisedPermission.entries.forEach { permission ->
            val color = when {
                permissions.isGranted(permission) -> NpColors.SuccessText
                permission == current -> NpColors.WarningText
                else -> NpColors.Hairline
            }
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Column(Modifier.semantics { heading() }) {
        Text(text = text.uppercase(), style = NpText.Eyebrow, color = NpColors.InkMuted)
        Spacer(Modifier.height(NpSpace.S))
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Ink),
        )
    }
}

@Composable
private fun StepLine(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, NpColors.Ink, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = number.toString(), style = NpText.BodySm, color = NpColors.Ink, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.width(NpSpace.L))
        Text(
            text = text,
            style = NpText.Body,
            color = NpColors.Ink,
            modifier = Modifier
                .weight(1f)
                .padding(top = 2.dp),
        )
    }
}

@Composable
private fun Footer(
    granted: Boolean,
    actionLabel: String,
    actionOpensScreen: Boolean,
    onActivate: () -> Unit,
    onOpenAppSettings: (() -> Unit)?,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(NpColors.Hairline),
        )
        Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
            if (granted) {
                NpButton(
                    text = PERMISSION_BACK_LABEL,
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = NpIcons.ArrowLeft,
                )
            } else {
                NpButton(
                    text = actionLabel,
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (actionOpensScreen) NpIcons.ExternalLink else null,
                )
                if (onOpenAppSettings != null) {
                    Spacer(Modifier.height(NpSpace.S))
                    NpButton(
                        text = PERMISSION_APP_SETTINGS_LABEL,
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth(),
                        variant = NpButtonVariant.Secondary,
                        trailingIcon = NpIcons.ExternalLink,
                    )
                }
                Spacer(Modifier.height(NpSpace.S))
                NpButton(
                    text = PERMISSION_BACK_LABEL,
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                )
            }
        }
    }
}
