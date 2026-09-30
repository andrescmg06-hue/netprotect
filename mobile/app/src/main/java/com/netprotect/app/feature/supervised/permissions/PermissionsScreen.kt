package com.netprotect.app.feature.supervised.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.supervised.PermissionsUi
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** INTERIM (Sprint 48, Claude): plain but complete. DeepSeek replaces this body; the signature is
 * final. The shell re-reads every permission when the app comes back from Settings. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        NpButton("Atrás", onBack, variant = NpButtonVariant.Text)
        Text("Permisos del dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
        Row("Acceso al uso de apps", permissions.usageAccess)
        if (!permissions.usageAccess) {
            NpButton("Abrir Ajustes", onOpenUsageAccessSettings)
            NpButton("Ya lo activé, verificar de nuevo", onRecheckUsageAccess, variant = NpButtonVariant.Text)
        }
        Row("Ubicación aproximada", permissions.location)
        if (!permissions.location) {
            NpButton("Permitir ubicación aproximada", onRequestLocation)
            NpButton("Ya lo activé, verificar de nuevo", onRecheckLocation, variant = NpButtonVariant.Text)
        }
        Row("Mostrar sobre otras apps", permissions.overlay)
        if (!permissions.overlay) {
            NpButton("Abrir Ajustes", onRequestOverlay)
            NpButton("Ya lo activé, verificar de nuevo", onRecheckOverlay, variant = NpButtonVariant.Text)
        }
        Row("Protección contra desinstalación", permissions.deviceAdmin)
        if (!permissions.deviceAdmin) NpButton("Activar protección", onRequestDeviceAdmin)
    }
}

@Composable
private fun Row(title: String, granted: Boolean) {
    Text("$title — ${if (granted) "Configurado" else "Pendiente"}", style = NpText.BodyStrong)
}
