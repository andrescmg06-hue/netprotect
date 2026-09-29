package com.netprotect.app.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.feature.home.LoadingScreen
import com.netprotect.app.feature.home.LoginScreen
import com.netprotect.app.feature.home.RoleSelectionScreen
import com.netprotect.app.feature.home.ServiceStatus
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpShapes
import com.netprotect.app.ui.theme.NpText

/** Galería de pantallas (solo debug): cada pantalla en cada estado, dentro de un marco de 760 dp
 * de alto, con su título encima. Datos ilustrativos; nada de esto se usa fuera de `src/debug`. */
@Composable
fun ScreensGallery() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        ScreenFrame("LoadingScreen") {
            LoadingScreen()
        }
        ScreenFrame("LoginScreen · Checking") {
            LoginScreen(error = null, service = ServiceStatus.Checking, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Ready") {
            LoginScreen(error = null, service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Unavailable") {
            LoginScreen(error = null, service = ServiceStatus.Unavailable, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("LoginScreen · Ready + error") {
            LoginScreen(error = "No se pudo iniciar sesión", service = ServiceStatus.Ready, onSignIn = {}, onRetryService = {})
        }
        ScreenFrame("RoleSelectionScreen · con nombre") {
            RoleSelectionScreen(
                displayName = "Andrés Mosquera",
                email = "tutor@example.com",
                error = null,
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("RoleSelectionScreen · sin nombre") {
            RoleSelectionScreen(
                displayName = null,
                email = "tutor@example.com",
                error = null,
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
        ScreenFrame("RoleSelectionScreen · con error") {
            RoleSelectionScreen(
                displayName = "Andrés Mosquera",
                email = "tutor@example.com",
                error = "No se pudo guardar el modo",
                onSelectTutor = {},
                onSelectSupervised = {},
                onSignOut = {},
            )
        }
    }
}

@Composable
private fun ScreenFrame(title: String, content: @Composable () -> Unit) {
    Column {
        Text(text = title, style = NpText.Headline, color = NpColors.ShieldNavy)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(760.dp)
                .background(NpColors.SkyGround)
                .border(1.dp, NpColors.Hairline, NpShapes.Lg),
            contentAlignment = Alignment.TopCenter,
        ) {
            content()
        }
    }
}
