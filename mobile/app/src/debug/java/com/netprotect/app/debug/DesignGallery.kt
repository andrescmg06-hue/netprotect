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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.CodeDisplay
import com.netprotect.app.ui.components.ConfirmDialog
import com.netprotect.app.ui.components.DayHeader
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.FilterChips
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.ListRow
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpBottomBar
import com.netprotect.app.ui.components.NpBottomItem
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.components.NpTopBar
import com.netprotect.app.ui.components.OtpInput
import com.netprotect.app.ui.components.PermissionCard
import com.netprotect.app.ui.components.SectionHeader
import com.netprotect.app.ui.components.SegmentedControl
import com.netprotect.app.ui.components.SeverityBadge
import com.netprotect.app.ui.components.StatusPill
import com.netprotect.app.ui.components.TimelineItem
import com.netprotect.app.ui.components.UsageBar
import com.netprotect.app.ui.format.DeviceStatusLabels
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Galería de componentes (solo debug): una sección por componente con todos sus estados. Los datos
 * son ilustrativos ("Tablet de Sofía") y nada de esto se usa fuera de `src/debug`. */
@Composable
fun DesignGallery() {
    var segmentedIndex by remember { mutableIntStateOf(0) }
    var chipsIndex by remember { mutableIntStateOf(0) }
    var bottomIndex by remember { mutableIntStateOf(0) }
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
                "ONLINE", "OFFLINE", "ALERT", "SYNCING", "RESTRICTED", "UNLINKED",
            ).forEach { status ->
                val (label, tone) = DeviceStatusLabels.label(status)
                StatusPill(text = label, tone = tone)
            }
            NpTone.entries.forEach { tone ->
                StatusPill(text = tone.name, tone = tone, showDot = false)
            }
        }

        Section("SeverityBadge") {
            listOf("INFO", "WARNING", "HIGH", "CRITICAL").forEach { level ->
                SeverityBadge(level = level)
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

        Section("SectionHeader") {
            SectionHeader(title = "Aplicaciones", actionLabel = "Ver todas", onAction = {})
        }

        Section("EmptyState") {
            EmptyState(
                icon = NpIcons.Smartphone,
                title = "Sin dispositivos",
                message = "Vincula un dispositivo para empezar.",
                actionLabel = "Vincular dispositivo",
                onAction = {},
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

        Section("NpBottomBar") {
            NpBottomBar(
                items = listOf(
                    NpBottomItem("Inicio", NpIcons.Home),
                    NpBottomItem("Dispositivos", NpIcons.Smartphone, badgeCount = 3),
                    NpBottomItem("Actividad", NpIcons.Clock),
                    NpBottomItem("Más", NpIcons.MoreHorizontal),
                ),
                selectedIndex = bottomIndex,
                onSelect = { bottomIndex = it },
            )
        }

        Section("SegmentedControl") {
            SegmentedControl(
                options = listOf("Hoy", "7 días", "30 días"),
                selectedIndex = segmentedIndex,
                onSelect = { segmentedIndex = it },
            )
        }

        Section("FilterChips") {
            FilterChips(
                options = listOf("Todas", "Redes sociales", "Juegos", "Streaming"),
                selectedIndex = chipsIndex,
                onSelect = { chipsIndex = it },
            )
        }

        Section("UsageBar") {
            UsageBar(fraction = 0f, contentDescription = "0 %")
            UsageBar(fraction = 0.4f, contentDescription = "40 %")
            UsageBar(fraction = 1f, contentDescription = "100 %")
        }

        Section("TimelineItem") {
            TimelineItem(
                icon = NpIcons.Ban,
                tone = NpTone.Danger,
                title = "Bloqueo de app",
                time = "10:42 a. m.",
                subtitle = "YouTube",
            )
            TimelineItem(
                icon = NpIcons.MapPin,
                tone = NpTone.Info,
                title = "Salió de la geocerca",
                time = "9:15 a. m.",
                subtitle = "Zona escolar",
            )
            TimelineItem(
                icon = NpIcons.Clock,
                tone = NpTone.Success,
                title = "Se alcanzó el límite",
                time = "8:03 a. m.",
                subtitle = "Instagram",
                isLast = true,
            )
        }

        Section("DayHeader") {
            DayHeader(text = "Hoy")
            DayHeader(text = "Ayer")
        }

        Section("CodeDisplay") {
            CodeDisplay(code = "482 917", remainingSeconds = 143, totalSeconds = 180)
            CodeDisplay(code = "482 917", remainingSeconds = 0, totalSeconds = 180)
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
