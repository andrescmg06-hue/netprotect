package com.netprotect.app.feature.supervised.battery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 67: the battery notice (lámina 13.3) says only what the check and the Settings screen back up. */
class BatteryContentTest {

    @Test
    fun threeStepsLikeTheSlide() {
        val steps = batterySteps()
        assertEquals(3, steps.size)
        assertTrue(steps[0].contains("«Abrir ajustes de batería»"))
        assertEquals("Elige «Sin restricciones» o «No optimizar».", steps[1])
        assertTrue(steps[2].contains("Lo comprobamos automáticamente al volver"))
    }

    @Test
    fun firstStepDoesNotPromiseNetProtectsOwnPage() {
        // ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS opens a list of apps, not NetProtect's page.
        assertTrue(batterySteps()[0].contains("lista de apps"))
        batteryTexts().forEach { assertFalse(it, it.contains("ajustes de batería de NetProtect")) }
    }

    @Test
    fun saysNamesChangeAndStepsAreStillBeingChecked() {
        assertTrue(BATTERY_NOTE.contains("cambian según el teléfono"))
        assertTrue(BATTERY_NOTE.contains("todavía se están comprobando"))
    }

    @Test
    fun onlyClaimsThatItCanStopNotThatItDid() {
        assertTrue(BATTERY_BODY.contains("puede detenerlo"))
        assertTrue(BATTERY_TITLE.contains("puede detenerse"))
        assertTrue(BATTERY_HOME_TITLE.contains("puede detener"))
    }

    @Test
    fun noPromiseNoControlClaimNoManufacturer() {
        val banned = listOf(
            "nunca", "siempre funcionará", "garantiza", "controla", "retención", "Play",
            "Samsung", "Xiaomi", "Huawei", "Motorola", "Oppo", "OnePlus", "Pixel", "One UI", "SM-",
        )
        batteryTexts().forEach { text ->
            banned.forEach { word -> assertFalse("«$word» in «$text»", text.contains(word, ignoreCase = true)) }
        }
    }

    @Test
    fun theTutorIsNeverSaidToSeeTheBattery() {
        // Nothing about the battery is sent anywhere: no text may say the tutor sees or is told about it.
        batteryTexts().forEach { text ->
            assertFalse(text, text.contains("tutor puede ver", ignoreCase = true))
            assertFalse(text, text.contains("avisa a tu tutor", ignoreCase = true))
        }
    }
}
