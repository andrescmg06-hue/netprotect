package com.netprotect.app.feature.supervised.linked

/** Line under "Permisos" in the Inicio tile (lámina 03): "2 pendientes", "1 pendiente" or "Todos configurados". */
fun permissionsTileLabel(pending: Int): String = when {
    pending <= 0 -> "Todos configurados"
    pending == 1 -> "1 pendiente"
    else -> "$pending pendientes"
}

/**
 * Status next to "Última comunicación". It follows the same 300 s rule as the headline, so the phone
 * never says "Conectado" before its first successful report.
 */
fun contactStatusLabel(reportRecent: Boolean): String =
    if (reportRecent) "Conectado" else "Sin actualización reciente"
