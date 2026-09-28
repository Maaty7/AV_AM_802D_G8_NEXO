package cl.duoc.nexo.utils

/**
 * Feriados legales de Chile para el año 2026, verificados contra 3 fuentes
 * independientes (feriadoslegales.cl, calculaferiados.cl, calendarr.com —
 * las 3 coinciden exactamente en las 16 fechas).
 *
 * ⚠️ Esta lista debe actualizarse manualmente cada año, idealmente en
 * enero, con el calendario oficial de feriados de Chile. Varios feriados
 * son movibles de un año a otro (Semana Santa, San Pedro y San Pablo,
 * Encuentro de Dos Mundos, Día Nacional de los Pueblos Indígenas), así que
 * NO se pueden calcular a partir del año anterior sin volver a verificar.
 */
object FeriadosChile {

    private val FERIADOS_2026 = setOf(
        "2026-01-01", // Año Nuevo
        "2026-04-03", // Viernes Santo
        "2026-04-04", // Sábado Santo
        "2026-05-01", // Día del Trabajo
        "2026-05-21", // Día de las Glorias Navales
        "2026-06-21", // Día Nacional de los Pueblos Indígenas
        "2026-06-29", // San Pedro y San Pablo
        "2026-07-16", // Virgen del Carmen
        "2026-08-15", // Asunción de la Virgen
        "2026-09-18", // Independencia Nacional
        "2026-09-19", // Día de las Glorias del Ejército
        "2026-10-12", // Encuentro de Dos Mundos
        "2026-10-31", // Día de las Iglesias Evangélicas y Protestantes
        "2026-11-01", // Día de Todos los Santos
        "2026-12-08", // Inmaculada Concepción
        "2026-12-25"  // Navidad
    )

    /** [fecha] en formato "yyyy-MM-dd". */
    fun esFeriado(fecha: String): Boolean = fecha in FERIADOS_2026
}
