package com.netprotect.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.theme.NetProtectTheme
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Visual state of [NpCodeField] (lámina 02): typing, sending the code, or the code was refused. */
enum class NpCodeFieldState { Editing, Sending, Error }

/** Number of boxes of the pairing code. */
const val NP_CODE_LENGTH = 6

/** What a code box shows: its digit, or nothing yet. Pure, so the paste behaviour is tested on the JVM. */
fun codeDigitAt(value: String, index: Int): Char? = value.getOrNull(index)?.takeIf(Char::isDigit)

/** The box with the caret: the first empty one while editing; none when full, sending or refused. */
fun activeCodeBox(value: String, state: NpCodeFieldState, length: Int = NP_CODE_LENGTH): Int? =
    if (state == NpCodeFieldState.Editing && value.length < length) value.length else null

/**
 * Code field, v2: six 4 dp-radius boxes with serif digits mirroring one hidden [BasicTextField] with the
 * numeric keyboard, so pasting the six digits at once fills every box. Only digits are kept and anything
 * past [length] is cut. While [NpCodeFieldState.Sending] the boxes turn blue-washed and the field does
 * not accept input; [NpCodeFieldState.Error] draws them in red. Boxes have a minimum height only, so the
 * digits grow with the font scale instead of being clipped.
 */
@Composable
fun NpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    state: NpCodeFieldState = NpCodeFieldState.Editing,
    length: Int = NP_CODE_LENGTH,
) {
    val active = activeCodeBox(value, state, length)
    Box(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { },
        ) {
            for (i in 0 until length) {
                val digit = codeDigitAt(value, i)
                val (background, border) = when {
                    state == NpCodeFieldState.Error -> NpColors.Paper to NpColors.DangerText
                    state == NpCodeFieldState.Sending -> NpColors.BlueWash to NpColors.Blue
                    i == active -> NpColors.Paper to NpColors.Blue
                    else -> NpColors.Paper to NpColors.HairlineStrong
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 60.dp)
                        .background(background, NpShapes.Field)
                        .border(
                            width = if (i == active) 2.dp else 1.dp,
                            color = border,
                            shape = NpShapes.Field,
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (digit != null) {
                        Text(text = digit.toString(), style = NpText.Title, color = NpColors.Ink)
                    }
                }
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { raw -> onValueChange(raw.filter(Char::isDigit).take(length)) },
            modifier = Modifier
                .matchParentSize()
                .alpha(0f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = state != NpCodeFieldState.Sending,
            singleLine = true,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F3EE)
@Composable
private fun NpCodeFieldPreview() {
    NetProtectTheme {
        NpCodeField(value = "482", onValueChange = {})
    }
}
