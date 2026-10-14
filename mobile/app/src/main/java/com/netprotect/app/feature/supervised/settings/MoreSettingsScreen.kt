package com.netprotect.app.feature.supervised.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.SignOutConfirmDialog
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/**
 * «Más ajustes» del modo supervisado (lámina 10). Sprint 63: solo «Cerrar sesión», que pide confirmación
 * antes de llamar a [onSignOut], y la nota de que solo el tutor puede desvincular. «Privacidad» no está aquí:
 * se abre desde Inicio (decisión 4, S66); «Cambiar de modo» se eliminó (PLAN §1, pregunta 13).
 */
@Composable
fun MoreSettingsScreen(
    onSignOut: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingSignOut by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
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
                    Icon(
                        painter = painterResource(NpIcons.ArrowLeft),
                        contentDescription = "Atrás",
                        tint = NpColors.Ink,
                    )
                }
                Spacer(Modifier.weight(1f))
                Image(
                    painter = painterResource(NpIcons.LogoShield),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Más ajustes",
                style = NpText.Display,
                color = NpColors.Ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(text = "Opciones de este teléfono.", style = NpText.BodySm, color = NpColors.InkMuted)
            Spacer(Modifier.height(16.dp))
            Hairline()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button) { confirmingSignOut = true }
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    painter = painterResource(NpIcons.LogOut),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = NpColors.Ink,
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Cerrar sesión", style = NpText.BodyStrong, color = NpColors.Ink)
                    Text(text = "Salir de tu cuenta en este teléfono.", style = NpText.BodySm, color = NpColors.InkMuted)
                }
                Spacer(Modifier.width(12.dp))
                Icon(
                    painter = painterResource(NpIcons.ChevronRight),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NpColors.InkMuted,
                )
            }
            Hairline()
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NpColors.BlueWash, NpShapes.Card)
                    .padding(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    painter = painterResource(NpIcons.Info),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(18.dp),
                    tint = NpColors.BlueText,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Para desvincular este dispositivo, pídeselo a tu tutor. Solo tu tutor puede hacerlo.",
                    style = NpText.BodySm,
                    color = NpColors.Ink,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmingSignOut) {
        SignOutConfirmDialog(
            onConfirm = {
                confirmingSignOut = false
                onSignOut()
            },
            onDismiss = { confirmingSignOut = false },
        )
    }
}

@Composable
private fun Hairline() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NpColors.Hairline),
    )
}
