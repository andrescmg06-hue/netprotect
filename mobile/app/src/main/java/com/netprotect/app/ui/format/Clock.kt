package com.netprotect.app.ui.format

import java.time.LocalTime

/** Hora en formato de 12 h ("3:42 p. m.", "12:05 a. m.", "12:00 p. m."). Medianoche = 12:xx a. m.,
 * mediodía = 12:xx p. m. Hora sin cero a la izquierda, minutos con dos dígitos, espacio normal
 * (U+0020) antes de "a. m."/"p. m." — construido a mano, no con el locale de la JVM. */
object Clock {
    fun format(time: LocalTime): String {
        val hour = time.hour
        val hour12 = if (hour % 12 == 0) 12 else hour % 12
        val period = if (hour < 12) "a. m." else "p. m."
        val minute = time.minute.toString().padStart(2, '0')
        return "$hour12:$minute $period"
    }
}
