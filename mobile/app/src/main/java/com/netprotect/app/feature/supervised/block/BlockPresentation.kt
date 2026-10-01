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
    /** The badge drawn over the padlock. */
    val badgeIcon: Int,
    /** The colour family of the badge and the hint card. */
    val tone: NpTone,
    val title: String,
    /** The sentence under the title. Same texts the old screen used. */
    val message: String,
    /** The icon and sentence of the coloured card under the app card. */
    val hintIcon: Int,
    val hint: String,
)

const val BLOCK_TITLE = "APP BLOQUEADA"

/** Small print under the hint. It tells the truth about the limit of the mechanism without
 * explaining how to get around it (the previous text named the way). */
const val BLOCK_COVER_NOTE = "Este bloqueo cubre la pantalla; la app puede seguir abierta en segundo plano."

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
    )
    BlockReason.WEEKLY_LIMIT -> BlockPresentation(
        badgeIcon = NpIcons.Calendar,
        tone = NpTone.Warning,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.Calendar,
        hint = "Ya usaste el tiempo permitido esta semana. El tiempo se reinicia el lunes.",
    )
    BlockReason.SCHEDULE -> BlockPresentation(
        badgeIcon = NpIcons.Moon,
        tone = NpTone.Purple,
        title = BLOCK_TITLE,
        message = blockMessage(reason),
        hintIcon = NpIcons.Moon,
        hint = "Esta app tiene un horario de uso definido por tu tutor.",
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
