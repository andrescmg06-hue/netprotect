package com.netprotect.app.feature.supervised.services

import com.netprotect.app.core.status.ServiceState
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ServicesStatusTextsTest {

    private val now = Instant.parse("2026-10-13T15:00:00Z")

    @Test
    fun everyStateHasItsLabel() {
        assertEquals("Activo", serviceStateLabel(ServiceState.Active))
        assertEquals("Inactivo", serviceStateLabel(ServiceState.Inactive))
        assertEquals("Sin confirmar", serviceStateLabel(ServiceState.Unconfirmed))
    }

    @Test
    fun unconfirmedLocationSaysNoneReceivedYet() {
        assertEquals(
            "No hay una ubicación reciente confirmada. Llega a tu tutor aproximadamente cada 15 minutos.",
            locationDescription(ServiceState.Unconfirmed),
        )
    }

    @Test
    fun otherLocationStatesDescribeTheFunction() {
        val expected = "Comparte la ubicación de este teléfono. Llega a tu tutor aproximadamente cada 15 minutos."
        assertEquals(expected, locationDescription(ServiceState.Active))
        assertEquals(expected, locationDescription(ServiceState.Inactive))
    }

    @Test
    fun locationIsNeverCalledRealTime() {
        ServiceState.entries.forEach { state ->
            assertFalse(locationDescription(state).contains("tiempo real"))
        }
    }

    @Test
    fun lastReportLabel() {
        assertEquals("Último reporte: hace 1 min", lastReportLabel(now.minusSeconds(60), now, ZoneOffset.UTC))
        assertEquals("Último reporte: hace un momento", lastReportLabel(now.minusSeconds(10), now, ZoneOffset.UTC))
        assertEquals("Último reporte: todavía no", lastReportLabel(null, now, ZoneOffset.UTC))
    }
}
