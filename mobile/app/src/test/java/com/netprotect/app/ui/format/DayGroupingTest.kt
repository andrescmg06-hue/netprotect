package com.netprotect.app.ui.format

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayGroupingTest {

    private val today = LocalDate.of(2026, 9, 26)

    @Test
    fun labels() {
        assertEquals("Hoy", DayGrouping.label(LocalDate.of(2026, 9, 26), today))
        assertEquals("Ayer", DayGrouping.label(LocalDate.of(2026, 9, 25), today))
        assertEquals(
            "24 de septiembre de 2026",
            DayGrouping.label(LocalDate.of(2026, 9, 24), today),
        )
    }
}
