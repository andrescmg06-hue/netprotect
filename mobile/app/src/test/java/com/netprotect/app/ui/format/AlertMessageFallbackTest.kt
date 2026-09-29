package com.netprotect.app.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** The API can omit the app or zone name; the tutor must never read the word "null". */
class AlertMessageFallbackTest {

    @Test
    fun aMissingPackageNameReadsAsAnApp() {
        assertEquals("Se bloqueó una app", AlertLabels.alertMessage("APP_BLOCKED", null, null))
        assertEquals(
            "Se alcanzó el límite de tiempo de una app",
            AlertLabels.alertMessage("APP_LIMIT_REACHED", null, null),
        )
    }

    @Test
    fun aMissingZoneNameReadsAsAZone() {
        assertEquals("Salió de una zona", AlertLabels.alertMessage("GEOFENCE_EXIT", null, null))
        assertEquals("Entró a una zona", AlertLabels.alertMessage("GEOFENCE_ENTER", null, null))
    }

    @Test
    fun realNamesAreStillUsed() {
        assertEquals("Se bloqueó com.instagram.android", AlertLabels.alertMessage("APP_BLOCKED", "com.instagram.android", null))
        assertEquals("Entró a Casa", AlertLabels.alertMessage("GEOFENCE_ENTER", null, "Casa"))
    }

    @Test
    fun noTypeEverProducesTheWordNull() {
        val types = listOf(
            "APP_BLOCKED", "APP_LIMIT_REACHED", "GEOFENCE_EXIT", "GEOFENCE_ENTER", "PERMISSION_REVOKED",
            "SERVICE_INACTIVE", "HEARTBEAT_SILENCE", "CLOCK_TAMPERING", "UNINSTALL_ATTEMPT",
        )
        types.forEach { assertFalse(it, AlertLabels.alertMessage(it, null, null).contains("null")) }
    }
}
