package com.netprotect.app.feature.tutor.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.auth.TokenSession
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.network.AuditClient
import com.netprotect.app.core.network.AuditLogEntry
import com.netprotect.app.core.network.toUiError
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/* Sprint 44: the seven sections of the pre-redesign TutorScreen.kt were moved here VERBATIM and
 * are removed one by one as Sprints 45–47 redesign them. After Sprint 46 this file only holds the
 * tutor's own audit log (the "Actividad" tab), which Sprint 47 will redesign and remove. */

private sealed interface AuditState {
    data object Loading : AuditState
    // Account-level, not per-device (backend/app/api/v1/endpoints/audit.py scopes it to
    // actor_user_id == current_user.id, not to a device) — the tutor's own audited actions.
    // Read-only and filter-less here, same split as history/alerts: filtering and CSV export
    // live only in the web panel.
    data class Loaded(val logs: List<AuditLogEntry>) : AuditState
    data class Error(val message: String) : AuditState
}

/** Sprint 22, read-only and account-level (not per-device): the tutor's own audited actions
 * (backend scopes GET /users/me/audit to actor_user_id == current_user.id). No filters or CSV
 * export here — those live only in the web panel, same split already used for historial/
 * alertas/geocercas.
 */
@Composable
private fun AuditSection(state: AuditState) {
    when (state) {
        AuditState.Loading -> Text("Cargando auditoría…", color = Color(0xFFABB5C4), fontSize = 13.sp)
        is AuditState.Error -> Text(state.message, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        is AuditState.Loaded -> {
            if (state.logs.isEmpty()) {
                Text("Sin acciones registradas todavía.", color = Color(0xFFABB5C4), fontSize = 13.sp)
            } else {
                Column {
                    state.logs.forEach { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            val resource = entry.resourceType?.let { " · $it" } ?: ""
                            Text("${entry.action}$resource", color = Color.White, fontSize = 13.sp)
                            Text(formatCapturedAt(entry.createdAt), color = Color(0xFF7D899A), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun formatCapturedAt(isoInstant: String): String = runCatching {
    Instant.parse(isoInstant)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}.getOrDefault(isoInstant)

@Composable
private fun LegacyFrame(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
    ) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(16.dp))
        content()
    }
}

/** The tutor's own audit log (tab "Actividad"), pre-redesign look until Sprint 47. */
@Composable
fun LegacyActivityScreen(baseUrl: String, session: TokenSession) {
    val client = remember { AuditClient(baseUrl) }
    var state by remember { mutableStateOf<AuditState>(AuditState.Loading) }
    LaunchedEffect(Unit) {
        state = try {
            AuditState.Loaded(session.authorized { token -> client.listMyAuditLog(token) })
        } catch (exception: Exception) {
            AuditState.Error(exception.toUiError().message)
        }
    }
    LegacyFrame("Mi actividad (auditoría)") { AuditSection(state) }
}
