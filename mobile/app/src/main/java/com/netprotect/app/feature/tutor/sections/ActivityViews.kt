package com.netprotect.app.feature.tutor.sections

import com.netprotect.app.core.network.AlertSilence
import com.netprotect.app.core.network.DeviceAlert
import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.DeviceStatistics
import com.netprotect.app.core.network.HistoryEvent
import com.netprotect.app.ui.format.AlertLabels
import com.netprotect.app.ui.format.CategoryLabels
import com.netprotect.app.ui.format.Clock
import com.netprotect.app.ui.format.DayGrouping
import com.netprotect.app.ui.format.Durations
import com.netprotect.app.ui.format.RuleLabels
import com.netprotect.app.ui.format.eventTimeLabel
import com.netprotect.app.ui.theme.NpTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Sprint 46: what the Historial, Estadísticas and Alertas screens receive, already computed.
 * Pure and tested on the JVM; the screens only draw. */

// ---- App names ----------------------------------------------------------------------------

/** History, alerts and compliance only carry the package; the visible name comes from the
 * device's own app list. Missing there (never synced, or the list failed to load) → the package. */
fun appLabels(apps: List<DeviceApplicationSummary>): Map<String, String> =
    apps.filter { it.appLabel.isNotBlank() }.associate { it.packageName to it.appLabel }

private fun appName(packageName: String, labels: Map<String, String>): String = labels[packageName] ?: packageName

// ---- Historial ----------------------------------------------------------------------------

enum class HistoryKind { Block, Enter, Exit }

data class HistoryRow(
    val id: String,
    val kind: HistoryKind,
    val title: String,
    /** "Motivo: Límite diario" for a block (null if the backend sent no reason); null for zones. */
    val subtitle: String?,
    /** Rule applied, to pick the icon/tone of a block ([RuleLabels.ruleTypeIcon]); null for zones. */
    val ruleType: String?,
    /** "3:42 p. m." — the day is in the group's header. */
    val time: String,
)

/** A day of the timeline: "Hoy" + "27 de septiembre de 2026", "Ayer" + date, or only the date. */
data class HistoryDay(val date: LocalDate, val title: String, val subtitle: String?, val rows: List<HistoryRow>)

/** Groups by the day *in the tutor's zone* (an event at 03:00 UTC is still yesterday evening in
 * Bogotá), newest day and newest event first. Unparseable times are dropped rather than guessed. */
fun historyDays(
    events: List<HistoryEvent>,
    labels: Map<String, String>,
    today: LocalDate,
    zone: ZoneId,
): List<HistoryDay> = events
    .mapNotNull { event ->
        val instant = runCatching { Instant.parse(event.occurredAt) }.getOrNull() ?: return@mapNotNull null
        instant to event
    }
    .sortedByDescending { it.first }
    .groupBy { it.first.atZone(zone).toLocalDate() }
    .map { (date, entries) ->
        val title = DayGrouping.label(date, today)
        HistoryDay(
            date = date,
            title = title,
            subtitle = if (date == today || date == today.minusDays(1)) DayGrouping.fullDate(date) else null,
            rows = entries.map { (instant, event) -> historyRow(event, Clock.format(instant.atZone(zone).toLocalTime()), labels) },
        )
    }

private fun historyRow(event: HistoryEvent, time: String, labels: Map<String, String>): HistoryRow =
    if (event.eventType == "GEOFENCE") {
        val enter = event.geofenceEventType == "ENTER"
        val zoneName = event.geofenceName ?: "una zona"
        HistoryRow(
            id = event.id,
            kind = if (enter) HistoryKind.Enter else HistoryKind.Exit,
            title = if (enter) "Entró · $zoneName" else "Salió · $zoneName",
            subtitle = null,
            ruleType = null,
            time = time,
        )
    } else {
        HistoryRow(
            id = event.id,
            kind = HistoryKind.Block,
            title = "Bloqueo de app · ${event.packageName?.let { appName(it, labels) } ?: "una app"}",
            subtitle = event.ruleTypeApplied?.let { "Motivo: ${RuleLabels.ruleTypeLabel(it)}" },
            ruleType = event.ruleTypeApplied,
            time = time,
        )
    }

// ---- Estadísticas -------------------------------------------------------------------------

enum class StatsPeriod(val apiValue: String, val label: String) {
    Today("today", "Hoy"),
    Week("7d", "7 días"),
    Month("30d", "30 días"),
}

/** [fraction] is relative to the most used app of the period (the first bar is full). */
data class TopAppRow(val packageName: String, val name: String, val duration: String, val fraction: Float)

data class BlockReasonItem(val ruleType: String, val label: String, val icon: Int, val tone: NpTone, val count: Int)

/**
 * One daily-limit rule. The backend's `compliance_rate` is *days within the limit / days with usage*
 * — not "percentage of the allowed time used", which is what the mockup claimed.
 */
data class ComplianceRow(
    val name: String,
    /** "Límite: 60 min/día". */
    val limit: String,
    /** Bar fill = compliance rate; null when there was no usage in the period (nothing evaluated). */
    val fraction: Float?,
    /** "5 de 7 días", "1 de 1 día" or "Sin uso registrado en este periodo." */
    val days: String,
)

data class StatisticsView(
    val topApps: List<TopAppRow>,
    val blocks: List<BlockReasonItem>,
    val compliance: List<ComplianceRow>,
)

/** Literal texts the Estadísticas screen uses; kept here so the tests pin them. */
const val COMPLIANCE_SUBTITLE = "Días dentro del límite en este periodo."
const val NO_USAGE_IN_PERIOD = "Sin uso registrado en este periodo."

fun statisticsView(stats: DeviceStatistics, labels: Map<String, String>): StatisticsView {
    val maxSeconds = stats.topApps.maxOfOrNull { it.totalSeconds }?.takeIf { it > 0 }
    return StatisticsView(
        topApps = stats.topApps.map {
            TopAppRow(
                packageName = it.packageName,
                name = it.appLabel?.takeIf { label -> label.isNotBlank() } ?: appName(it.packageName, labels),
                duration = shortDuration(it.totalSeconds),
                fraction = if (maxSeconds == null) 0f else it.totalSeconds.toFloat() / maxSeconds,
            )
        },
        blocks = stats.blocksByReason
            .filter { it.count > 0 }
            .sortedByDescending { it.count }
            .take(7)
            .map {
                BlockReasonItem(
                    ruleType = it.ruleTypeApplied,
                    label = RuleLabels.ruleTypeLabel(it.ruleTypeApplied),
                    icon = RuleLabels.ruleTypeIcon(it.ruleTypeApplied),
                    tone = RuleLabels.ruleTypeTone(it.ruleTypeApplied),
                    count = it.count,
                )
            },
        compliance = stats.compliance.map {
            val name = when {
                it.packageName != null -> appName(it.packageName, labels)
                it.category != null -> CategoryLabels.categoryLabel(it.category)
                else -> "Regla sin nombre"
            }
            ComplianceRow(
                name = name,
                limit = "Límite: ${it.dailyLimitMinutes} min/día",
                fraction = if (it.daysEvaluated == 0) null else it.complianceRate?.toFloat()?.coerceIn(0f, 1f),
                days = complianceDays(it.daysCompliant, it.daysEvaluated),
            )
        },
    )
}

/** The bar's label has a fixed width; "menos de 1 min" doesn't fit there, "< 1 min" does. */
fun shortDuration(totalSeconds: Int): String =
    if (totalSeconds < 60) "< 1 min" else Durations.format(totalSeconds.toLong())

fun complianceDays(compliant: Int, evaluated: Int): String = when (evaluated) {
    0 -> NO_USAGE_IN_PERIOD
    1 -> "$compliant de 1 día"
    else -> "$compliant de $evaluated días"
}

// ---- Alertas ------------------------------------------------------------------------------

enum class AlertFilter(val label: String) {
    All("Todas"),
    Unread("No leídas"),
    /** CRITICAL and HIGH together: what needs the tutor's attention (decided by the owner, S46). */
    Critical("Críticas"),
    Warnings("Advertencias"),
}

data class AlertItem(
    val id: String,
    val level: String,
    /** Web label of the type, with the app's visible name ("Se bloqueó YouTube"). */
    val title: String,
    /** "Hoy, 3:42 p. m." */
    val time: String,
    /** "Repetido 3 veces", or null for a single occurrence. */
    val repeated: String?,
    val unread: Boolean,
    /** An active silence covers this alert's signal: the "Silenciar" action is replaced by a mark. */
    val silenced: Boolean,
)

fun alertItems(
    alerts: List<DeviceAlert>,
    silences: List<AlertSilence>,
    labels: Map<String, String>,
    now: Instant,
    zone: ZoneId,
): List<AlertItem> {
    val today = now.atZone(zone).toLocalDate()
    val silencedKeys = silences.filter { isSilenceActive(it, now) }.map { it.dedupKey }.toSet()
    return alerts.map { alert ->
        AlertItem(
            id = alert.id,
            level = alert.level,
            title = AlertLabels.alertMessage(
                alert.alertType,
                alert.packageName?.let { appName(it, labels) },
                alert.geofenceName,
            ),
            time = eventTimeLabel(alert.lastOccurredAt, today, zone),
            repeated = if (alert.occurrenceCount > 1) "Repetido ${alert.occurrenceCount} veces" else null,
            unread = alert.readAt == null,
            silenced = alert.dedupKey.isNotEmpty() && alert.dedupKey in silencedKeys,
        )
    }
}

/** Indefinite (`silencedUntil == null`) or not yet expired. An unreadable date counts as active:
 * the backend is the one that stops the alerts, the app only avoids offering a pointless button. */
fun isSilenceActive(silence: AlertSilence, now: Instant): Boolean {
    val until = silence.silencedUntil ?: return true
    val instant = runCatching { Instant.parse(until) }.getOrNull() ?: return true
    return instant.isAfter(now)
}

fun filterAlerts(items: List<AlertItem>, filter: AlertFilter): List<AlertItem> = when (filter) {
    AlertFilter.All -> items
    AlertFilter.Unread -> items.filter { it.unread }
    AlertFilter.Critical -> items.filter { it.level == "CRITICAL" || it.level == "HIGH" }
    AlertFilter.Warnings -> items.filter { it.level == "WARNING" }
}
