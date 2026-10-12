package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Entrada de código (OTP): casillas visibles que reflejan un único `BasicTextField` oculto con
 * teclado numérico. Solo dígitos, máximo `length`, acepta pegar (filtra a dígitos y recorta). */
@Composable
fun OtpInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    enabled: Boolean = true,
    isError: Boolean = false,
) {
    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            for (i in 0 until length) {
                val digit = value.getOrNull(i)
                val isActive = i == value.length && enabled && !isError
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(NpColors.PaperWhite, NpShapes.Md)
                        .border(
                            width = 2.dp,
                            color = when {
                                isError -> NpColors.Danger
                                isActive -> NpColors.SignalBlue
                                else -> NpColors.HairlineStrong
                            },
                            shape = NpShapes.Md,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (digit != null) {
                        Text(text = digit.toString(), style = NpText.NumericSmall, color = NpColors.Ink)
                    }
                }
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() }.take(length)) },
            modifier = Modifier
                .matchParentSize()
                .alpha(0f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = enabled,
            singleLine = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F9FF)
@Composable
private fun OtpInputPreview() {
    NetProtectTheme {
        OtpInput(value = "482", onValueChange = {})
    }
}
