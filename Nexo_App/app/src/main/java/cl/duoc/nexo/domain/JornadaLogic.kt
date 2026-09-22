package cl.duoc.nexo.domain

/**
 * Lógica pura de horarios de jornada — sin dependencias de Android, para
 * poder testearla directo. [JornadaActivaViewModel] la usa para decidir
 * cuál es la jornada de referencia y si está activa ahora mismo.
 */

/** Convierte "HH:mm" a minutos desde medianoche (0-1439). Entradas inválidas caen a 0. */
fun minutosDelDia(hora: String): Int {
    val partes = hora.split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
    return h * 60 + m
}

/**
 * Indica si una jornada [horaInicioMin]-[horaTerminoMin] está activa en el
 * minuto [horaActualMin] del día. Soporta jornadas que cruzan medianoche
 * (ej. horario nocturno 22:00-06:00), donde horaInicioMin > horaTerminoMin.
 */
fun jornadaEstaActiva(horaActualMin: Int, horaInicioMin: Int, horaTerminoMin: Int): Boolean {
    return if (horaInicioMin <= horaTerminoMin) {
        horaActualMin in horaInicioMin..horaTerminoMin
    } else {
        horaActualMin >= horaInicioMin || horaActualMin <= horaTerminoMin
    }
}
