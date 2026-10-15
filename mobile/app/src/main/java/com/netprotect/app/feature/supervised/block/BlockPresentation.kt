package com.netprotect.app.feature.supervised.block

import com.netprotect.app.core.rules.BlockReason
import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpTone

/**
 * Sprint 49: what the "App bloqueada" screen says and shows for each of the 7 real reasons, as a
 * pure function so every case is tested without a device.
 *
 * Every text here is true of the real engine (RuleEvaluator, D-11 a):
 * - the daily counter resets at local midnight and the weekly one on Monday (AppInventoryCollector),
 *   so "se reinicia mañana / el lunes" is said only for those two reasons;
 * - no schedule hours are shown (they don't reach this screen — D-11 b was not chosen);
 * - there is no "ask for more time" or "unlock" anywhere: that function doesn't exist.
 */
data class BlockPresentation(
    /** The reason's icon. Not drawn since S66: lámina 07 shows one padlock for every reason. */
    val badgeIcon: Int,
    /** The reason's colour family. Not drawn since S66 (lámina 07 is navy, cream and blue). */
    val tone: NpTone,
    /** The small uppercase label of the navy panel. */
    val title: String,
    /** The serif headline of the reason. Same texts the old screen used. */
    val message: String,
    /** The icon and sentence of the hint. */
    val hintIcon: Int,
    val hint: String,
    /**
     * True when [hint] is drawn as the ruled row with its icon (lámina 07: the reasons that carry a
     * when/what clue — the two limits, the schedule and school time); false when it is the grey
     * detail under the serif [message], so no reason loses the text it already had.
     */
    val hintAsRow: Boolean = false,
)

const val BLOCK_TITLE = "APP BLOQUEADA"

/** Small print above "Ir al inicio". It tells the truth about the limit of the mechanism (the overlay
 * covers the app, it does not stop it) without explaining how to get around it. */
const val BLOCK_COVER_NOTE = "El bloqueo cubre la pantalla de la app, pero no la cierra."

fun blockMessage(reason: BlockReason): String = when (reason) {
    BlockReason.BLOCK -> "Tu tutor bloqueó esta app."
    BlockReason.DAILY_LIMIT -> "Ya usaste el tiempo diario permitido para esta app."
    BlockReason.WEEKLY_LIMIT -> "Ya usaste el tiempo semanal permitido para esta app."
    BlockReason.SCHEDULE -> "Esta app está bloqueada en este horario."
    BlockReason.CATEGORY -> "Tu tutor bloqueó la categoría a la que pertenece esta app."
    BlockReason.SCHOOL_MODE -> "Es horario escolar y esta app no está aprobada para este momento."
    BlockReason.DEFAULT_POLICY ->
        "Este dispositivo sólo permite las apps que tu tutor aprobó, y ésta no está aprobada."
}

/** [categoryLabel] is the real assigned category's Spanish name (or null if the app has none). */
fun blockPresentation(reason: BlockReason, categoryLabel: String?): BlockPresentation = when (reason) {
    BlockReason.BLOCK -> BlockPresentation(
        badgeIcon = NpIcons.User,
        tone = NpTone.Danger,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.User,
        hint = "Tu tutor decidió bloquear esta app en este dispositivo.",
    )
    BlockReason.DAILY_LIMIT -> BlockPresentation(
        badgeIcon = NpIcons.Clock,
        tone = NpTone.Warning,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.Clock,
        hint = "Ya usaste el tiempo permitido por hoy. El tiempo se reinicia mañana.",
        hintAsRow = true,
    )
    BlockReason.WEEKLY_LIMIT -> BlockPresentation(
        badgeIcon = NpIcons.Calendar,
        tone = NpTone.Warning,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.Calendar,
        hint = "Ya usaste el tiempo permitido esta semana. El tiempo se reinicia el lunes.",
        hintAsRow = true,
    )
    BlockReason.SCHEDULE -> BlockPresentation(
        badgeIcon = NpIcons.Moon,
        tone = NpTone.Purple,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.Moon,
        hint = "Esta app tiene un horario de uso definido por tu tutor.",
        hintAsRow = true,
    )
    BlockReason.CATEGORY -> BlockPresentation(
        badgeIcon = NpIcons.LayoutGrid,
        tone = NpTone.Danger,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.LayoutGrid,
        // "limita", not "bloquea": a category rule can be a limit or a schedule too.
        hint = if (categoryLabel != null) {
            "La categoría «$categoryLabel» tiene una regla de tu tutor que la limita en este momento."
        } else {
            "La categoría de esta app tiene una regla de tu tutor que la limita en este momento."
        },
    )
    BlockReason.SCHOOL_MODE -> BlockPresentation(
        badgeIcon = NpIcons.GraduationCap,
        tone = NpTone.Warning,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.GraduationCap,
        hint = "En horario escolar solo puedes usar las apps que tu tutor aprobó.",
        hintAsRow = true,
    )
    BlockReason.DEFAULT_POLICY -> BlockPresentation(
        badgeIcon = NpIcons.ShieldCheck,
        tone = NpTone.Info,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.ShieldCheck,
        hint = "Este dispositivo solo permite el uso de las apps que tu tutor aprobó.",
    )
}
