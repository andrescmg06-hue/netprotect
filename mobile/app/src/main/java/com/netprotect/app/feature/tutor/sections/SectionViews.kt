package com.netprotect.app.feature.tutor.sections

import com.netprotect.app.core.network.DeviceApplicationSummary
import com.netprotect.app.core.network.Geofence
import com.netprotect.app.core.network.GeofenceEvent
import com.netprotect.app.core.network.LocationReport
import com.netprotect.app.ui.format.GeoMath
import java.time.Instant

/** Sprint 45: what the Apps, Ubicación and Geocercas screens receive, already computed. The rules
 * live here (pure, tested on the JVM) so the screens only draw. */

// ---- Apps ---------------------------------------------------------------------------------

/** Installed apps first, then by the latest known usage (most first), ties by name. Uninstalled
 * apps go last: they are history, not what the child uses now. */
fun sortApps(apps: List<DeviceApplicationSummary>): List<DeviceApplicationSummary> =
    apps.sortedWith(
        compareBy<DeviceApplicationSummary> { it.uninstalledAt != null }
            .thenByDescending { it.latestUsageSeconds ?: -1 }
            .thenBy { it.appLabel.lowercase() },
    )

/** Local filter for the search box: by app name or package, case- and accent-insensitive. */
fun filterApps(apps: List<DeviceApplicationSummary>, query: String): List<DeviceApplicationSummary> {
    val needle = normalize(query.trim())
    if (needle.isEmpty()) return apps
    return apps.filter { normalize(it.appLabel).contains(needle) || it.packageName.lowercase().contains(needle) }
}

private fun normalize(text: String): String =
    java.text.Normalizer.normalize(text.lowercase(), java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

// ---- Ubicación ----------------------------------------------------------------------------

/** The latest report (null = never reported / aged out) and, if the reading is *certainly* inside
 * one of the tutor's zones, that zone's name. With several, the smallest zone (most specific). */
data class LocationView(val report: LocationReport?, val insideGeofence: String?)

fun locationView(report: LocationReport?, geofences: List<Geofence>): LocationView {
    if (report == null) return LocationView(null, null)
    val inside = geofences
        .filter {
            GeoMath.certainlyInside(
                report.latitude, report.longitude, report.accuracyMeters,
                it.latitude, it.longitude, it.radiusMeters,
            )
        }
        .minByOrNull { it.radiusMeters }
    return LocationView(report, inside?.name)
}

// ---- Geocercas ----------------------------------------------------------------------------

data class GeofenceItem(val geofence: Geofence, val lastEvent: GeofenceEvent?)

/** Every zone with its most recent ENTER/EXIT (matched by id, not by name), plus the full history
 * newest first. Events of deleted zones stay in the history but belong to no zone. */
data class GeofencesView(val items: List<GeofenceItem>, val history: List<GeofenceEvent>)

fun geofencesView(geofences: List<Geofence>, events: List<GeofenceEvent>): GeofencesView {
    val newestFirst = events.sortedByDescending { parseInstant(it.occurredAt) ?: Instant.MIN }
    val lastById = newestFirst.filter { it.geofenceId != null }.groupBy { it.geofenceId }.mapValues { it.value.first() }
    return GeofencesView(
        items = geofences.map { GeofenceItem(it, lastById[it.id]) },
        history = newestFirst,
    )
}

private fun parseInstant(text: String): Instant? = runCatching { Instant.parse(text) }.getOrNull()
