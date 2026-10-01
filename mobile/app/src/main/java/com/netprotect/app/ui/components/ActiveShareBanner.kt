package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** INTERIM (Sprint 50, Claude): the band SupervisedShell shows at the top of every supervised
 * screen while a screen share is really running, in addition to Android's own notification.
 * It owns the status bar inset (the screen below it no longer needs one). DeepSeek restyles it;
 * the signature is final. */
@Composable
fun ActiveShareBanner(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NpColors.SignalBlue)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Tu tutor está viendo esta pantalla",
            style = NpText.BodyStrong,
            color = NpColors.PaperWhite,
            modifier = Modifier.weight(1f),
        )
        NpButton("Detener", onStop, variant = NpButtonVariant.Secondary, small = true)
    }
}
