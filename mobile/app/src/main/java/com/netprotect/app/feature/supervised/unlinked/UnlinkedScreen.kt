package com.netprotect.app.feature.supervised.unlinked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/**
 * Sprint 64 (B1, lámina 10.4): the tutor ended the link. Distinct from never having been linked: the
 * shell shows it only after a server answer said so. It does not say who or when — that data does not
 * exist on the phone. «Cerrar sesión» asks nothing here: no protection is running any more.
 *
 * Provisional body (T4 restyles it); the signature is final.
 */
@Composable
fun UnlinkedScreen(
    onRelink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Text(text = "VÍNCULO TERMINADO", style = NpText.Eyebrow, color = NpColors.InkMuted)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Este dispositivo ya no está vinculado",
            style = NpText.Display,
            color = NpColors.Ink,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Tu tutor terminó el vínculo. Si fue un error, pídele un código nuevo para vincularlo otra vez.",
            style = NpText.Body,
            color = NpColors.InkMuted,
        )
        Spacer(Modifier.height(32.dp))
        NpButton(
            text = "Vincular de nuevo",
            onClick = onRelink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        NpButton(
            text = "Cerrar sesión",
            onClick = onSignOut,
            modifier = Modifier.fillMaxWidth(),
            variant = NpButtonVariant.Secondary,
        )
    }
}
