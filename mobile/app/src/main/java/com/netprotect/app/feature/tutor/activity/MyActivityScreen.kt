package com.netprotect.app.feature.tutor.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.netprotect.app.ui.components.NpButton
import com.netprotect.app.ui.components.NpButtonVariant
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpColors
import com.netprotect.app.ui.theme.NpText

/** INTERIM (Sprint 47, Claude): plain but complete, so the tab keeps working between commits.
 * DeepSeek replaces this body with the redesign; the signature is final. A tab root: no back. */
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
    Column(
        modifier = modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Mi actividad", style = NpText.Display, color = NpColors.ShieldNavy)
        when (activity) {
            LoadState.Loading -> Text("Cargando…", style = NpText.Body)
            is LoadState.Failed -> {
                Text(activity.message, style = NpText.Body, color = NpColors.DangerText)
                NpButton("Reintentar", onRefresh, variant = NpButtonVariant.Secondary)
            }
            is LoadState.Loaded -> {
                if (activity.value.isEmpty()) Text("Sin acciones registradas todavía.", style = NpText.Body)
                activity.value.forEach { day ->
                    Text(listOfNotNull(day.title, day.subtitle).joinToString(" · "), style = NpText.Title)
                    day.rows.forEach { row ->
                        Text(listOfNotNull(row.time, row.title, row.subtitle).joinToString(" — "), style = NpText.Body)
                    }
                }
                moreError?.let { Text(it, style = NpText.Body, color = NpColors.DangerText) }
                if (hasMore) NpButton("Cargar más", onLoadMore, variant = NpButtonVariant.Secondary, loading = loadingMore)
            }
        }
    }
}
