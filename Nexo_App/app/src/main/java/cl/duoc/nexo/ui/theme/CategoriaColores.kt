package cl.duoc.nexo.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Color de marca por categoría — fuente única de verdad, para que el
 * dashboard de Estadísticas y el PDF de informes se vean coherentes entre sí.
 */
val coloresPorCategoria: Map<String, Color> = mapOf(
    "Educación" to NexoDeep,
    "Comunicación" to NexoTeal,
    "Redes Sociales" to NexoViolet,
    "Entretenimiento" to NexoCoral,
    "Juegos" to NexoAmber,
    "Otros" to NexoMute
)
