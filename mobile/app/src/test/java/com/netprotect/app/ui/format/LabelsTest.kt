package com.netprotect.app.ui.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelsTest {

    @Test
    fun categoryLabels() {
        val expected = mapOf(
            "SOCIAL_MEDIA" to "Redes sociales",
            "GAMES" to "Juegos",
            "STREAMING" to "Streaming",
            "EDUCATION" to "Educación",
            "PRODUCTIVITY" to "Productividad",
            "COMMUNICATION" to "Comunicación",
            "NEWS" to "Noticias",
            "SHOPPING" to "Compras",
            "FINANCE" to "Finanzas",
            "UTILITIES" to "Utilidades",
            "ADULT_CONTENT" to "Contenido para adultos",
        )
        for ((category, label) in expected) {
            assertEquals(label, CategoryLabels.categoryLabel(category))
            assertTrue(CategoryLabels.categoryLabel(category).isNotEmpty())
            assertNotEquals(category, CategoryLabels.categoryLabel(category))
        }
    }

    @Test
    fun unknownValuesFallBackToTheRawValue() {
        assertEquals("NUEVO_VALOR", CategoryLabels.categoryLabel("NUEVO_VALOR"))
    }
}
