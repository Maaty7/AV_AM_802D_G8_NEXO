package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class RangoFechas(val desde: LocalDate, val hasta: LocalDate)

data class RangosComparacionSemanal(val estaSemana: RangoFechas, val semanaAnterior: RangoFechas)

/**
 * A partir de la fecha de un informe, calcula el rango "lunes de esta semana
 * hasta esa fecha" y el rango equivalente de la semana anterior (lunes
 * anterior hasta el mismo día de la semana) — los informes de la MISMA
 * jornada dentro de cada rango se suman y se comparan en "Esta semana vs.
 * semana anterior".
 */
fun calcularRangosComparacionSemanal(fecha: LocalDate): RangosComparacionSemanal {
    val lunesEstaSemana = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lunesSemanaAnterior = lunesEstaSemana.minusWeeks(1)
    val mismoDiaSemanaAnterior = fecha.minusWeeks(1)
    return RangosComparacionSemanal(
        estaSemana = RangoFechas(lunesEstaSemana, fecha),
        semanaAnterior = RangoFechas(lunesSemanaAnterior, mismoDiaSemanaAnterior)
    )
}

data class ComparacionSemanal(
    val total: ComparacionValor,
    val categorias: List<ComparacionCategoria>
)

/**
 * Suma los informes de [informesEstaSemana] y los compara contra la suma de
 * [informesSemanaAnterior] (los mismos rangos de [calcularRangosComparacionSemanal]).
 * Si no hay ningún informe de la semana anterior, no hay nada con qué
 * comparar: devuelve null. Un día sin informe (feriado, día sin clases)
 * simplemente no está en la lista — no suma nada, no hace falta tratarlo
 * distinto.
 */
fun calcularComparacionSemanal(
    informesEstaSemana: List<ReportEntity>,
    informesSemanaAnterior: List<ReportEntity>
): ComparacionSemanal? {
    if (informesSemanaAnterior.isEmpty()) return null

    val totalActual = informesEstaSemana.sumOf { it.minutosTotales }
    val totalAnterior = informesSemanaAnterior.sumOf { it.minutosTotales }

    val categorias = NOMBRES_CATEGORIAS.indices
        .map { i ->
            val actual = informesEstaSemana.sumOf { minutosPorCategoria(it)[i] }
            val anterior = informesSemanaAnterior.sumOf { minutosPorCategoria(it)[i] }
            ComparacionCategoria(NOMBRES_CATEGORIAS[i], ComparacionValor(actual, anterior))
        }
        .filter { it.valor.actual > 0 || it.valor.anterior > 0 }

    return ComparacionSemanal(
        total = ComparacionValor(totalActual, totalAnterior),
        categorias = categorias
    )
}
