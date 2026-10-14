package com.netprotect.app.feature.supervised.linked

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLabelsTest {

    @Test
    fun permissionsTileLabelUsesTheSingularAndAllConfigured() {
        assertEquals("Todos configurados", permissionsTileLabel(0))
        assertEquals("1 pendiente", permissionsTileLabel(1))
        assertEquals("2 pendientes", permissionsTileLabel(2))
        assertEquals("4 pendientes", permissionsTileLabel(4))
    }

    @Test
    fun contactStatusOnlySaysConnectedWithARecentReport() {
        assertEquals("Conectado", contactStatusLabel(reportRecent = true))
        assertEquals("Sin actualización reciente", contactStatusLabel(reportRecent = false))
    }
}
