package com.netprotect.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpTone

/** Baldosa redondeada (radio 10–12 dp) con el lavado del tono y el icono en su color de texto. */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    tone: NpTone = NpTone.Info,
    size: Dp = 48.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(tone.wash, NpShapes.Md),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            modifier = Modifier.size(size * 0.5f),
            tint = tone.text,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun IconTilePreview() {
    NetProtectTheme {
        IconTile(icon = NpIcons.Smartphone, tone = NpTone.Info, contentDescription = "Dispositivo")
    }
}
