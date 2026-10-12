package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.format.AlertLabels
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Nivel de alerta en píldora: icono + texto + color (nunca solo color). */
@Composable
fun SeverityBadge(level: String, modifier: Modifier = Modifier) {
    val tone = AlertLabels.levelTone(level)
    Row(
        modifier = modifier
            .height(24.dp)
            .background(tone.wash, NpShapes.Pill)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(AlertLabels.levelIcon(level)),
            contentDescription = null,
            modifier = Modifier.size(13.dp),
            tint = tone.text,
        )
        Spacer(Modifier.width(6.dp))
        Text(text = AlertLabels.levelLabel(level), style = NpText.Caption, color = tone.text)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun SeverityBadgePreview() {
    NetProtectTheme {
        SeverityBadge(level = "CRITICAL")
    }
}
