package com.netprotect.app.ui.format

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val SHORT_MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

/** Sprint 45: "24 sep", or "24 sep 2025" when it isn't the same year as [today]. */
fun shortDate(date: LocalDate, today: LocalDate): String {
    val base = "${date.dayOfMonth} ${SHORT_MONTHS[date.monthValue - 1]}"
    return if (date.year == today.year) base else "$base ${date.year}"
}

/** An app's usage as the tutor should read it. The backend's latest usage is the latest *day* that
 * has data, not necessarily today: "1 h 24 min" only when that day is today, otherwise the day is
 * said ("12 min · ayer", "12 min · 24 sep"). Never presents an old figure as today's. */
fun usageLabel(seconds: Int?, usageDate: String?, today: LocalDate): String {
    if (seconds == null || usageDate == null) return "Sin uso registrado"
    val day = runCatching { LocalDate.parse(usageDate) }.getOrNull() ?: return "Sin uso registrado"
    val amount = Durations.format(seconds.toLong())
    return when (day) {
        today -> amount
        today.minusDays(1) -> "$amount · ayer"
        else -> "$amount · ${shortDate(day, today)}"
    }
}

/** "Hoy, 8:12 a. m.", "Ayer, 6:40 p. m." or "24 de septiembre de 2026, 10:15 a. m." for an event
 * time, in the tutor phone's zone. Falls back to the raw text if it can't be parsed. */
fun eventTimeLabel(isoInstant: String, today: LocalDate, zone: ZoneId): String {
    val time = runCatching { ZonedDateTime.ofInstant(java.time.Instant.parse(isoInstant), zone) }.getOrNull()
        ?: return isoInstant
    return "${DayGrouping.label(time.toLocalDate(), today)}, ${Clock.format(time.toLocalTime())}"
}
