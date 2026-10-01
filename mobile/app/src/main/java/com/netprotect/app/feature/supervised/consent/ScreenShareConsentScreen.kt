package com.netprotect.app.feature.supervised.consent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Consentimiento de vista remota del modo supervisado. Solo muestra la petición y responde. */
@Composable
fun ScreenShareConsentScreen(
    onContinue: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var authorized by rememberSaveable { mutableStateOf(false) }
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
            BrandHeader(subtitle = "Dispositivo supervisado")
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Consentimiento para ver la pantalla",
                style = NpText.Display,
                color = NpColors.ShieldNavy,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tu tutor pidió ver la pantalla de este dispositivo ahora.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(20.dp))
            NpCard(containerColor = NpColors.BlueWash, borderColor = NpColors.BlueWash) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(icon = NpIcons.Eye, tone = NpTone.Info, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "¿Qué permite?", style = NpText.Title, color = NpColors.ShieldNavy)
                        Text(
                            text = "Que tu tutor vea la pantalla de este dispositivo en tiempo real, solo durante esta sesión.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(text = "¿Cuándo se puede usar?", style = NpText.Headline, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            NpCard {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ConsentRow(
                        icon = NpIcons.Clock,
                        tone = NpTone.Purple,
                        title = "Solo cuando tu tutor lo pide",
                        description = "No es continua: tu tutor la pide desde el panel web y tú decides cada vez.",
                    )
                    ConsentRow(
                        icon = NpIcons.Shield,
                        tone = NpTone.Success,
                        title = "Con fines de supervisión",
                        description = "Se usa para acompañar tu seguridad digital.",
                    )
                    ConsentRow(
                        icon = NpIcons.EyeOff,
                        tone = NpTone.Info,
                        title = "Puedes detenerla cuando quieras",
                        description = "Desde la notificación de Android o desde el aviso azul de esta app.",
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(text = "Tu consentimiento", style = NpText.Headline, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            NpCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(value = authorized, role = Role.Checkbox, onValueChange = { authorized = it }),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = authorized, onCheckedChange = null)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Autorizo a mi tutor a ver la pantalla de este dispositivo ahora",
                            style = NpText.BodyStrong,
                            color = NpColors.ShieldNavy,
                        )
                        Text(
                            text = "Solo para esta sesión. Android te pedirá confirmarlo.",
                            style = NpText.Body,
                            color = NpColors.SlateMuted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            NpButton(
                text = "Continuar",
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = authorized,
            )
            Spacer(Modifier.height(8.dp))
            NpButton(
                text = "Ahora no",
                onClick = onDecline,
                modifier = Modifier.fillMaxWidth(),
                variant = NpButtonVariant.Secondary,
            )
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Android te pedirá confirmarlo en un diálogo del sistema. Mientras se comparta verás un aviso permanente. No se graba ni se controla el dispositivo.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConsentRow(
    @androidx.annotation.DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    description: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon = icon, tone = tone, size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
            Text(text = description, style = NpText.Body, color = NpColors.SlateMuted)
        }
    }
}
