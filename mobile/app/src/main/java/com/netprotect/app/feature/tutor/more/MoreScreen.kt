package com.netprotect.app.feature.tutor.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Pestaña "Más": cambiar de modo, cerrar sesión y acerca de. */
@Composable
fun MoreScreen(
    appVersion: String,
    onSwitchMode: () -> Unit,
    onSignOut: () -> Unit,
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
            Text(text = "Más", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(16.dp))
            NpCard(onClick = onSwitchMode) {
                MoreRow(
                    icon = NpIcons.ArrowLeftRight,
                    tone = NpTone.Info,
                    title = "Cambiar de modo",
                    titleColor = NpColors.ShieldNavy,
                    description = "Usa este teléfono como dispositivo supervisado.",
                    chevron = true,
                )
            }
            Spacer(Modifier.height(12.dp))
            NpCard(onClick = onSignOut) {
                MoreRow(
                    icon = NpIcons.LogOut,
                    tone = NpTone.Danger,
                    title = "Cerrar sesión",
                    titleColor = NpColors.DangerText,
                    description = "Cierra la sesión en este teléfono.",
                    chevron = false,
                )
            }
            Spacer(Modifier.height(12.dp))
            NpCard {
                MoreRow(
                    icon = NpIcons.ShieldCheck,
                    tone = NpTone.Success,
                    title = "Acerca de NetProtect",
                    titleColor = NpColors.ShieldNavy,
                    description = "Versión $appVersion",
                    chevron = false,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "NetProtect solo muestra lo que el dispositivo supervisado comparte con tu cuenta. La ubicación es aproximada, nada se graba y cada acción queda en tu registro de actividad.",
                    style = NpText.Body,
                    color = NpColors.Ink,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MoreRow(
    @androidx.annotation.DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    description: String,
    chevron: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, tone = tone, size = 48.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = NpText.Title, color = titleColor)
            Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
        }
        if (chevron) {
            Icon(
                painter = painterResource(NpIcons.ChevronRight),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = NpColors.SlateMuted,
            )
        }
    }
}
