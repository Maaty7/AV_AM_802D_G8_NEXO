package cl.duoc.nexo.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Paleta NEXO v2 — celeste cielo + azul profundo + amarillo neón.
// Fuente única de verdad: ningún otro archivo debe declarar Color(0x…).
// Cualquier otro tono = uno de estos con .copy(alpha = …).
// Ver DESIGN.md para proporciones y reglas de contraste.
// ─────────────────────────────────────────────────────────────────────────

/** Celeste cielo — marca luminosa: gradientes, gráficos, íconos, estados activos. Nunca texto pequeño sobre blanco. */
val NexoSky = Color(0xFF38BDF8)

/** Azul profundo — marca seria: texto, encabezados, barras, fondos del tema oscuro. */
val NexoDeep = Color(0xFF0B2D6B)

/** Amarillo neón — SOLO para destacar (CTA, tab activo, badges, dato clave). Nunca texto sobre fondo claro. */
val NexoNeon = Color(0xFFE4FF3A)

/** Blanco frío — fondos y superficies del tema claro. */
val NexoMist = Color(0xFFF5FAFF)

// Tonos derivados permitidos (no agregar otros)
/** Fondo del tema oscuro (azul casi negro). */
val NexoNight = Color(0xFF061A3F)

/** Capa más profunda del tema oscuro. */
val NexoAbyss = Color(0xFF030E26)

/** Contenedores suaves y chips. */
val NexoSkySoft = Color(0xFFBFE8FC)

/** Neón en estado presionado / categoría Juegos. */
val NexoNeonDim = Color(0xFFB8D400)
