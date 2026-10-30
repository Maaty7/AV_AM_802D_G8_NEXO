package cl.duoc.nexo.domain

/**
 * Lógica pura de las pausas (recreos/descansos) dentro de una jornada — sin
 * dependencias de Android, para poder testearla directo. Reutiliza
 * [minutosDelDia]/[parsearHora] de JornadaLogic.kt.
 */

/**
 * Minutos transcurridos desde que empezó la jornada hasta el minuto del día
 * [minuto] (0-1439), resolviendo el cruce de medianoche de una jornada
 * nocturna (donde [horaInicioJornadaMin] > horaTerminoJornadaMin). El
 * resultado siempre cae en [0, 1439] — proyectar tanto la jornada como sus
 * pausas a este espacio relativo evita tener que decidir a qué día
 * calendario pertenece cada hora por separado.
 */
fun minutosDesdeInicioJornada(minuto: Int, horaInicioJornadaMin: Int): Int {
    val delta = minuto - horaInicioJornadaMin
    return if (delta < 0) delta + 24 * 60 else delta
}

/**
 * Valida que una pausa [nuevaInicio]-[nuevaTermino] ("HH:mm") quede dentro
 * de la jornada [horaInicioJornada]-[horaTerminoJornada] (soporta jornada
 * nocturna) y no se solape con ninguna de [otrasPausas] (lista de
 * (horaInicio, horaTermino) de las demás pausas de la misma jornada).
 * Devuelve `null` si es válida, o un mensaje de error listo para mostrar.
 *
 * - Una pausa pegada a un borde (empieza junto con la jornada, o termina
 *   junto con ella) es válida.
 * - Una pausa de duración cero (mismo inicio y término) es inválida.
 * - Dos pausas que se tocan sin solaparse (una termina justo cuando empieza
 *   la otra) se consideran válidas entre sí.
 *
 * Límite conocido: no distingue una pausa que termina exactamente en la
 * hora de inicio de la jornada (un posible "casi una vuelta completa") de
 * una pausa de duración cero — ambas quedan rechazadas. No hay caso de uso
 * real para una pausa que dure casi toda la jornada, así que no se resuelve
 * esa ambigüedad con más lógica.
 */
fun validarPausa(
    nuevaInicio: String,
    nuevaTermino: String,
    horaInicioJornada: String,
    horaTerminoJornada: String,
    otrasPausas: List<Pair<String, String>>
): String? {
    val horaInicioJornadaMin = minutosDelDia(horaInicioJornada)
    val duracionJornada = minutosDesdeInicioJornada(minutosDelDia(horaTerminoJornada), horaInicioJornadaMin)

    val inicioOffset = minutosDesdeInicioJornada(minutosDelDia(nuevaInicio), horaInicioJornadaMin)
    val terminoOffset = minutosDesdeInicioJornada(minutosDelDia(nuevaTermino), horaInicioJornadaMin)

    if (terminoOffset <= inicioOffset) {
        return "La hora de término debe ser posterior a la de inicio."
    }
    if (terminoOffset > duracionJornada) {
        return "La pausa debe quedar dentro del horario de la jornada ($horaInicioJornada–$horaTerminoJornada)."
    }

    otrasPausas.forEach { (otraInicio, otraTermino) ->
        val otraInicioOffset = minutosDesdeInicioJornada(minutosDelDia(otraInicio), horaInicioJornadaMin)
        val otraTerminoOffset = minutosDesdeInicioJornada(minutosDelDia(otraTermino), horaInicioJornadaMin)
        val seSolapan = inicioOffset < otraTerminoOffset && otraInicioOffset < terminoOffset
        if (seSolapan) {
            return "La pausa se solapa con \"$otraInicio–$otraTermino\"."
        }
    }

    return null
}

/**
 * Milisegundos absolutos [inicio, fin) de una pausa, dado el inicio absoluto
 * ya resuelto ([jornadaInicioMillis]) de la jornada a la que pertenece —
 * típicamente el `inicioMillis` que devuelve `calcularRangoJornada`.
 * Reutiliza el mismo desplazamiento de [minutosDesdeInicioJornada], así que
 * una pausa de una jornada nocturna queda ubicada correctamente sin tener
 * que resolver por separado a qué día calendario pertenece.
 */
fun resolverRangoPausa(
    pausaHoraInicio: String,
    pausaHoraTermino: String,
    horaInicioJornada: String,
    jornadaInicioMillis: Long
): Pair<Long, Long> {
    val horaInicioJornadaMin = minutosDelDia(horaInicioJornada)
    val inicioOffsetMin = minutosDesdeInicioJornada(minutosDelDia(pausaHoraInicio), horaInicioJornadaMin)
    val terminoOffsetMin = minutosDesdeInicioJornada(minutosDelDia(pausaHoraTermino), horaInicioJornadaMin)

    val inicioMillis = jornadaInicioMillis + inicioOffsetMin * 60_000L
    val finMillis = jornadaInicioMillis + terminoOffsetMin * 60_000L
    return inicioMillis to finMillis
}

/**
 * Los sub-rangos [inicio, fin) de una sesión [sesionInicio, sesionFin) que
 * quedan FUERA de todas las [pausas] (rangos [inicio, fin) en millis, ya
 * resueltos con [resolverRangoPausa]) — la sesión con "agujeros" donde caían
 * las pausas. Sin pausas, o si ninguna se solapa con la sesión, devuelve la
 * sesión completa sin modificar. Las pausas de una misma jornada ya se
 * validaron sin solaparse entre sí (ver [validarPausa]), así que no hace
 * falta fusionar rangos superpuestos acá.
 */
fun recortarPorPausas(sesionInicio: Long, sesionFin: Long, pausas: List<Pair<Long, Long>>): List<Pair<Long, Long>> {
    if (sesionFin <= sesionInicio) return emptyList()
    if (pausas.isEmpty()) return listOf(sesionInicio to sesionFin)

    val solapes = pausas
        .mapNotNull { (pausaInicio, pausaFin) ->
            val solapeInicio = maxOf(sesionInicio, pausaInicio)
            val solapeFin = minOf(sesionFin, pausaFin)
            if (solapeFin > solapeInicio) solapeInicio to solapeFin else null
        }
        .sortedBy { it.first }

    if (solapes.isEmpty()) return listOf(sesionInicio to sesionFin)

    val resultado = mutableListOf<Pair<Long, Long>>()
    var cursor = sesionInicio
    solapes.forEach { (pausaInicio, pausaFin) ->
        if (pausaInicio > cursor) resultado.add(cursor to pausaInicio)
        cursor = maxOf(cursor, pausaFin)
    }
    if (cursor < sesionFin) resultado.add(cursor to sesionFin)
    return resultado
}

/** Cuánto de una sesión de uso (en millis) cae dentro de alguna pausa, y cuánto queda fuera de todas. */
data class SesionDividida(val fueraDePausasMs: Long, val dentroDePausasMs: Long)

/**
 * Divide una sesión [sesionInicio, sesionFin) (millis) según cuánto cae
 * dentro de alguna de [pausas] y cuánto queda fuera de todas — el mismo
 * recorte de [recortarPorPausas], como un total agregado en vez de rangos.
 */
fun dividirPorPausas(sesionInicio: Long, sesionFin: Long, pausas: List<Pair<Long, Long>>): SesionDividida {
    val duracionTotal = (sesionFin - sesionInicio).coerceAtLeast(0L)
    val fuera = recortarPorPausas(sesionInicio, sesionFin, pausas).sumOf { (inicio, fin) -> fin - inicio }
    return SesionDividida(fueraDePausasMs = fuera, dentroDePausasMs = duracionTotal - fuera)
}
