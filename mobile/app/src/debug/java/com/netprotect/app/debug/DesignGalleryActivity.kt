package com.netprotect.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.netprotect.app.ui.theme.NetProtectTheme

/** Actividad de la galería de componentes. Solo existe en builds debug; se abre con
 * `adb shell am start -n com.netprotect.app/com.netprotect.app.debug.DesignGalleryActivity`. */
class DesignGalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetProtectTheme {
                DesignGallery()
            }
        }
    }
}
