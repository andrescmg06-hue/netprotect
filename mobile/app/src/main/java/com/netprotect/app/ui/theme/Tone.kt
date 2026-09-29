package com.netprotect.app.ui.theme

import androidx.compose.ui.graphics.Color

/** Tono semántico (sólido / lavado / texto) para badges y métricas. Se elige por significado, no
 * para decorar, y nunca es el único portador de información (siempre va con etiqueta de texto). */
enum class NpTone(val solid: Color, val wash: Color, val text: Color) {
    Success(NpColors.Success, NpColors.SuccessWash, NpColors.SuccessText),
    Danger(NpColors.Danger, NpColors.DangerWash, NpColors.DangerText),
    Warning(NpColors.Warning, NpColors.WarningWash, NpColors.WarningText),
    Info(NpColors.SignalBlue, NpColors.BlueWash, NpColors.SignalBlueDeep),
    Purple(NpColors.Violet, NpColors.VioletWash, NpColors.VioletText),
    Neutral(NpColors.SlateMuted, NpColors.NeutralWash, NpColors.SlateMuted),
}
