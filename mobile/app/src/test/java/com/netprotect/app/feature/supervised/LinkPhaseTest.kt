package com.netprotect.app.feature.supervised

import com.netprotect.app.feature.supervised.link.LinkPhase
import com.netprotect.app.feature.supervised.link.linkPhase
import com.netprotect.app.feature.supervised.link.linkStatusLabel
import org.junit.Assert.assertEquals
import org.junit.Test

/** Sprint 63: the three states of Vincular (lámina 02). */
class LinkPhaseTest {

    @Test
    fun phases() {
        assertEquals(LinkPhase.Waiting, linkPhase(linking = false, error = null))
        assertEquals(LinkPhase.Linking, linkPhase(linking = true, error = null))
        assertEquals(LinkPhase.Failed, linkPhase(linking = false, error = "El código no es válido o ya venció."))
    }

    @Test
    fun aNewAttemptHidesTheOldError() {
        assertEquals(LinkPhase.Linking, linkPhase(linking = true, error = "El código no es válido o ya venció."))
    }

    @Test
    fun statusLabels() {
        assertEquals("Esperando el código", linkStatusLabel(LinkPhase.Waiting))
        assertEquals("Confirmando el código con tu tutor", linkStatusLabel(LinkPhase.Linking))
        assertEquals("El código no se pudo usar", linkStatusLabel(LinkPhase.Failed))
    }
}
