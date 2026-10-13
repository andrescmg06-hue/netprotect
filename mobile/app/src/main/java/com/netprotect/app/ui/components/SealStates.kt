package com.netprotect.app.ui.components

/** State of one arc of [NpSeal]. Colour is never the only carrier: the caller always pairs the seal with text. */
enum class SealArcState { Ready, Pending, Inactive }

/** Number of arcs the seal draws: one per required permission. */
const val SEAL_ARC_COUNT = 4

/**
 * Coerces [states] to exactly [SEAL_ARC_COUNT] entries: extra entries are dropped and missing ones are
 * drawn as [SealArcState.Inactive], so a wrong-sized list never crashes or silently draws fewer arcs.
 */
fun normalizeSealStates(states: List<SealArcState>): List<SealArcState> =
    List(SEAL_ARC_COUNT) { index -> states.getOrNull(index) ?: SealArcState.Inactive }

/**
 * Maps the permission count to arc states. Arcs are ordered clockwise from the top-right one, so the
 * ready arcs fill the right half first (as in lámina 03, "Falta un paso"). Without a recent report every
 * arc is [SealArcState.Inactive], whatever the permissions say.
 */
fun sealStatesFor(readyCount: Int, reporting: Boolean = true): List<SealArcState> =
    List(SEAL_ARC_COUNT) { index ->
        when {
            !reporting -> SealArcState.Inactive
            index < readyCount -> SealArcState.Ready
            else -> SealArcState.Pending
        }
    }

/** Caption under the seal, e.g. "4 de 4 permisos" (the hero panel renders it uppercase). */
fun permissionsCaption(readyCount: Int, total: Int = SEAL_ARC_COUNT): String =
    "${readyCount.coerceIn(0, total)} de $total permisos"

/** Starting point of the one-shot draw animation: complete when the user removed animations. */
fun sealInitialProgress(reducedMotion: Boolean): Float = if (reducedMotion) 1f else 0f
