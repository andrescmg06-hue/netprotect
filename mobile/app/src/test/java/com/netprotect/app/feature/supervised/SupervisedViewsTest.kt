package com.netprotect.app.feature.supervised

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupervisedViewsTest {

    @Test
    fun pendingCountsOnlyTheFourAskedPermissions() {
        assertEquals(0, PermissionsUi(usageAccess = true, location = true, overlay = true, deviceAdmin = true).pending)
        assertEquals(2, PermissionsUi(usageAccess = false, location = true, overlay = false, deviceAdmin = true).pending)
        assertEquals(4, PermissionsUi(usageAccess = false, location = false, overlay = false, deviceAdmin = false).pending)
    }

    @Test
    fun pendingLabelIsSingularPluralOrDone() {
        assertEquals("Todos los permisos están configurados", pendingPermissionsLabel(0))
        assertEquals("1 permiso pendiente", pendingPermissionsLabel(1))
        assertEquals("3 permisos pendientes", pendingPermissionsLabel(3))
    }

    @Test
    fun pastedCodesKeepOnlySixDigits() {
        assertEquals("123456", sanitizePairingCode("123 456"))
        assertEquals("123456", sanitizePairingCode("12-34-56"))
        assertEquals("123456", sanitizePairingCode("1234567890"))
        assertEquals("", sanitizePairingCode("abc"))
    }

    @Test
    fun onlySixDigitsAreACompleteCode() {
        assertTrue(isCompletePairingCode("012345"))
        assertFalse(isCompletePairingCode("12345"))
        assertFalse(isCompletePairingCode("12345a"))
        assertFalse(isCompletePairingCode("1234567"))
    }

    @Test
    fun lastContactIsTheNewerOfLocalHeartbeatAndServer() {
        val local = Instant.parse("2026-09-30T15:00:00Z")
        assertEquals(local, lastContact(local, "2026-09-30T14:58:00Z"))
        assertEquals(Instant.parse("2026-09-30T15:01:00Z"), lastContact(local, "2026-09-30T15:01:00Z"))
        assertEquals(local, lastContact(local, "not a date"))
        assertEquals(Instant.parse("2026-09-30T14:00:00Z"), lastContact(null, "2026-09-30T14:00:00Z"))
        assertNull(lastContact(null, null))
    }
}
