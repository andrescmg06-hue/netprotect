package com.netprotect.app.ui.format

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private fun fullDate(date: LocalDate): String =
    "${date.dayOfMonth} de ${MONTHS[date.monthValue - 1]} de ${date.year}"

/** Fechas relativas ("hace un momento", "hace 12 min", "ayer", "26 de septiembre de 2026").
 * El tiempo entra por parámetro (nunca `Instant.now()` dentro). */
object RelativeTime {
    fun format(instant: Instant, now: Instant, zone: ZoneId): String {
        val diffSeconds = Duration.between(instant, now).seconds
        // < 60 s o futura (desfase de reloj)
        if (diffSeconds < 60) return "hace un momento"
        // 1–59 min
        if (diffSeconds < 3600) return "hace ${diffSeconds / 60} min"
        val instantDate = instant.atZone(zone).toLocalDate()
        val nowDate = now.atZone(zone).toLocalDate()
        // 1–23 h dentro del mismo día calendario
        if (diffSeconds < 86400 && instantDate == nowDate) return "hace ${diffSeconds / 3600} h"
        // día calendario de ayer (en la zona dada), aunque sea menos de 24 h (cruza medianoche)
        if (instantDate == nowDate.minusDays(1)) return "ayer"
        return fullDate(instantDate)
    }
}
