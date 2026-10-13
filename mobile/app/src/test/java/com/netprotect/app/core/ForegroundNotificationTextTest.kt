package com.netprotect.app.core

import com.netprotect.app.core.location.LocationReportingService
import com.netprotect.app.core.rules.RuleEnforcementService
import com.netprotect.app.core.screenshare.ScreenShareService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** Sprint 65 (T5b): the three ongoing notifications only say what the services really do. */
class ForegroundNotificationTextTest {

    private val texts = listOf(
        RuleEnforcementService.NOTIFICATION_TEXT,
        LocationReportingService.NOTIFICATION_TEXT,
        ScreenShareService.NOTIFICATION_TEXT,
    )

    @Test
    fun `the three texts are pinned`() {
        assertEquals("Control de apps funcionando en este dispositivo.", RuleEnforcementService.NOTIFICATION_TEXT)
        assertEquals("Se envía una ubicación aproximada cada 15 minutos.", LocationReportingService.NOTIFICATION_TEXT)
        assertEquals("Toca para abrir NetProtect y detener la vista.", ScreenShareService.NOTIFICATION_TEXT)
    }

    @Test
    fun `no text makes a false claim`() {
        val forbidden = listOf("controla", "graba", "no permite quitar", "no se puede quitar")
        for (text in texts) {
            for (word in forbidden) {
                assertFalse("\"$text\" contains \"$word\"", text.lowercase().contains(word))
            }
        }
    }

    @Test
    fun `the only number is the real location interval`() {
        for (text in texts) {
            val rest = text.replace("15 minutos", "")
            assertFalse("\"$text\" has an unexpected number", rest.any { it.isDigit() })
        }
        assertEquals(15 * 60_000L, LocationReportingService.REPORT_INTERVAL_MS)
    }
}
