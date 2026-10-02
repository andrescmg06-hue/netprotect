package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Banda que se ve encima de toda pantalla supervisada mientras se comparte la pantalla, además de
 * la notificación de Android. Dibuja su propio relleno de barra de estado. */
@Composable
fun ActiveShareBanner(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NpColors.SignalBlue)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(NpIcons.Eye),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = NpColors.PaperWhite,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Tu tutor está viendo esta pantalla",
            style = NpText.BodyStrong,
            color = NpColors.PaperWhite,
            modifier = Modifier.weight(1f),
        )
        NpButton("Detener", onStop, variant = NpButtonVariant.Secondary, small = true)
    }
}
