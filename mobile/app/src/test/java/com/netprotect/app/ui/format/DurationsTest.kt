package com.netprotect.app.ui.format

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationsTest {

    @Test
    fun underAMinute() {
        assertEquals("menos de 1 min", Durations.format(0))
        assertEquals("menos de 1 min", Durations.format(59))
    }

    @Test
    fun wholeMinutes() {
        assertEquals("1 min", Durations.format(60))
        assertEquals("48 min", Durations.format(2880))
    }

    @Test
    fun hours() {
        assertEquals("1 h", Durations.format(3600))
        assertEquals("2 h", Durations.format(7200))
    }

    @Test
    fun hoursAndMinutes() {
        assertEquals("1 h 24 min", Durations.format(5040))
    }

    @Test
    fun negativesAreZero() {
        assertEquals("menos de 1 min", Durations.format(-5))
    }
}
