package com.netprotect.app.feature.supervised.link

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
import com.netprotect.app.feature.supervised.isCompletePairingCode
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.OtpInput
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** INTERIM (Sprint 48, Claude): plain but complete, so linking keeps working between commits.
 * DeepSeek replaces this body with the redesign; the signature is final. */
@Composable
fun LinkDeviceScreen(
    code: String,
    onCodeChange: (String) -> Unit,
    linking: Boolean,
    error: String?,
    rechecking: Boolean,
    onLink: () -> Unit,
    onCheckLink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Vincular este dispositivo", style = NpText.Display, color = NpColors.ShieldNavy)
        Text("Ingresa el código de 6 dígitos que te dio tu tutor.", style = NpText.Body)
        OtpInput(value = code, onValueChange = onCodeChange)
        error?.let { Text(it, style = NpText.Body, color = NpColors.DangerText) }
        NpButton("Vincular dispositivo", onLink, enabled = isCompletePairingCode(code) && !linking, loading = linking)
        NpButton("Comprobar vínculo", onCheckLink, variant = NpButtonVariant.Secondary, loading = rechecking)
        NpButton("Cerrar sesión", onSignOut, variant = NpButtonVariant.Text)
    }
}
