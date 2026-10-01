package com.netprotect.app.feature.supervised.consent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/**
 * INTERIM (Sprint 50, Claude): plain but complete, so a screen-share request can be answered
 * between commits. DeepSeek replaces this body with the mockup 15 design; the signature is final.
 *
 * Shown by SupervisedShell only while the tutor's request is pending. [onContinue] and [onDecline]
 * send exactly what the old card sent (screen_share_consent true/false) and [onContinue] opens
 * Android's own capture dialog — nothing starts without that dialog. "Continuar" is only enabled
 * after the box is ticked (D-12 a: a second, explicit gesture). The box starts unticked for every
 * request: the consent is for this session only, never a standing permission.
 */
@Composable
fun ScreenShareConsentScreen(
    onContinue: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var authorized by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Consentimiento para ver la pantalla", style = NpText.Display, color = NpColors.ShieldNavy)
        Text("Tu tutor pidió ver la pantalla de este dispositivo ahora.", style = NpText.Body)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = authorized, onCheckedChange = { authorized = it })
            Text("Autorizo a mi tutor a ver la pantalla de este dispositivo ahora", style = NpText.BodyStrong)
        }
        NpButton("Continuar", onContinue, enabled = authorized)
        NpButton("Ahora no", onDecline, variant = NpButtonVariant.Secondary)
    }
}
