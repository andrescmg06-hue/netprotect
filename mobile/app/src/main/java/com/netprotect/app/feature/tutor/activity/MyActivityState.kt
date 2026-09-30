package com.netprotect.app.feature.tutor.activity

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.netprotect.app.core.network.AuditLogEntry
import com.netprotect.app.core.network.AuditPage
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.ui.format.AuditLabels
import com.netprotect.app.ui.format.Clock
import com.netprotect.app.ui.format.DayGrouping
import com.netprotect.app.ui.state.LoadState
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.coroutines.cancellation.CancellationException

/** Sprint 47: one row of Mi actividad — only what the audit log stores (action, resource, time). */
data class ActivityRow(
    val id: String,
    val icon: Int,
    val tone: NpTone,
    /** "Ubicación consultada". */
    val title: String,
    /** "Tablet de Sofía", "Dispositivo", "Alerta"… or null when the action names no resource. */
    val subtitle: String?,
    /** "3:42 p. m." — the day is in the group's header. */
    val time: String,
)

/** "Hoy" + "29 de septiembre de 2026", "Ayer" + date, or only the date. */
data class ActivityDay(val date: LocalDate, val title: String, val subtitle: String?, val rows: List<ActivityRow>)

/** Groups by the day in the tutor's zone, newest first. Unparseable times are dropped. */
fun activityDays(
    entries: List<AuditLogEntry>,
    deviceNames: Map<String, String>,
    today: LocalDate,
    zone: ZoneId,
): List<ActivityDay> = entries
    .mapNotNull { entry -> runCatching { Instant.parse(entry.createdAt) }.getOrNull()?.let { it to entry } }
    .sortedByDescending { it.first }
    .groupBy { it.first.atZone(zone).toLocalDate() }
    .map { (date, rows) ->
        ActivityDay(
            date = date,
            title = DayGrouping.label(date, today),
            subtitle = if (date == today || date == today.minusDays(1)) DayGrouping.fullDate(date) else null,
            rows = rows.map { (instant, entry) ->
                val family = AuditLabels.family(entry.action)
                ActivityRow(
                    id = entry.id,
                    icon = family.icon,
                    tone = family.tone,
                    title = AuditLabels.auditActionLabel(entry.action),
                    subtitle = AuditLabels.resourceLabel(entry.resourceType, entry.resourceId, deviceNames),
                    time = Clock.format(instant.atZone(zone).toLocalTime()),
                )
            },
        )
    }

/**
 * Sprint 47 (D-02): Mi actividad with "Cargar más". The log is newest-first and grows while the
 * tutor reads it (every action they take adds a row at the top), so offsets shift: a later page can
 * repeat entries already shown. They are dropped by id instead of showing the same action twice.
 * A failed "Cargar más" keeps what is shown and says why ([moreError]); a failed first load is the
 * screen's error state.
 */
class MyActivityController(private val loadPage: suspend (offset: Int) -> AuditPage) {
    var state: LoadState<List<AuditLogEntry>> by mutableStateOf(LoadState.Loading)
        private set
    var total: Int by mutableStateOf(0)
        private set
    var loadingMore: Boolean by mutableStateOf(false)
        private set
    var moreError: String? by mutableStateOf(null)
        private set

    val hasMore: Boolean
        get() = (state as? LoadState.Loaded)?.value?.let { it.size < total } ?: false

    suspend fun refresh() {
        state = LoadState.Loading
        moreError = null
        state = try {
            val page = loadPage(0)
            total = page.total
            LoadState.Loaded(page.entries.distinctBy { it.id })
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            val error = exception.toUiError()
            LoadState.Failed(error.message, notFound = error == UiError.NotFound)
        }
    }

    suspend fun loadMore() {
        val shown = (state as? LoadState.Loaded)?.value ?: return
        if (loadingMore || !hasMore) return
        loadingMore = true
        moreError = null
        try {
            val page = loadPage(shown.size)
            val known = shown.mapTo(HashSet()) { it.id }
            val added = page.entries.filter { it.id !in known }
            total = page.total
            // Nothing new although the server says there is more: stop offering the button
            // instead of looping on the same page.
            if (added.isEmpty()) total = shown.size
            state = LoadState.Loaded(shown + added)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (exception: Exception) {
            moreError = exception.toUiError().message
        } finally {
            loadingMore = false
        }
    }
}
