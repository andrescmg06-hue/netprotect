package com.netprotect.app.ui.format

import com.netprotect.app.ui.icons.NpIcons
import com.netprotect.app.ui.theme.NpTone

/** Etiquetas de tipo de regla — textos **literales** del panel web (`ruleFormatting.ts`). */
object RuleLabels {
    fun ruleTypeLabel(type: String): String = when (type) {
        "ALLOW" -> "Permitir"
        "BLOCK" -> "Bloquear"
        "DAILY_LIMIT" -> "Límite diario"
        "WEEKLY_LIMIT" -> "Límite semanal"
        "SCHEDULE" -> "Horario"
        "CATEGORY" -> "Por categoría"
        "SCHOOL_MODE" -> "Horario escolar"
        "DEFAULT_POLICY" -> "Sin aprobar"
        else -> type
    }

    /** Sprint 46: icon and tone of a block reason (Estadísticas, Historial). App-level reasons in
     * red, time-based in amber, category/policy in violet; unknown values neutral. */
    fun ruleTypeIcon(type: String): Int = when (type) {
        "BLOCK" -> NpIcons.Ban
        "DAILY_LIMIT", "WEEKLY_LIMIT" -> NpIcons.Calendar
        "SCHEDULE" -> NpIcons.Clock
        "SCHOOL_MODE" -> NpIcons.GraduationCap
        "CATEGORY" -> NpIcons.LayoutGrid
        "DEFAULT_POLICY" -> NpIcons.Shield
        "ALLOW" -> NpIcons.Check
        else -> NpIcons.Ban
    }

    fun ruleTypeTone(type: String): NpTone = when (type) {
        "BLOCK", "DAILY_LIMIT", "WEEKLY_LIMIT" -> NpTone.Danger
        "SCHEDULE", "SCHOOL_MODE" -> NpTone.Warning
        "CATEGORY", "DEFAULT_POLICY" -> NpTone.Purple
        "ALLOW" -> NpTone.Success
        else -> NpTone.Neutral
    }
}
