package com.netprotect.app.feature.tutor.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** INTERIM (Sprint 44, Claude): plain but complete, so no tutor function disappears between
 * commits. DeepSeek replaces this body with the redesigned screen; the signature is final. */
@Composable
fun MoreScreen(
    appVersion: String,
    onSwitchMode: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Más", style = NpText.Display, color = NpColors.ShieldNavy)
        NpButton("Cambiar de modo", onSwitchMode, variant = NpButtonVariant.Secondary)
        NpButton("Cerrar sesión", onSignOut, variant = NpButtonVariant.Text)
        Text("NetProtect $appVersion", style = NpText.Body, color = NpColors.SlateMuted)
    }
}
