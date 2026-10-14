package com.netprotect.app.feature.supervised.link

/** Sprint 63: the three states of lámina 02. Sending wins over an old error: a new attempt is running. */
enum class LinkPhase { Waiting, Linking, Failed }

fun linkPhase(linking: Boolean, error: String?): LinkPhase = when {
    linking -> LinkPhase.Linking
    error != null -> LinkPhase.Failed
    else -> LinkPhase.Waiting
}

/** The status line at the foot of the screen, by phase (lámina 02). */
fun linkStatusLabel(phase: LinkPhase): String = when (phase) {
    LinkPhase.Waiting -> "Esperando el código"
    LinkPhase.Linking -> "Confirmando el código con tu tutor"
    LinkPhase.Failed -> "El código no se pudo usar"
}
