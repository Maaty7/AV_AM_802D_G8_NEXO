package cl.duoc.nexo.domain

/** Uso total del teléfono (todas las apps) dentro de un bloque de tiempo fijo, para graficar la actividad a lo largo de la jornada. */
data class FranjaUso(
    val etiqueta: String,
    val minutos: Long
)
