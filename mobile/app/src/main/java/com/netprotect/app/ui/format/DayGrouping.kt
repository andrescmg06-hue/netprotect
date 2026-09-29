package com.netprotect.app.ui.format

import java.time.LocalDate

private val MONTHS = listOf(
    "enero", "febrero", "marzo", "abril", "mayo", "junio",
    "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
)

private fun fullDate(date: LocalDate): String =
    "${date.dayOfMonth} de ${MONTHS[date.monthValue - 1]} de ${date.year}"

/** Agrupación por día ("Hoy", "Ayer", o la fecha completa). */
object DayGrouping {
    fun label(date: LocalDate, today: LocalDate): String = when {
        date == today -> "Hoy"
        date == today.minusDays(1) -> "Ayer"
        else -> fullDate(date)
    }
}
