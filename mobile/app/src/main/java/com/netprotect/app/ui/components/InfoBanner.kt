package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Banner informativo: fondo `tone.wash`, borde suave, icono a la izquierda, título y texto. */
@Composable
fun InfoBanner(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    tone: NpTone = NpTone.Info,
    @DrawableRes icon: Int = NpIcons.Info,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(tone.wash, NpShapes.Lg)
            .border(1.dp, tone.solid.copy(alpha = 0.2f), NpShapes.Lg)
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = tone.text,
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = title, style = NpText.BodyStrong, color = tone.text)
            Spacer(Modifier.height(4.dp))
            Text(text = text, style = NpText.Body, color = tone.text)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun InfoBannerPreview() {
    NetProtectTheme {
        InfoBanner(title = "Aviso", text = "La ubicación se reporta cada ~15 minutos.")
    }
}
