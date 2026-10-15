package com.netprotect.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Shown when the signed-in account is a tutor (TUTOR without SUPERVISADO). The app is only for
 * the minor's phone, so the role is never granted silently: the user either confirms this phone
 * is a supervised one or signs out. Presentation only: receives values and callbacks. */
@Composable
fun TutorAccountScreen(
    loading: Boolean,
    error: String?,
    onUseAsSupervised: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeBackdrop(modifier) {
        BrandHeader(stacked = true)
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Esta app es para el teléfono del menor",
            style = NpText.Display,
            color = NpColors.Ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tu cuenta es de tutor. Para ver y configurar los dispositivos que supervisas, " +
                "usa el panel web de NetProtect.",
            style = NpText.Body,
            color = NpColors.InkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        NpButton(
            text = "Usar este teléfono como supervisado",
            onClick = onUseAsSupervised,
            modifier = Modifier.fillMaxWidth(),
            loading = loading,
        )
        if (error != null) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = error,
                style = NpText.Body,
                color = NpColors.DangerText,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
            )
        }
        Spacer(Modifier.height(16.dp))
        NpButton(
            text = "Cerrar sesión",
            onClick = onSignOut,
            variant = NpButtonVariant.Text,
            icon = NpIcons.LogOut,
            enabled = !loading,
        )
    }
}

/** The role check (`GET`/`POST /users/me/roles`) failed: the error with "Reintentar", plus a way
 * out, since without the role the supervised mode cannot work. */
@Composable
fun RoleCheckFailedScreen(
    message: String,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HomeBackdrop(modifier) {
        BrandHeader(stacked = true)
        Spacer(Modifier.height(24.dp))
        ErrorState(message = message, onRetry = onRetry)
        NpButton(
            text = "Cerrar sesión",
            onClick = onSignOut,
            variant = NpButtonVariant.Text,
            icon = NpIcons.LogOut,
        )
    }
}

@Composable
private fun HomeBackdrop(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.Cream),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}
