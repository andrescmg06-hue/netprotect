package com.netprotect.app.feature.tutor

import com.netprotect.app.ui.navigation.NavStack

/** Sprint 44 (D-01, D-08): every place the tutor can be. The four tabs of the bottom bar are the
 * possible roots of the stack; a device detail and its six sections are pushed on top. */
sealed interface TutorRoute {
    data object Home : TutorRoute
    data object Devices : TutorRoute
    data object Activity : TutorRoute
    data object More : TutorRoute
    data class Detail(val deviceId: String) : TutorRoute
    data class Section(val deviceId: String, val section: DeviceSection) : TutorRoute
}

enum class DeviceSection { Apps, Location, Geofences, History, Statistics, Alerts }

enum class TutorTab(val root: TutorRoute) {
    Home(TutorRoute.Home),
    Devices(TutorRoute.Devices),
    Activity(TutorRoute.Activity),
    More(TutorRoute.More),
}

/** The highlighted tab is the one at the bottom of the stack, so a device opened from Inicio keeps
 * Inicio selected and one opened from Dispositivos keeps Dispositivos selected. */
val NavStack<TutorRoute>.currentTab: TutorTab
    get() = TutorTab.entries.firstOrNull { it.root == entries.first() } ?: TutorTab.Home

/** Tapping a tab starts that tab from its root. Tapping the tab you are already in also goes back
 * to its root (the usual Android behaviour). */
fun NavStack<TutorRoute>.selectTab(tab: TutorTab): NavStack<TutorRoute> = resetTo(tab.root)

/** System back: one level up; from the root of any tab other than Inicio, to Inicio; from Inicio,
 * null — the system handles it (leaves the app). */
fun NavStack<TutorRoute>.back(): NavStack<TutorRoute>? = when {
    canGoBack -> pop()
    current != TutorRoute.Home -> resetTo(TutorRoute.Home)
    else -> null
}

fun encodeTutorRoute(route: TutorRoute): String = when (route) {
    TutorRoute.Home -> "home"
    TutorRoute.Devices -> "devices"
    TutorRoute.Activity -> "activity"
    TutorRoute.More -> "more"
    is TutorRoute.Detail -> "detail/${route.deviceId}"
    is TutorRoute.Section -> "section/${route.deviceId}/${route.section.name}"
}

fun decodeTutorRoute(text: String): TutorRoute? {
    val parts = text.split("/")
    return when (parts.first()) {
        "home" -> TutorRoute.Home
        "devices" -> TutorRoute.Devices
        "activity" -> TutorRoute.Activity
        "more" -> TutorRoute.More
        "detail" -> parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.let(TutorRoute::Detail)
        "section" -> {
            val id = parts.getOrNull(1)?.takeIf { it.isNotBlank() }
            val section = parts.getOrNull(2)?.let { name -> DeviceSection.entries.firstOrNull { it.name == name } }
            if (id != null && section != null) TutorRoute.Section(id, section) else null
        }
        else -> null
    }
}
