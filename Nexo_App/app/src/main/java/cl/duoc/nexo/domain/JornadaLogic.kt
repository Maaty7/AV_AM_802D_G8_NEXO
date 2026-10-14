package cl.duoc.nexo.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Lógica pura de horarios de jornada — sin dependencias de Android, para
 * poder testearla directo. [JornadaActivaViewModel]/[UsageStatsRepository]
 * la usan para decidir cuál es la jornada de referencia, si está activa
 * ahora mismo, y qué rango de tiempo real corresponde consultar.
 */

/** Convierte "HH:mm" a minutos desde medianoche (0-1439). Entradas inválidas caen a 0. */
fun minutosDelDia(hora: String): Int {
    val (h, m) = parsearHora(hora)
    return h * 60 + m
}

/** Convierte "HH:mm" a (hora, minuto). Entradas inválidas caen a (0, 0). */
fun parsearHora(hora: String): Pair<Int, Int> {
    val partes = hora.split(":")
    val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
    val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
    return h to m
}

/** La misma abreviación de 2 letras que usa ScheduleEntity.dias (LU/MA/MI/JU/VI/SA/DO). */
fun abreviacionDia(dia: DayOfWeek): String = when (dia) {
    DayOfWeek.MONDAY -> "LU"
    DayOfWeek.TUESDAY -> "MA"
    DayOfWeek.WEDNESDAY -> "MI"
    DayOfWeek.THURSDAY -> "JU"
    DayOfWeek.FRIDAY -> "VI"
    DayOfWeek.SATURDAY -> "SA"
    DayOfWeek.SUNDAY -> "DO"
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

/** Resultado de calcular el rango real (en millis) de una jornada para un día concreto. */
sealed interface RangoJornada {
    data class Valido(val inicioMillis: Long, val finMillis: Long) : RangoJornada
    data object Invalido : RangoJornada
}

/**
 * Calcula el rango real [horaInicio]-[horaTermino] de una jornada que
 * TERMINA el día [fechaBase] (si es una jornada nocturna, donde
 * horaInicio > horaTermino, el inicio cae en [fechaBase] menos 1 día).
 *
 * [fechaBase] siempre debe ser el día al que pertenece la jornada —
 * nunca "el día de hoy al momento de ejecutar" si se está generando un
 * informe fuera de horario (ver [calcularFechaObjetivo]), porque de lo
 * contrario el rango puede quedar invertido (fin <= inicio) y se
 * perdería silenciosamente el uso real.
 *
 * El fin se recorta a [ahoraMillis] si la jornada todavía está en curso
 * (para no "inventar" uso futuro). Devuelve [RangoJornada.Invalido] —
 * distinguible de "sin uso" — si el resultado queda con fin <= inicio.
 */
fun calcularRangoJornada(
    fechaBase: LocalDate,
    horaInicio: String,
    horaTermino: String,
    ahoraMillis: Long = System.currentTimeMillis(),
    zona: ZoneId = ZoneId.systemDefault()
): RangoJornada {
    val (hInicio, mInicio) = parsearHora(horaInicio)
    val (hTermino, mTermino) = parsearHora(horaTermino)

    val esNocturna = (hInicio * 60 + mInicio) > (hTermino * 60 + mTermino)
    val fechaInicioReal = if (esNocturna) fechaBase.minusDays(1) else fechaBase

    val inicioMillis = fechaInicioReal.atTime(hInicio, mInicio).atZone(zona).toInstant().toEpochMilli()
    val terminoMillis = fechaBase.atTime(hTermino, mTermino).atZone(zona).toInstant().toEpochMilli()
    val finMillis = minOf(terminoMillis, ahoraMillis)

    return if (finMillis <= inicioMillis) {
        RangoJornada.Invalido
    } else {
        RangoJornada.Valido(inicioMillis, finMillis)
    }
}

/**
 * La fecha a la que pertenece la ocurrencia más reciente de [horaTermino]
 * que ya pasó (<=  [ahora]) — es decir, "la jornada que corresponde
 * informar en este momento". Si el Worker corre pasada la medianoche (o
 * muy atrasado por Doze), esto da el día ANTERIOR, no el día de hoy.
 */
fun calcularFechaObjetivo(horaTermino: String, ahora: LocalDateTime = LocalDateTime.now()): LocalDate {
    val (h, m) = parsearHora(horaTermino)
    var candidato = ahora.toLocalDate().atTime(h, m)
    if (candidato.isAfter(ahora)) {
        candidato = candidato.minusDays(1)
    }
    return candidato.toLocalDate()
}
