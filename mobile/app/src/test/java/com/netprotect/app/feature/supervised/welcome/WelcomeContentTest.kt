package com.netprotect.app.feature.supervised.welcome

import com.netprotect.app.feature.supervised.privacy.PRIVACY_NO_RECORDING
import com.netprotect.app.feature.supervised.privacy.privacySections
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sprint 67: the welcome after a new link (láminas 13.1 and 13.2). */
class WelcomeContentTest {

    @Test
    fun nextGoesToStepTwoAndThenFinishes() {
        assertEquals(WelcomeStep.TutorCanSee, welcomeNext(WelcomeStep.Linked))
        assertNull(welcomeNext(WelcomeStep.TutorCanSee))
    }

    @Test
    fun backReturnsToStepOneAndNeverLeavesIt() {
        assertEquals(WelcomeStep.Linked, welcomeBack(WelcomeStep.TutorCanSee))
        assertEquals(WelcomeStep.Linked, welcomeBack(WelcomeStep.Linked))
    }

    @Test
    fun stepLabelCountsTwoSteps() {
        assertEquals("Paso 1 de 2", welcomeStepLabel(WelcomeStep.Linked))
        assertEquals("Paso 2 de 2", welcomeStepLabel(WelcomeStep.TutorCanSee))
    }

    @Test
    fun shownOnlyRightAfterLinkingAndOncePerDevice() {
        assertTrue(shouldShowWelcome(justLinked = true, seenForDeviceId = null, deviceId = "d1"))
        // Already finished for this link: not again.
        assertFalse(shouldShowWelcome(justLinked = true, seenForDeviceId = "d1", deviceId = "d1"))
        // Seen for an older link: a different device id shows it again.
        assertTrue(shouldShowWelcome(justLinked = true, seenForDeviceId = "d0", deviceId = "d1"))
        // Phones already linked when the app updates (no pairing now) never see it.
        assertFalse(shouldShowWelcome(justLinked = false, seenForDeviceId = null, deviceId = "d1"))
    }

    @Test
    fun finishLabelPromisesPermissionsOnlyWhenOneIsPending() {
        assertEquals("Entendido, configurar permisos", welcomeFinishLabel(1))
        assertEquals("Entendido, configurar permisos", welcomeFinishLabel(4))
        assertEquals("Entendido", welcomeFinishLabel(0))
    }

    @Test
    fun stepTwoSaysExactlyWhatPrivacidadSays() {
        val privacyItems = privacySections.flatMap { it.items }
        assertEquals(privacyItems, welcomeTutorSees + welcomeYouDecide)
        assertEquals(privacySections.first().items, welcomeTutorSees)
        assertEquals(listOf("Solo con tu permiso"), welcomeYouDecide.map { it.title })
        assertTrue(welcomeTexts().contains(PRIVACY_NO_RECORDING))
    }

    @Test
    fun noFalseControlClaimNoRetentionNoDesignNote() {
        val banned = listOf("ni controla", "controla", "retención", "conserva", "días", "meses", "esté abierta", "pendiente de validar")
        welcomeTexts().forEach { text ->
            banned.forEach { word -> assertFalse("«$word» in «$text»", text.contains(word, ignoreCase = true)) }
        }
    }

    @Test
    fun neverRealTimeExceptToDenyIt() {
        welcomeTexts().filter { it.contains("tiempo real") }.forEach { assertTrue(it, it.contains("no en tiempo real")) }
    }
}
