package com.netprotect.app.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.ListRow
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpChipStatus
import com.netprotect.app.ui.components.NpHeroPanel
import com.netprotect.app.ui.components.NpRail
import com.netprotect.app.ui.components.NpRailStep
import com.netprotect.app.ui.components.NpSeal
import com.netprotect.app.ui.components.RailStatus
import com.netprotect.app.ui.components.SealArcState
import com.netprotect.app.ui.components.permissionsCaption
import com.netprotect.app.ui.components.sealStatesFor
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.OtpInput
import com.netprotect.app.ui.components.PermissionCard
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Galería de componentes (solo debug): una sección por componente con todos sus estados. Los datos
 * son ilustrativos ("Tablet de Sofía") y nada de esto se usa fuera de `src/debug`. */
@Composable
fun DesignGallery() {
    var otp by remember { mutableStateOf("") }
    var showConfirm by remember { mutableStateOf(false) }
    var confirmDanger by remember { mutableStateOf(false) }

    if (showConfirm) {
        ConfirmDialog(
            title = if (confirmDanger) "Desvincular dispositivo" else "Guardar cambios",
            message = if (confirmDanger) "¿Seguro que quieres desvincularlo?" else "Se guardarán los cambios.",
            confirmLabel = if (confirmDanger) "Desvincular" else "Guardar",
            dismissLabel = "Cancelar",
            onConfirm = { showConfirm = false },
            onDismiss = { showConfirm = false },
            danger = confirmDanger,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Section("NpSeal") {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                NpSeal(states = sealStatesFor(readyCount = 4), size = 96.dp, contentDescription = "4 de 4 permisos")
                NpSeal(
                    states = listOf(SealArcState.Ready, SealArcState.Ready, SealArcState.Pending, SealArcState.Inactive),
                    size = 96.dp,
                )
                NpSeal(states = sealStatesFor(readyCount = 4, reporting = false), size = 96.dp)
            }
        }

        Section("NpHeroPanel") {
            NpHeroPanel(
                eyebrow = "Dispositivo supervisado",
                headline = "Todo funciona",
                sealStates = sealStatesFor(readyCount = 4),
                sealCaption = permissionsCaption(4),
                summary = "Los 4 permisos están configurados y el dispositivo se comunicó hace un momento.",
            )
            NpHeroPanel(
                eyebrow = "Dispositivo supervisado",
                headline = "Falta un paso",
                sealStates = sealStatesFor(readyCount = 2),
                sealCaption = permissionsCaption(2),
                summary = "Faltan 2 permisos para que NetProtect funcione bien en este teléfono.",
                action = {
                    NpButton("Revisar permisos", {}, Modifier.fillMaxWidth(), trailingIcon = NpIcons.ArrowRight)
                },
            )
            NpHeroPanel(
                eyebrow = "Dispositivo supervisado",
                headline = "Sin reporte reciente",
                sealStates = sealStatesFor(readyCount = 4, reporting = false),
                sealCaption = permissionsCaption(4),
                summary = "No recibimos una actualización reciente del dispositivo. El vínculo sigue activo.",
            )
        }

        Section("NpRail") {
            NpRail(
                steps = listOf(
                    NpRailStep(
                        "Mostrar sobre otras apps",
                        "Necesario para mostrar la pantalla de bloqueo cuando corresponda.",
                        RailStatus.Next,
                        action = {
                            NpButton(
                                "Activar en Ajustes", {}, Modifier.fillMaxWidth(),
                                trailingIcon = NpIcons.ExternalLink,
                            )
                        },
                    ),
                    NpRailStep(
                        "Protección contra desinstalación",
                        "Avisa a tu tutor si alguien intenta desinstalar NetProtect.",
                        RailStatus.Pending,
                    ),
                    NpRailStep(
                        "Acceso al uso de apps",
                        "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.",
                        RailStatus.Done,
                    ),
                    NpRailStep(
                        "Ubicación aproximada",
                        "Permite reportar la última ubicación conocida. No se usa la ubicación precisa.",
                        RailStatus.Done,
                    ),
                ),
            )
            HorizontalDivider(color = NpColors.Hairline)
            NpRail(
                steps = listOf(
                    NpRailStep(
                        "Acceso al uso de apps",
                        "Permite saber cuánto tiempo se usan las apps y aplicar las reglas de tu tutor.",
                        RailStatus.Done,
                    ),
                    NpRailStep(
                        "Ubicación aproximada",
                        "Permite reportar la última ubicación conocida. No se usa la ubicación precisa.",
                        RailStatus.Done,
                    ),
                    NpRailStep(
                        "Mostrar sobre otras apps",
                        "Necesario para mostrar la pantalla de bloqueo cuando corresponda.",
                        RailStatus.Done,
                    ),
                    NpRailStep(
                        "Protección contra desinstalación",
                        "Avisa a tu tutor si alguien intenta desinstalar NetProtect.",
                        RailStatus.Done,
                    ),
                ),
            )
        }

        Section("NpButton") {
            NpButton("Revisar permisos", {}, Modifier.fillMaxWidth(), trailingIcon = NpIcons.ArrowRight)
            NpButton("Volver al inicio", {}, Modifier.fillMaxWidth(), trailingIcon = NpIcons.Home)
            NpButton(
                "Ya lo activé, verificar de nuevo", {}, Modifier.fillMaxWidth(),
                variant = NpButtonVariant.Secondary, trailingIcon = NpIcons.RefreshCw,
            )
            NpButton("Verificar de nuevo", {}, variant = NpButtonVariant.Text, icon = NpIcons.RefreshCw)
            NpButton("Primario", {})
            NpButton("Secundario", {}, variant = NpButtonVariant.Secondary)
            NpButton("Texto", {}, variant = NpButtonVariant.Text)
            NpButton("Peligro", {}, variant = NpButtonVariant.Danger)
            NpButton("Cargando", {}, loading = true)
            NpButton("Deshabilitado", {}, enabled = false)
            NpButton("Con icono", {}, icon = NpIcons.Check)
            NpButton("Pequeño", {}, small = true)
            NpButton(
                "Iniciar sesión con Google", {}, Modifier.fillMaxWidth(), variant = NpButtonVariant.Secondary,
                icon = NpIcons.GoogleG, tintIcon = false, trailingIcon = NpIcons.ArrowRight,
            )
            NpButton("Elegir", {}, Modifier.fillMaxWidth(), trailingIcon = NpIcons.ArrowRight)
        }

        Section("StatusPill") {
            NpChipStatus.entries.forEach { status -> StatusPill(status = status) }
            StatusPill(status = NpChipStatus.Linked, filled = false)
            StatusPill(text = "Sin actualización reciente", tone = NpTone.Neutral, modifier = Modifier.width(140.dp))
            listOf(
                "En línea" to NpTone.Success,
                "Desconectado" to NpTone.Neutral,
                "Alerta" to NpTone.Danger,
                "Sincronizando" to NpTone.Info,
                "Restringido" to NpTone.Warning,
                "Desvinculado" to NpTone.Neutral,
            ).forEach { (label, tone) ->
                StatusPill(text = label, tone = tone)
            }
            NpTone.entries.forEach { tone ->
                StatusPill(text = tone.name, tone = tone, showDot = false)
            }
        }

        Section("InfoBanner") {
            InfoBanner(title = "Aviso", text = "La ubicación se reporta cada ~15 minutos.")
            InfoBanner(
                title = "Advertencia",
                text = "El permiso de uso está desactivado.",
                tone = NpTone.Warning,
                icon = NpIcons.TriangleAlert,
            )
        }

        Section("LoadingState") {
            LoadingState()
        }

        Section("ErrorState") {
            ErrorState(message = "No se pudo cargar.", onRetry = {})
        }

        Section("BrandHeader") {
            BrandHeader()
            BrandHeader(subtitle = "Panel del tutor")
            BrandHeader(modifier = Modifier.fillMaxWidth(), stacked = true)
        }

        Section("NpTopBar") {
            NpTopBar(title = "Dispositivos")
            NpTopBar(onBack = {})
        }

        Section("OtpInput") {
            OtpInput(value = "", onValueChange = { otp = it })
            OtpInput(value = "482", onValueChange = { otp = it })
            OtpInput(value = "123456", onValueChange = { otp = it }, isError = true)
        }

        Section("PermissionCard") {
            PermissionCard(
                icon = NpIcons.Eye,
                title = "Acceso a uso de apps",
                description = "Necesario para aplicar reglas de bloqueo.",
                granted = false,
                actionLabel = "Conceder permiso",
                onAction = {},
                secondaryLabel = "Ya lo activé, verificar de nuevo",
                onSecondary = {},
            )
            PermissionCard(
                icon = NpIcons.Eye,
                title = "Acceso a uso de apps",
                description = "Concedido.",
                granted = true,
                actionLabel = "Conceder permiso",
                onAction = {},
            )
        }

        Section("NpCard") {
            NpCard { Text("Tarjeta normal") }
            NpCard(onClick = {}) { Text("Tarjeta pulsable") }
        }

        Section("ListRow") {
            ListRow(
                title = "Tablet de Sofía",
                subtitle = "Android 13",
                leading = { Icon(painterResource(NpIcons.Smartphone), contentDescription = null, tint = NpColors.InkMuted) },
                trailing = { Icon(painterResource(NpIcons.ChevronRight), contentDescription = null, tint = NpColors.InkMuted) },
                onClick = {},
            )
        }

        Section("ConfirmDialog") {
            NpButton("Abrir diálogo", { confirmDanger = false; showConfirm = true })
            NpButton("Abrir diálogo peligroso", { confirmDanger = true; showConfirm = true }, variant = NpButtonVariant.Danger)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(text = title, style = NpText.Title, color = NpColors.Ink)
        content()
    }
}
