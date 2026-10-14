package com.netprotect.app.feature.supervised.battery

// Lámina 13.3, corrected so every sentence is true of the code. Sources: BatteryOptimization.isIgnoring
// (PowerManager.isIgnoringBatteryOptimizations, the standard Android whitelist), BatteryOptimization.settingsIntent
// (ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS, a list of apps, or NetProtect's page as the fallback) and the
// shell's ON_RESUME re-read. Optimization only *can* stop the app in the background, so nothing says it did; the
// slide's device model and manufacturer branch are left out (one text for every phone).

/** The row on Inicio, shown only while the optimization applies. */
const val BATTERY_HOME_TITLE = "La batería puede detener NetProtect"
const val BATTERY_HOME_SUBTITLE = "Toca para ver cómo revisarlo."

const val BATTERY_TITLE = "NetProtect puede detenerse en segundo plano"
const val BATTERY_BODY =
    "Android está optimizando la batería de NetProtect. Eso puede detenerlo en segundo plano, " +
        "y entonces tu tutor puede dejar de recibir reportes."
const val BATTERY_STEPS_HEADING = "Cómo arreglarlo"
const val BATTERY_NOTE =
    "Los nombres de las opciones cambian según el teléfono. Estos pasos todavía se están comprobando en teléfonos reales."
const val BATTERY_OPEN_SETTINGS = "Abrir ajustes de batería"
const val BATTERY_LATER = "Más tarde"

/** After coming back with the optimization off: what Android now reports, nothing more. */
const val BATTERY_DONE_TITLE = "Batería revisada"
const val BATTERY_DONE_TEXT = "Android confirma que no está optimizando la batería de NetProtect."
const val BATTERY_BACK_HOME = "Volver al inicio"

/** The three steps of the slide; the first one does not promise NetProtect's own page (the list may open). */
fun batterySteps(): List<String> = listOf(
    "Toca «$BATTERY_OPEN_SETTINGS». Si Android abre una lista de apps, busca NetProtect en ella.",
    "Elige «Sin restricciones» o «No optimizar».",
    "Vuelve a esta pantalla. Lo comprobamos automáticamente al volver.",
)

/** Every text the battery row and screen can show; for the wording tests. */
fun batteryTexts(): List<String> =
    listOf(BATTERY_HOME_TITLE, BATTERY_HOME_SUBTITLE, BATTERY_TITLE, BATTERY_BODY, BATTERY_STEPS_HEADING) +
        batterySteps() +
        listOf(BATTERY_NOTE, BATTERY_OPEN_SETTINGS, BATTERY_LATER, BATTERY_DONE_TITLE, BATTERY_DONE_TEXT, BATTERY_BACK_HOME)
