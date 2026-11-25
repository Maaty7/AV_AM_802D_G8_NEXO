package cl.duoc.nexo.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────
// Roles semánticos que cambian según el tema. Las pantallas usan ESTOS
// nombres en vez de un hex o un token crudo, así nunca queda un color que
// se lea bien en claro y mal en oscuro (o al revés).
// ─────────────────────────────────────────────────────────────────────────

/** Texto secundario: Deep 65 % en claro, Mist 70 % en oscuro. */
val NexoTextoSecundario: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

/** Acento de marca para íconos, links y acciones de texto: Deep en claro, Sky en oscuro. */
val NexoAcento: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.primary

/** Fondo suave para íconos, pistas de barras y chips. */
val NexoContenedorSuave: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalNexoTemaOscuro.current) NexoDeep.copy(alpha = 0.55f) else NexoSkySoft.copy(alpha = 0.55f)

/** Bordes decorativos (divisores, inputs en reposo). */
val NexoBorde: Color
    @Composable @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

/** Texto de error. Siempre acompañado de un ícono: nunca depender solo del color. */
val NexoTextoError: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalNexoTemaOscuro.current) NexoNeon else NexoDeep

/** Fondo del badge/aviso "Pendiente" o alerta: neón 20 % (claro) / 15 % (oscuro). */
val NexoAlertaFondo: Color
    @Composable @ReadOnlyComposable
    get() = NexoNeon.copy(alpha = if (LocalNexoTemaOscuro.current) 0.15f else 0.20f)

/** Texto del badge/aviso "Pendiente": Deep en claro, Neon en oscuro. */
val NexoTextoAlerta: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalNexoTemaOscuro.current) NexoNeon else NexoDeep

/** Fondo de estado "éxito / activo": Sky suave. */
val NexoExitoFondo: Color
    @Composable @ReadOnlyComposable
    get() = NexoSky.copy(alpha = if (LocalNexoTemaOscuro.current) 0.18f else 0.22f)

/** Texto de estado "éxito / activo": Deep en claro (el Sky no se lee chico sobre blanco), Sky en oscuro. */
val NexoTextoExito: Color
    @Composable @ReadOnlyComposable
    get() = if (LocalNexoTemaOscuro.current) NexoSky else NexoDeep

/** Botón principal / CTA: relleno neón con texto e ícono Deep, en ambos temas. */
@Composable
fun nexoBotonPrincipalColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = NexoNeon,
    contentColor = NexoDeep,
    disabledContainerColor = NexoNeon.copy(alpha = 0.35f),
    disabledContentColor = NexoDeep.copy(alpha = 0.50f)
)

/** Botón secundario: relleno de marca (Deep en claro, Sky en oscuro). */
@Composable
fun nexoBotonSecundarioColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.primary,
    contentColor = MaterialTheme.colorScheme.onPrimary
)

/** Switch encendido en neón (pista) con perilla Deep. */
@Composable
fun nexoSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedTrackColor = NexoNeon,
    checkedThumbColor = NexoDeep,
    checkedBorderColor = NexoDeep.copy(alpha = 0.20f),
    checkedIconColor = NexoNeon,
    uncheckedTrackColor = NexoContenedorSuave,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline
)
