package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpSpace
import com.netprotect.app.ui.theme.NpText

/** Banda que se ve encima de toda pantalla supervisada mientras se comparte la pantalla, además de
 * la notificación de Android (lámina 03, estado 4). Dibuja su propio relleno de barra de estado. */
@Composable
fun ActiveShareBanner(onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NpColors.Blue)
            .statusBarsPadding()
            .padding(horizontal = NpSpace.L, vertical = NpSpace.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(NpColors.Paper, CircleShape),
        )
        Spacer(Modifier.width(NpSpace.M))
        Text(
            text = "Tu tutor está viendo tu pantalla",
            style = NpText.BodyStrong,
            color = NpColors.Paper,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(NpSpace.M))
        // White outline on blue: none of NpButton's variants reads on this background.
        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .clip(NpShapes.Field)
                .border(1.5.dp, NpColors.Paper, NpShapes.Field)
                .clickable(role = Role.Button, onClick = onStop)
                .padding(horizontal = NpSpace.L, vertical = NpSpace.S),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "Detener", style = NpText.Label, color = NpColors.Paper)
        }
    }
}
