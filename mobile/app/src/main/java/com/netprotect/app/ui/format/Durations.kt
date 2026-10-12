package com.netprotect.app.ui.format

/** Duraciones ("menos de 1 min", "48 min", "1 h 24 min", "2 h"). Negativos se tratan como 0. */
object Durations {
    fun format(totalSeconds: Long): String {
        val s = if (totalSeconds < 0) 0L else totalSeconds
        if (s < 60L) return "menos de 1 min"
        if (s < 3600L) return "${s / 60L} min"
        val hours = s / 3600L
        val minutes = (s % 3600L) / 60L
        return if (minutes == 0L) "${hours} h" else "${hours} h ${minutes} min"
    }
}
