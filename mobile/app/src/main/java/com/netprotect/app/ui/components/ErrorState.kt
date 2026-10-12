package com.netprotect.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Estado de error: icono `CircleAlert` en `Danger`, mensaje y botón Secondary "Reintentar". */
@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(NpIcons.CircleAlert),
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = NpColors.Danger,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = NpText.Body,
            color = NpColors.SlateMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        NpButton(text = "Reintentar", onClick = onRetry, variant = NpButtonVariant.Secondary)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun ErrorStatePreview() {
    NetProtectTheme {
        ErrorState(message = "No se pudo cargar.", onRetry = {})
    }
}
