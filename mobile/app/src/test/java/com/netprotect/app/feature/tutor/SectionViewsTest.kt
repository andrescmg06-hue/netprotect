package com.netprotect.app.feature.tutor

import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.Geofence
import com.netprotect.app.core.network.GeofenceEvent
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.feature.tutor.sections.filterApps
import com.netprotect.app.feature.tutor.sections.geofencesView
import com.netprotect.app.feature.tutor.sections.locationView
import com.netprotect.app.feature.tutor.sections.sortApps
import com.netprotect.app.ui.components.monogramLetter
import com.netprotect.app.ui.components.monogramTone
import com.netprotect.app.ui.format.GeoMath
import com.netprotect.app.ui.format.eventTimeLabel
import com.netprotect.app.ui.format.usageLabel
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionViewsTest {

    // ---- geometry (D-05: "Dentro de" only when certain) ----------------------------------

    // One degree of latitude ≈ 111 195 m, so 0.001° ≈ 111.2 m.
    private val lat = 4.1500
    private val lon = -73.6300

    @Test
    fun distanceIsAboutOneHundredElevenMetersPerThousandthOfADegree() {
        val d = GeoMath.distanceMeters(lat, lon, lat + 0.001, lon)
        assertEquals(111.2, d, 0.5)
        assertEquals(0.0, GeoMath.distanceMeters(lat, lon, lat, lon), 1e-9)
    }

    @Test
    fun insideOnlyWhenDistancePlusAccuracyFitsTheRadius() {
        // 111 m from the centre, radius 300 m.
        assertTrue(GeoMath.certainlyInside(lat + 0.001, lon, 150.0, lat, lon, 300.0)) // 261 ≤ 300
        assertFalse(GeoMath.certainlyInside(lat + 0.001, lon, 200.0, lat, lon, 300.0)) // 311 > 300: only maybe
    }

    @Test
    fun accuracyLargerThanTheRadiusIsNeverInsideEvenAtTheCentre() {
        assertFalse(GeoMath.certainlyInside(lat, lon, 500.0, lat, lon, 300.0))
    }

    @Test
    fun exactlyOnTheLimitCountsAsInside() {
        assertTrue(GeoMath.certainlyInside(lat, lon, 300.0, lat, lon, 300.0))
    }

    @Test
    fun invalidInputsAreNeverInside() {
        assertFalse(GeoMath.certainlyInside(lat, lon, -1.0, lat, lon, 300.0))
        assertFalse(GeoMath.certainlyInside(lat, lon, 0.0, lat, lon, 0.0))
    }

    private fun zone(id: String, name: String, radius: Double, dLat: Double = 0.0) =
        Geofence(id, name, lat + dLat, lon, radius)

    private fun report(accuracy: Double) =
        LocationReport(lat, lon, accuracy, "2026-09-29T15:00:00Z", "2026-09-29T15:00:05Z")

    @Test
    fun locationPicksTheSmallestZoneThatCertainlyContainsTheReading() {
        val view = locationView(report(50.0), listOf(zone("a", "Barrio", 2_000.0), zone("b", "Casa", 150.0)))
        assertEquals("Casa", view.insideGeofence)
    }

    @Test
    fun locationSaysNothingWhenItIsOnlyMaybeInside() {
        val view = locationView(report(400.0), listOf(zone("b", "Casa", 150.0)))
        assertNull(view.insideGeofence)
        assertEquals(400.0, view.report!!.accuracyMeters, 0.0)
    }

    @Test
    fun noReportMeansNoZone() {
        val view = locationView(null, listOf(zone("b", "Casa", 150.0)))
        assertNull(view.report)
        assertNull(view.insideGeofence)
    }

    // ---- geofence last event --------------------------------------------------------------

    private fun event(id: String, zoneId: String?, name: String, type: String, at: String) =
        GeofenceEvent(id, name, type, at, zoneId)

    @Test
    fun lastEventIsMatchedByIdAndIsTheNewest() {
        val zones = listOf(zone("z1", "Casa", 150.0), zone("z2", "Casa", 300.0), zone("z3", "Biblioteca", 100.0))
        val events = listOf(
            event("e1", "z1", "Casa", "ENTER", "2026-09-29T13:00:00Z"),
            event("e2", "z1", "Casa", "EXIT", "2026-09-29T14:00:00Z"),
            event("e3", "z2", "Casa", "ENTER", "2026-09-28T09:00:00Z"),
            event("e4", null, "Zona borrada", "EXIT", "2026-09-29T15:00:00Z"),
        )
        val view = geofencesView(zones, events)
        assertEquals("e2", view.items.first { it.geofence.id == "z1" }.lastEvent!!.id)
        assertEquals("e3", view.items.first { it.geofence.id == "z2" }.lastEvent!!.id)
        assertNull(view.items.first { it.geofence.id == "z3" }.lastEvent)
        assertEquals(listOf("e4", "e2", "e1", "e3"), view.history.map { it.id })
    }

    // ---- apps ---------------------------------------------------------------------------------

    private fun app(label: String, seconds: Int?, uninstalled: Boolean = false, pkg: String = "com.$label") =
        DeviceApplicationSummary(pkg, label, false, if (uninstalled) "2026-09-20T00:00:00Z" else null, "2026-09-29", seconds)

    @Test
    fun appsInstalledFirstThenMostUsedThenByName() {
        val sorted = sortApps(
            listOf(app("Zeta", 60), app("TikTok", 3600, uninstalled = true), app("alfa", 60), app("YouTube", 5040), app("Sin", null)),
        )
        assertEquals(listOf("YouTube", "alfa", "Zeta", "Sin", "TikTok"), sorted.map { it.appLabel })
    }

    @Test
    fun searchIgnoresCaseAndAccentsAndMatchesThePackage() {
        val apps = listOf(app("Cámara", 10, pkg = "com.android.camera2"), app("YouTube", 10, pkg = "com.google.android.youtube"))
        assertEquals(listOf("Cámara"), filterApps(apps, "camara").map { it.appLabel })
        assertEquals(listOf("YouTube"), filterApps(apps, "GOOGLE").map { it.appLabel })
        assertEquals(2, filterApps(apps, "   ").size)
    }

    @Test
    fun usageSaysTheDayWhenItIsNotToday() {
        val today = LocalDate.of(2026, 9, 29)
        assertEquals("1 h 24 min", usageLabel(5040, "2026-09-29", today))
        assertEquals("12 min · ayer", usageLabel(720, "2026-09-28", today))
        assertEquals("12 min · 24 sep", usageLabel(720, "2026-09-24", today))
        assertEquals("12 min · 24 dic 2025", usageLabel(720, "2025-12-24", today))
        assertEquals("Sin uso registrado", usageLabel(null, null, today))
        assertEquals("Sin uso registrado", usageLabel(10, "not-a-date", today))
    }

    @Test
    fun eventTimeUsesTheTutorsZone() {
        val bogota = ZoneId.of("America/Bogota")
        val today = LocalDate.of(2026, 9, 29)
        assertEquals("Hoy, 8:12 a. m.", eventTimeLabel("2026-09-29T13:12:00Z", today, bogota))
        assertEquals("Ayer, 6:40 p. m.", eventTimeLabel("2026-09-28T23:40:00Z", today, bogota))
    }

    @Test
    fun monogramIsStableAndReadable() {
        assertEquals(monogramTone("com.whatsapp"), monogramTone("com.whatsapp"))
        assertEquals("W", monogramLetter("whatsApp", "com.whatsapp"))
        assertEquals("C", monogramLetter("  ", "com.example"))
    }
}
