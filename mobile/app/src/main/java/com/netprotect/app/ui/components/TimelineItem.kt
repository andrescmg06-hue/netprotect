package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText
import com.netprotect.app.ui.theme.NpTone

/** Entrada de línea de tiempo: icono en círculo `tone.wash`, línea vertical `Hairline` hasta el
 * siguiente (oculta si `isLast`), título, hora y subtítulo opcional. */
@Composable
fun TimelineItem(
    @DrawableRes icon: Int,
    tone: NpTone,
    title: String,
    time: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    isLast: Boolean = false,
) {
    Row(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(tone.wash, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = tone.text,
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(48.dp)
                        .background(NpColors.Hairline),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = title,
                    style = NpText.BodyStrong,
                    color = NpColors.Ink,
                    modifier = Modifier.weight(1f),
                )
                Text(text = time, style = NpText.Caption, color = NpColors.SlateMuted)
            }
            if (subtitle != null) {
                Text(text = subtitle, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun TimelineItemPreview() {
    NetProtectTheme {
        TimelineItem(
            icon = NpIcons.Clock,
            tone = NpTone.Info,
            title = "Bloqueo de app",
            time = "10:42 a. m.",
            subtitle = "YouTube",
        )
    }
}
