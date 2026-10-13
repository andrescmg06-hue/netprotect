package com.netprotect.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/** Sprint 63: whether the phone has a network at all, to tell "no Internet" apart from "the
 * NetProtect service does not answer" when a request fails with an [java.io.IOException].
 *
 * Uses [NetworkCapabilities.NET_CAPABILITY_INTERNET], not `NET_CAPABILITY_VALIDATED`: a captive
 * portal or a network Android has not validated yet still counts as "has network". Telling such a
 * user "no hay conexión a Internet" while the Wi-Fi icon is on would be wrong; the service-down
 * message is the safer reading there. Needs `ACCESS_NETWORK_STATE`, a normal permission (no dialog).
 *
 * Before [init] runs (e.g. a background worker in a process with no Activity yet) it reports
 * online, so a failure falls back to the service-down message instead of guessing "no Internet". */
object NetworkState {
    @Volatile
    private var connectivity: ConnectivityManager? = null

    fun init(context: Context) {
        if (connectivity == null) {
            connectivity = context.applicationContext.getSystemService(ConnectivityManager::class.java)
        }
    }

    fun isOnline(): Boolean {
        val manager = connectivity ?: return true
        return try {
            val network = manager.activeNetwork ?: return false
            val capabilities = manager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: SecurityException) {
            true
        }
    }
}
