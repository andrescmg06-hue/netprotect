package com.netprotect.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Barra superior: botón atrás opcional + título o, si no hay título, el `BrandHeader`. */
@Composable
fun NpTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    title: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(NpIcons.ArrowLeft),
                    contentDescription = "Atrás",
                    tint = NpColors.ShieldNavy,
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        if (title != null) {
            Text(
                text = title,
                style = NpText.Title,
                color = NpColors.ShieldNavy,
                modifier = Modifier.weight(1f),
            )
        } else {
            BrandHeader(modifier = Modifier.weight(1f))
        }
        trailing?.invoke()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun NpTopBarPreview() {
    NetProtectTheme {
        NpTopBar(onBack = {}, title = "Dispositivos")
    }
}
