package cl.duoc.nexo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color por categoría — fuente única de verdad para el dashboard de
 * Estadísticas, la Home y el PDF de informes.
 *
 * Lo productivo va en azules y las distracciones en amarillo neón (el color
 * que "llama la atención" = lo que hay que vigilar).
 */
val coloresPorCategoria: Map<String, Color> = mapOf(
    "Educación" to NexoDeep,
    "Comunicación" to NexoSky,
    "Redes Sociales" to NexoNeon,
    "Entretenimiento" to NexoNeon.copy(alpha = 0.65f),
    "Juegos" to NexoNeonDim,
    "Otros" to NexoSky.copy(alpha = 0.40f)
)

/** Color para una categoría desconocida (cae en "Otros"). */
val colorCategoriaPorDefecto: Color = NexoSky.copy(alpha = 0.40f)

/** Las categorías amarillas necesitan un borde fino sobre fondo claro para no perderse. */
fun esCategoriaNeon(categoria: String): Boolean =
    categoria == "Redes Sociales" || categoria == "Entretenimiento" || categoria == "Juegos"

fun colorDeCategoria(categoria: String?): Color =
    categoria?.let { coloresPorCategoria[it] } ?: colorCategoriaPorDefecto
