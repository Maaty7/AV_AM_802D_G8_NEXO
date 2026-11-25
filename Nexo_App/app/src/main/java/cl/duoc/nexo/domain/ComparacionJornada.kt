package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity

enum class TendenciaComparacion { SUBIO, BAJO, IGUAL }

/**
 * Compara un valor (tiempo total o de una categoría) entre el informe actual
 * y el anterior. Si [anterior] es 0, [porcentaje] queda en null — no tiene
 * sentido calcular un porcentaje de cambio contra una base de cero minutos.
 */
data class ComparacionValor(
    val actual: Long,
    val anterior: Long
) {
    val diferenciaMinutos: Long = actual - anterior
    val porcentaje: Double? = if (anterior == 0L) null else (diferenciaMinutos.toDouble() / anterior) * 100.0
    val tendencia: TendenciaComparacion = when {
        diferenciaMinutos > 0 -> TendenciaComparacion.SUBIO
        diferenciaMinutos < 0 -> TendenciaComparacion.BAJO
        else -> TendenciaComparacion.IGUAL
    }
}

data class ComparacionCategoria(val nombre: String, val valor: ComparacionValor)

data class ComparacionJornada(
    val total: ComparacionValor,
    val categorias: List<ComparacionCategoria>
)

private val NOMBRES_CATEGORIAS = listOf(
    "Educación", "Comunicación", "Redes Sociales", "Entretenimiento", "Juegos", "Otros"
)

private fun minutosPorCategoria(informe: ReportEntity): List<Long> = listOf(
    informe.minutosEducacion,
    informe.minutosComunicacion,
    informe.minutosRedesSociales,
    informe.minutosEntretenimiento,
    informe.minutosJuegos,
    informe.minutosOtros
)

/**
 * Compara [actual] contra la jornada anterior ([anterior], la misma jornada
 * — mismo `scheduleId` — más reciente con fecha previa a la de [actual]).
 * Devuelve null si no hay jornada anterior con la que comparar. Las
 * categorías en las que ambos informes tienen 0 minutos se omiten, para no
 * llenar la comparación con filas sin actividad en ninguno de los dos lados.
 */
fun calcularComparacionJornada(actual: ReportEntity, anterior: ReportEntity?): ComparacionJornada? {
    if (anterior == null) return null

    val actuales = minutosPorCategoria(actual)
    val anteriores = minutosPorCategoria(anterior)
    val categorias = NOMBRES_CATEGORIAS.indices
        .map { i -> ComparacionCategoria(NOMBRES_CATEGORIAS[i], ComparacionValor(actuales[i], anteriores[i])) }
        .filter { it.valor.actual > 0 || it.valor.anterior > 0 }

    return ComparacionJornada(
        total = ComparacionValor(actual.minutosTotales, anterior.minutosTotales),
        categorias = categorias
    )
}
