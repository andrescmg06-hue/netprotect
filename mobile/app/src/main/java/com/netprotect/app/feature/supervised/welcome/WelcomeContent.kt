package com.netprotect.app.feature.supervised.welcome

import com.netprotect.app.feature.supervised.privacy.PRIVACY_NO_RECORDING
import com.netprotect.app.feature.supervised.privacy.PrivacyItem
import com.netprotect.app.feature.supervised.privacy.privacySections

/** The two steps of the welcome shown once after a new link (láminas 13.1 and 13.2). */
enum class WelcomeStep { Linked, TutorCanSee }

const val WELCOME_STEP_COUNT = 2

/** The step «Ver qué hace NetProtect» leads to; null after the last one (the welcome is finished). */
fun welcomeNext(step: WelcomeStep): WelcomeStep? = when (step) {
    WelcomeStep.Linked -> WelcomeStep.TutorCanSee
    WelcomeStep.TutorCanSee -> null
}

/** Back (arrow or system) on the second step returns to the first; on the first it stays there, so the
 * welcome is never skipped by accident. */
fun welcomeBack(step: WelcomeStep): WelcomeStep = when (step) {
    WelcomeStep.Linked -> WelcomeStep.Linked
    WelcomeStep.TutorCanSee -> WelcomeStep.Linked
}

/** "Paso 2 de 2". */
fun welcomeStepLabel(step: WelcomeStep): String = "Paso ${step.ordinal + 1} de $WELCOME_STEP_COUNT"

/**
 * Shown only right after a pairing that just succeeded ([justLinked]) and only if it was not already
 * finished for this very [deviceId]. Phones that were linked before this version never see it: their
 * start goes through the link check, not a pairing.
 */
fun shouldShowWelcome(justLinked: Boolean, seenForDeviceId: String?, deviceId: String): Boolean =
    justLinked && seenForDeviceId != deviceId

const val WELCOME_EYEBROW = "Vinculación completada"
const val WELCOME_TITLE = "Este teléfono ya está vinculado"
const val WELCOME_BODY = "Antes de seguir, te explicamos en dos pasos qué hace NetProtect aquí."
const val WELCOME_NEXT = "Ver qué hace NetProtect"

const val WELCOME_SEE_TITLE = "Esto es lo que tu tutor puede ver"
const val WELCOME_DECIDE_HEADING = "Y tú decides"
const val WELCOME_FINISH = "Entendido"
const val WELCOME_FINISH_TO_PERMISSIONS = "Entendido, configurar permisos"

/** The last button only promises the permissions when there is one left to configure (it then opens Permisos). */
fun welcomeFinishLabel(pendingPermissions: Int): String =
    if (pendingPermissions > 0) WELCOME_FINISH_TO_PERMISSIONS else WELCOME_FINISH

private const val SCREEN_VIEW_HEADING = "Vista de pantalla"

// Step 2 says exactly what Privacidad says, taken from the same list, so the two can never drift apart:
// the slide's shorter lines (e.g. «Si el dispositivo está activo») are not all backed by the code.
/** What NetProtect sends or does that reaches the tutor (Privacidad's «Qué hace»). */
val welcomeTutorSees: List<PrivacyItem> =
    privacySections.filter { it.heading != SCREEN_VIEW_HEADING }.flatMap { it.items }

/** The screen view, which only happens if the person accepts (Privacidad's «Vista de pantalla»). */
val welcomeYouDecide: List<PrivacyItem> =
    privacySections.filter { it.heading == SCREEN_VIEW_HEADING }.flatMap { it.items }

/** Every text the welcome can show, both steps and both finish labels; for the wording tests. */
fun welcomeTexts(): List<String> =
    listOf(WELCOME_EYEBROW, WELCOME_TITLE, WELCOME_BODY, WELCOME_NEXT) +
        listOf(welcomeStepLabel(WelcomeStep.TutorCanSee), WELCOME_SEE_TITLE) +
        welcomeTutorSees.flatMap { listOf(it.title, it.body) } +
        WELCOME_DECIDE_HEADING +
        welcomeYouDecide.flatMap { listOf(it.title, it.body) } +
        PRIVACY_NO_RECORDING +
        listOf(WELCOME_FINISH, WELCOME_FINISH_TO_PERMISSIONS)
