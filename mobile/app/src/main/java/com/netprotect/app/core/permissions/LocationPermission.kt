package com.netprotect.app.core.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

/** ACCESS_COARSE_LOCATION is an ordinary runtime permission (unlike PACKAGE_USAGE_STATS): the
 * system dialog grants or denies it directly, no trip to Settings required. Deliberately only
 * coarse, never ACCESS_FINE_LOCATION — see docs/android/capability-matrix.md (Sprint 13) for why
 * approximate location is enough for a ~15-minute reporting interval and requesting more would
 * violate data minimization for no benefit this feature needs.
 *
 * Also deliberately never ACCESS_BACKGROUND_LOCATION: a foreground service declaring
 * foregroundServiceType="location" already counts as "foreground" for Android's location
 * permission system while it runs, which is this project's actual mechanism (see
 * LocationReportingService) — requesting the background permission on top would only add a
 * Play policy obligation (Prominent Disclosure) this project doesn't need to take on.
 */
object LocationPermission {
    fun isGranted(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
}
