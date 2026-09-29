package com.netprotect.app.ui.format

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** Sprint 45 (D-05): everything here runs on the tutor's phone with data it already has. Nothing
 * is sent anywhere — no geocoding, no map tiles (the child's coordinates never leave the device). */
object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    /** Great-circle distance in meters (haversine). */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** True only when the whole uncertainty circle of the reading fits inside the zone:
     * distance + accuracy ≤ radius. A reading that merely *might* be inside is not "inside" —
     * saying "Dentro de «Casa»" about a child has to be certain, not plausible. */
    fun certainlyInside(
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double,
        centerLatitude: Double,
        centerLongitude: Double,
        radiusMeters: Double,
    ): Boolean {
        if (accuracyMeters < 0 || radiusMeters <= 0) return false
        val distance = distanceMeters(latitude, longitude, centerLatitude, centerLongitude)
        return distance + accuracyMeters <= radiusMeters
    }
}
