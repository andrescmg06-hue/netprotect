package com.netprotect.app.feature.supervised.privacy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 66: Privacidad publishes only what the code backs, including what the slide left out. */
class PrivacyContentTest {

    private val texts = privacyTexts()
    private val joined = texts.joinToString("\n")

    @Test
    fun noRetentionNoFalseControlClaimNoDesignNote() {
        val banned = listOf("impide", "ni controla", "controla", "retención", "conserva", "días", "meses", "pendiente de validar")
        texts.forEach { text ->
            banned.forEach { word -> assertFalse("«$word» in «$text»", text.contains(word, ignoreCase = true)) }
        }
    }

    @Test
    fun neverRealTimeExceptToDenyIt() {
        texts.filter { it.contains("tiempo real") }.forEach { assertTrue(it, it.contains("no en tiempo real")) }
    }

    @Test
    fun saysTheRealCadences() {
        // HEARTBEAT_INTERVAL_MS, APP_SYNC_INTERVAL_MS (SupervisedShell) and REPORT_INTERVAL_MS (LocationReportingService).
        assertTrue(joined.contains("Cada minuto mientras la app esté abierta."))
        assertTrue(joined.contains("cada 5 minutos mientras la app esté abierta"))
        assertTrue(joined.contains("aproximadamente cada 15 minutos, no en tiempo real"))
    }

    @Test
    fun includesWhatTheSlideLeftOut() {
        assertTrue(joined.contains("cuánto tiempo usaste cada una hoy"))
        assertTrue(joined.contains("la versión de Android y de NetProtect, la zona horaria"))
        assertTrue(joined.contains("si el acceso al uso está concedido"))
    }

    @Test
    fun screenViewIsConsentedStoppableAndNotRecorded() {
        assertTrue(joined.contains("Tú decides si aceptas y puedes detenerla en cualquier momento."))
        assertEquals("NetProtect no graba ni guarda lo que se ve en tu pantalla.", texts.last())
    }
}
