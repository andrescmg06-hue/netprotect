package com.netprotect.app.feature.supervised.permissions

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpChipStatus
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/**
 * «Se desactivó un permiso» (lámina 13.4): shown by the shell when coming back to the app finds that [permission],
 * granted at the previous read, is no longer granted ([revokedPermissions]). Only calls the callbacks: «Activar de
 * nuevo» ([onActivate]) opens this permission's detail inside the app, so it has no external-link icon; the arrow
 * and «Volver al inicio» ([onHome]) go back to Inicio.
 */
@Composable
fun PermissionRevokedScreen(
    permission: SupervisedPermission,
    onActivate: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val texts = permissionRevokedTexts(permission)

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
            TopRow(onBack = onHome)
            Spacer(Modifier.height(NpSpace.L))
            Text(text = PERMISSION_REVOKED_EYEBROW.uppercase(), style = NpText.Eyebrow, color = NpColors.WarningText)
            Spacer(Modifier.height(NpSpace.S))
            Text(
                text = PERMISSION_REVOKED_TITLE,
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(NpSpace.M))
            Text(text = texts.body, style = NpText.Body, color = NpColors.InkMuted)
            Spacer(Modifier.height(NpSpace.L))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NpColors.Paper, NpShapes.Card)
                    .border(1.dp, NpColors.WarningText, NpShapes.Card)
                    .padding(NpSpace.L),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconTile(icon = permissionIcon(permission), tone = NpTone.Warning)
                Spacer(Modifier.width(NpSpace.L))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(NpSpace.S)) {
                    Text(text = texts.permissionTitle, style = NpText.Title, color = NpColors.Ink)
                    StatusPill(status = NpChipStatus.Pending, showDot = false)
                }
            }
            Spacer(Modifier.height(NpSpace.L))
            Text(text = texts.note, style = NpText.BodySm, color = NpColors.InkMuted)
            Spacer(Modifier.height(NpSpace.Xl))
        }
        Column(Modifier.fillMaxWidth()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NpColors.Hairline),
            )
            Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
                NpButton(
                    text = PERMISSION_REVOKED_ACTIVATE,
                    onClick = onActivate,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = NpIcons.ArrowRight,
                )
                Spacer(Modifier.height(NpSpace.S))
                NpButton(
                    text = PERMISSION_REVOKED_HOME,
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth(),
                    variant = NpButtonVariant.Secondary,
                )
            }
        }
    }
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
