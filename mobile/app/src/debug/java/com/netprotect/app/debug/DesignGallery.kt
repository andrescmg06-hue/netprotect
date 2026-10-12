package com.netprotect.app.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
        Section("NpButton") {
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
                leading = { Icon(painterResource(NpIcons.Smartphone), contentDescription = null, tint = NpColors.SlateMuted) },
                trailing = { Icon(painterResource(NpIcons.ChevronRight), contentDescription = null, tint = NpColors.SlateMuted) },
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
        Text(text = title, style = NpText.Headline, color = NpColors.ShieldNavy)
        content()
    }
}
