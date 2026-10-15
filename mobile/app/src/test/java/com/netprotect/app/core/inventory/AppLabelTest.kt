package com.netprotect.app.core.inventory

import org.junit.Assert.assertEquals
import org.junit.Test

/** The backend accepts an app_label of 1 to 255 characters and rejects the WHOLE applications
 * sync for one bad entry. A real OPPO (Android 16) has a launcher app whose label resolves to an
 * empty string, so no usage at all reached the tutor until the label was made safe here.
 */
class AppLabelTest {

    @Test
    fun `an empty label falls back to the package name`() {
        assertEquals("com.example.nameless", AppInventoryCollector.displayLabel("", "com.example.nameless"))
    }

    @Test
    fun `a blank label falls back to the package name`() {
        assertEquals("com.example.blank", AppInventoryCollector.displayLabel("  \n ", "com.example.blank"))
    }

    @Test
    fun `a missing label falls back to the package name`() {
        assertEquals("com.example.none", AppInventoryCollector.displayLabel(null, "com.example.none"))
    }

    @Test
    fun `a real label is kept, trimmed`() {
        assertEquals("Cámara", AppInventoryCollector.displayLabel("  Cámara ", "com.oplus.camera"))
    }

    @Test
    fun `a label longer than the backend accepts is cut to its limit`() {
        val label = AppInventoryCollector.displayLabel("x".repeat(400), "com.example.long")

        assertEquals(255, label.length)
    }
}
