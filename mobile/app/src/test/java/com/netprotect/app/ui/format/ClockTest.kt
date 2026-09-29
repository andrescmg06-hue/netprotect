package com.netprotect.app.ui.format

import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ClockTest {

    @Test
    fun formatsTwelveHourClock() {
        assertEquals("12:05 a. m.", Clock.format(LocalTime.of(0, 5)))
        assertEquals("12:00 p. m.", Clock.format(LocalTime.of(12, 0)))
        assertEquals("12:30 p. m.", Clock.format(LocalTime.of(12, 30)))
        assertEquals("3:42 p. m.", Clock.format(LocalTime.of(15, 42)))
        assertEquals("11:59 p. m.", Clock.format(LocalTime.of(23, 59)))
        assertEquals("9:07 a. m.", Clock.format(LocalTime.of(9, 7)))
    }
}
