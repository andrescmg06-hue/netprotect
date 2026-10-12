package com.netprotect.app.ui.format

/** Etiquetas de categoría (catálogo del Sprint 10) — textos **literales** del panel web
 * (`categoryFormatting.ts`). */
object CategoryLabels {
    fun categoryLabel(category: String): String = when (category) {
        "SOCIAL_MEDIA" -> "Redes sociales"
        "GAMES" -> "Juegos"
        "STREAMING" -> "Streaming"
        "EDUCATION" -> "Educación"
        "PRODUCTIVITY" -> "Productividad"
        "COMMUNICATION" -> "Comunicación"
        "NEWS" -> "Noticias"
        "SHOPPING" -> "Compras"
        "FINANCE" -> "Finanzas"
        "UTILITIES" -> "Utilidades"
        "ADULT_CONTENT" -> "Contenido para adultos"
        else -> category
    }
}
