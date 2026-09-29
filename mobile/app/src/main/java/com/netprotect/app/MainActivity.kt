package com.netprotect.app

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.netprotect.app.feature.home.HomeScreen
import com.netprotect.app.ui.theme.NetProtectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Sprint 42: the design system is light-only, so the system bars are transparent with dark
        // icons. Screens that are still on the old dark look (until their sprint) show them poorly;
        // accepted on the integration branch, not on main.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            NetProtectTheme {
                HomeScreen()
            }
        }
    }
}
