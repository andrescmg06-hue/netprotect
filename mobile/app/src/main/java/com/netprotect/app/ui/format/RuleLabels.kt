package com.netprotect.app.ui.format

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
}
