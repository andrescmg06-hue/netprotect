package com.netprotect.app.feature.tutor.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.BrandHeader
import com.netprotect.app.ui.components.EmptyState
import com.netprotect.app.ui.components.ErrorState
import com.netprotect.app.ui.components.IconTile
import com.netprotect.app.ui.components.InfoBanner
import com.netprotect.app.ui.components.LoadingState
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.components.NpCard
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** Pestaña "Actividad": el registro de auditoría del propio tutor (solo lectura). */
@Composable
fun MyActivityScreen(
    activity: LoadState<List<ActivityDay>>,
    hasMore: Boolean,
    loadingMore: Boolean,
    moreError: String?,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NpColors.SkyGround),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            BrandHeader(subtitle = "Panel del tutor")
            Spacer(Modifier.height(24.dp))
            Text(text = "Mi actividad", style = NpText.Display, color = NpColors.ShieldNavy)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Registro de las acciones que has realizado en NetProtect (app y panel web). Esta información es de solo lectura.",
                style = NpText.Body,
                color = NpColors.SlateMuted,
            )
            Spacer(Modifier.height(16.dp))
            when (activity) {
                LoadState.Loading -> LoadingState()
                is LoadState.Failed -> ErrorState(message = activity.message, onRetry = onRefresh)
                is LoadState.Loaded -> {
                    if (activity.value.isEmpty()) {
                        EmptyState(
                            icon = NpIcons.History,
                            title = "Sin acciones registradas todavía.",
                            message = "Aquí aparecerán las acciones que realices desde la app o el panel web.",
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            activity.value.forEach { day -> DayCard(day) }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (moreError != null) {
                        Text(text = moreError, style = NpText.Body, color = NpColors.DangerText)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (hasMore) {
                        NpButton(
                            text = "Cargar más",
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth(),
                            variant = NpButtonVariant.Secondary,
                            loading = loadingMore,
                            enabled = !loadingMore,
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            InfoBanner(
                title = "Información importante",
                text = "Este registro no se puede editar ni borrar. No incluye la actividad del dispositivo supervisado.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DayCard(day: ActivityDay) {
    NpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = day.title, style = NpText.Title, color = NpColors.ShieldNavy)
            if (day.subtitle != null) {
                Spacer(Modifier.width(8.dp))
                Text(text = day.subtitle, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            day.rows.forEach { row -> ActivityRowView(row) }
        }
    }
}

@Composable
private fun ActivityRowView(row: ActivityRow) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = row.time,
            style = NpText.Caption.copy(fontWeight = FontWeight.Normal),
            color = NpColors.SlateMuted,
            modifier = Modifier.width(72.dp),
        )
        Spacer(Modifier.width(8.dp))
        IconTile(icon = row.icon, tone = row.tone, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = row.title, style = NpText.BodyStrong, color = NpColors.ShieldNavy)
            if (row.subtitle != null) {
                Text(text = row.subtitle, style = NpText.Body, color = NpColors.SlateMuted)
            }
        }
    }
}
