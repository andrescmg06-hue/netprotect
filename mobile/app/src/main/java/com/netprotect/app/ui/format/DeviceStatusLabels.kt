package com.netprotect.app.ui.format

import com.netprotect.app.ui.theme.NpTone

/** Estado del dispositivo → (etiqueta, tono). `ONLINE`/`OFFLINE`/`ALERT` son literales del panel web
 * (`deviceStatus.ts`); `SYNCING`/`RESTRICTED`/`UNLINKED` son nuevos (los da este encargo). */
object DeviceStatusLabels {
    fun label(status: String): Pair<String, NpTone> = when (status) {
        "ONLINE" -> "En línea" to NpTone.Success
        "OFFLINE" -> "Desconectado" to NpTone.Neutral
        "ALERT" -> "Alerta" to NpTone.Danger
        "SYNCING" -> "Sincronizando" to NpTone.Info
        "RESTRICTED" -> "Restringido" to NpTone.Warning
        "UNLINKED" -> "Desvinculado" to NpTone.Neutral
        else -> status to NpTone.Neutral
    }
}
