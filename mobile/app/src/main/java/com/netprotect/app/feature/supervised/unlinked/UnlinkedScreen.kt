package com.netprotect.app.feature.supervised.unlinked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import com.netprotect.app.ui.components.NpSeal
import com.netprotect.app.ui.components.SEAL_ARC_COUNT
import com.netprotect.app.ui.components.SealArcState
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/**
 * Sprint 64 (B1, lámina 10.4): the tutor ended the link. Distinct from never having been linked: the
 * shell shows it only after a server answer said so. It does not say who or when — that data does not
 * exist on the phone. «Cerrar sesión» asks nothing here: no protection is running any more.
 */
@Composable
fun UnlinkedScreen(
    onRelink: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream),
    ) {
        val screenHeight = maxHeight
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // At least one screen tall so the buttons sit at the bottom; with a large font they follow the text.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = screenHeight)
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.Xxl)) {
                    // Every arc dormant: nothing is supervised any more. Decorative, the text says it.
                    NpSeal(
                        states = List(SEAL_ARC_COUNT) { SealArcState.Inactive },
                        inactiveColor = NpColors.Ink,
                    )
                    Spacer(Modifier.height(NpSpace.Xl))
                    Text(text = "VÍNCULO TERMINADO", style = NpText.Eyebrow, color = NpColors.InkMuted)
                    Spacer(Modifier.height(NpSpace.M))
                    Text(
                        text = "Este dispositivo ya no está vinculado",
                        style = NpText.Display,
                        color = NpColors.Ink,
                        modifier = Modifier.semantics { heading() },
                    )
                    Spacer(Modifier.height(NpSpace.M))
                    Text(
                        text = "Tu tutor terminó el vínculo. Si fue un error, pídele un código nuevo para vincularlo otra vez.",
                        style = NpText.Body,
                        color = NpColors.InkMuted,
                    )
                }
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(NpColors.Hairline),
                    )
                    Column(Modifier.padding(horizontal = NpSpace.Gutter, vertical = NpSpace.L)) {
                        NpButton(
                            text = "Vincular de nuevo",
                            onClick = onRelink,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = NpIcons.ArrowRight,
                        )
                        Spacer(Modifier.height(NpSpace.M))
                        NpButton(
                            text = "Cerrar sesión",
                            onClick = onSignOut,
                            modifier = Modifier.fillMaxWidth(),
                            variant = NpButtonVariant.Secondary,
                        )
                    }
                }
            }
        }
    }
}
