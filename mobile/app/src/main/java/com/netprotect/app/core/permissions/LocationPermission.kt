package com.netprotect.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** What the user granted. Since Android 12 the same system dialog lets them choose precise or
 * approximate even when the app asks for ACCESS_FINE_LOCATION; choosing approximate grants only
 * ACCESS_COARSE_LOCATION.
 */
enum class LocationAccess { NONE, APPROXIMATE, PRECISE }

/** ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION are ordinary runtime permissions (unlike
 * PACKAGE_USAGE_STATS): the system dialog grants or denies them directly, no trip to Settings.
 *
 * Precise location since 2026-10-14, by explicit decision of the project owner: on a real phone
 * the approximate fix placed the device kilometres away, which made it useless to the tutor. That
 * replaces the Sprint 13 coarse-only choice — see docs/android/capability-matrix.md (Sprint 13).
 * The app treats [LocationAccess.APPROXIMATE] as an incomplete permission in the UI; the reporting
 * service still runs with it (NETWORK_PROVIDER only) because an approximate fix beats none.
 *
 * Still never ACCESS_BACKGROUND_LOCATION: a foreground service declaring
 * foregroundServiceType="location" already counts as "foreground" for Android's location
 * permission system while it runs, which is this project's actual mechanism (see
 * LocationReportingService) — requesting the background permission on top would only add a
 * Play policy obligation (Prominent Disclosure) this project doesn't need to take on.
 */
object LocationPermission {
    /** Both are requested together: Android 12+ requires COARSE alongside FINE. */
    val REQUESTED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun access(context: Context): LocationAccess = accessFrom(
        fineGranted = isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION),
        coarseGranted = isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION),
    )

    /** Any location at all (precise or approximate): what the reporting service needs to run. */
    fun isGranted(context: Context): Boolean = access(context) != LocationAccess.NONE

    fun isPreciseGranted(context: Context): Boolean = access(context) == LocationAccess.PRECISE

    internal fun accessFrom(fineGranted: Boolean, coarseGranted: Boolean): LocationAccess = when {
        fineGranted -> LocationAccess.PRECISE
        coarseGranted -> LocationAccess.APPROXIMATE
        else -> LocationAccess.NONE
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
