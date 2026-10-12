package com.netprotect.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Tema del diseño (solo claro). Claude envuelve `MainActivity` en este tema en la revisión
 * siguiente; por ahora ninguna pantalla lo usa. */
@Composable
fun NetProtectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NetProtectColorScheme,
        typography = NetProtectTypography,
        shapes = NetProtectShapes,
        content = content,
    )
}
