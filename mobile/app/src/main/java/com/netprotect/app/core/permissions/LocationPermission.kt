package com.netprotect.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** What the user granted. Since Android 12 the same system dialog lets them choose precise or
 * approximate even when the app asks for ACCESS_FINE_LOCATION; choosing approximate grants only
 * ACCESS_COARSE_LOCATION.
 */
enum class LocationAccess { NONE, APPROXIMATE, PRECISE }

/** Sprint 73 (T6): the next step of the location permission. [BACKGROUND_EXPLANATION] means precise location is
 * granted and the background one is still missing: the shell shows its explanation, then the system request. */
enum class LocationRequest { FOREGROUND_DIALOG, BACKGROUND_EXPLANATION }

/** ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION are ordinary runtime permissions (unlike
 * PACKAGE_USAGE_STATS): the system dialog grants or denies them directly, no trip to Settings.
 *
 * Precise location since 2026-10-14, by explicit decision of the project owner: on a real phone
 * the approximate fix placed the device kilometres away, which made it useless to the tutor. That
 * replaces the Sprint 13 coarse-only choice — see docs/android/capability-matrix.md (Sprint 13).
 * The app treats [LocationAccess.APPROXIMATE] as an incomplete permission in the UI; the reporting
 * service still runs with it (NETWORK_PROVIDER only) because an approximate fix beats none.
 *
 * Sprint 73 (T6): ACCESS_BACKGROUND_LOCATION ("Permitir todo el tiempo") is now requested too, by
 * explicit decision of the project owner (2026-10-15), which reverses the Sprint 13/14 choice of never
 * asking for it: the `location`-typed foreground-service trick keeps reporting while the process lives, but
 * from Android 11 (API 30) a service started from the background (the system restarting it after killing the
 * process) cannot read the location without it, so reporting stopped until the app was reopened. From API 30
 * the system dialog no longer offers it together with the foreground permission: it is requested on its own,
 * after precise location, and the system sends the person to Settings (hence the explanation shown first,
 * see SupervisedShell). Below API 30 there is no such restriction and it is not asked. Not having it never
 * blocks anything: location reporting works as before while the app or its service is alive.
 */
object LocationPermission {
    /** Both are requested together: Android 12+ requires COARSE alongside FINE. */
    val REQUESTED = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun access(context: Context): LocationAccess = accessFrom(
        fineGranted = isPermissionGranted(context, Manifest.permission.ACCESS_FINE_LOCATION),
        coarseGranted = isPermissionGranted(context, Manifest.permission.ACCESS_COARSE_LOCATION),
    )

    /** The background permission, requested on its own from API 30 (see the class comment). */
    const val BACKGROUND = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    /** Any location at all (precise or approximate): what the reporting service needs to run. */
    fun isGranted(context: Context): Boolean = access(context) != LocationAccess.NONE

    /** Whether the background-start restriction applies and the permission is therefore needed. */
    fun backgroundNeeded(): Boolean = backgroundNeeded(Build.VERSION.SDK_INT)

    /** True when the background permission is not needed on this Android version or is granted. */
    fun backgroundReady(context: Context): Boolean = backgroundReady(
        sdkInt = Build.VERSION.SDK_INT,
        backgroundGranted = isPermissionGranted(context, BACKGROUND),
    )

    /** From Android 11 a location foreground service started from the background needs the permission. */
    internal fun backgroundNeeded(sdkInt: Int): Boolean = sdkInt >= Build.VERSION_CODES.R

    internal fun backgroundReady(sdkInt: Int, backgroundGranted: Boolean): Boolean =
        !backgroundNeeded(sdkInt) || backgroundGranted

    /** What the "Ubicación" permission asks for next, given what is granted and whether the background
     * permission is ready. Approximate goes first: the person is asked to change to precise (existing flow)
     * before the background step, which only follows precise location.
     */
    internal fun nextRequest(access: LocationAccess, backgroundReady: Boolean): LocationRequest = when {
        access != LocationAccess.PRECISE -> LocationRequest.FOREGROUND_DIALOG
        !backgroundReady -> LocationRequest.BACKGROUND_EXPLANATION
        else -> LocationRequest.FOREGROUND_DIALOG
    }

    internal fun accessFrom(fineGranted: Boolean, coarseGranted: Boolean): LocationAccess = when {
        fineGranted -> LocationAccess.PRECISE
        coarseGranted -> LocationAccess.APPROXIMATE
        else -> LocationAccess.NONE
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
