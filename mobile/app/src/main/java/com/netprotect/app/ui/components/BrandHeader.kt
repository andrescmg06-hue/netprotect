package com.netprotect.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Marca: escudo + "Net"/"Protect" (Title 700 ×1.4) + subtítulo opcional en mayúsculas. */
@Composable
fun BrandHeader(
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(NpIcons.LogoShield),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Row {
                val brandStyle = NpText.Title.copy(fontWeight = FontWeight.Bold, lineHeight = 25.2.sp)
                Text(text = "Net", style = brandStyle, color = NpColors.ShieldNavy)
                Text(text = "Protect", style = brandStyle, color = NpColors.SignalBlue)
            }
            if (subtitle != null) {
                Text(
                    text = subtitle.uppercase(),
                    style = NpText.Caption.copy(letterSpacing = 0.2.em),
                    color = NpColors.SlateMuted,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun BrandHeaderPreview() {
    NetProtectTheme {
        BrandHeader(subtitle = "Panel del tutor")
    }
}
