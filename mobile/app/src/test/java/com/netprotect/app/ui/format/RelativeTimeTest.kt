package com.netprotect.app.ui.format

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val zone = ZoneId.of("America/Bogota")

    private fun instantOf(date: LocalDate, time: LocalTime): Instant =
        LocalDateTime.of(date, time).atZone(zone).toInstant()

    // "Ahora" fijo al final del día, para que "hace 23 h" caiga el mismo día calendario.
    private val now = instantOf(LocalDate.of(2026, 9, 26), LocalTime.of(23, 0))

    @Test
    fun underAMinuteIsAMoment() {
        assertEquals("hace un momento", RelativeTime.format(now.minusSeconds(30), now, zone))
    }

    @Test
    fun aFutureInstantIsAMoment() {
        assertEquals("hace un momento", RelativeTime.format(now.plusSeconds(120), now, zone))
    }

    @Test
    fun minutes() {
        assertEquals("hace 12 min", RelativeTime.format(now.minusSeconds(12 * 60), now, zone))
        assertEquals("hace 59 min", RelativeTime.format(now.minusSeconds(59 * 60), now, zone))
    }

    @Test
    fun oneHour() {
        assertEquals("hace 1 h", RelativeTime.format(now.minusSeconds(60 * 60), now, zone))
    }

    @Test
    fun twentyThreeHoursSameDay() {
        assertEquals("hace 23 h", RelativeTime.format(now.minusSeconds(23 * 3600), now, zone))
    }

    @Test
    fun yesterdayLateEveningIsAyer() {
        val then = instantOf(LocalDate.of(2026, 9, 25), LocalTime.of(23, 0))
        val seen = instantOf(LocalDate.of(2026, 9, 26), LocalTime.of(9, 0))
        assertEquals("ayer", RelativeTime.format(then, seen, zone))
    }

    @Test
    fun crossesMidnightUsesZoneNotUtc() {
        // 1 h de diferencia, pero en Bogota (UTC−5) el día calendario cambió: el instante en UTC ya
        // es 04:30Z del 26, de modo que con UTC saldría "hace 1 h" y con la zona correcta "ayer".
        val then = instantOf(LocalDate.of(2026, 9, 25), LocalTime.of(23, 30))
        val seen = instantOf(LocalDate.of(2026, 9, 26), LocalTime.of(0, 30))
        assertEquals("ayer", RelativeTime.format(then, seen, zone))
    }

    @Test
    fun twoDaysAgoIsAFullDate() {
        val then = instantOf(LocalDate.of(2026, 9, 24), LocalTime.of(23, 0))
        assertEquals("24 de septiembre de 2026", RelativeTime.format(then, now, zone))
    }
}
