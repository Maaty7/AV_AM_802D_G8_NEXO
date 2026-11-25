package cl.duoc.nexo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NEXOTheme
import cl.duoc.nexo.ui.theme.NUMEROS_TABULARES
import cl.duoc.nexo.ui.theme.NexoAlertaFondo
import cl.duoc.nexo.ui.theme.NexoContenedorSuave
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoExitoFondo
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoTextoAlerta
import cl.duoc.nexo.ui.theme.NexoTextoError
import cl.duoc.nexo.ui.theme.NexoTextoExito
import cl.duoc.nexo.ui.theme.NexoTextoSecundario
import cl.duoc.nexo.ui.theme.nexoBotonPrincipalColors

/**
 * Botón principal / CTA: relleno neón, texto e ícono Deep, forma de
 * cápsula, 52 dp de alto. Al presionar baja a 0.96 con spring y, en tema
 * oscuro, el glow neón se intensifica.
 */
@Composable
fun NexoBotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icono: ImageVector? = null,
    enabled: Boolean = true
) {
    val interaccion = remember { MutableInteractionSource() }
    val presionado by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionado) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh),
        label = "escalaBoton"
    )
    val oscuro = LocalNexoTemaOscuro.current
    val glow by animateFloatAsState(
        targetValue = when {
            !oscuro || !enabled -> 0f
            presionado -> 1f
            else -> 0.5f
        },
        animationSpec = tween(160),
        label = "glowBoton"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interaccion,
        shape = CircleShape,
        colors = nexoBotonPrincipalColors(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .defaultMinSize(minHeight = 52.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .drawBehind {
                if (glow > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            listOf(NexoNeon.copy(alpha = 0.35f * glow), Color.Transparent),
                            center = center,
                            radius = size.maxDimension * 0.75f
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(-12.dp.toPx(), -12.dp.toPx()),
                        size = androidx.compose.ui.geometry.Size(size.width + 24.dp.toPx(), size.height + 24.dp.toPx()),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height)
                    )
                }
            }
    ) {
        if (icono != null) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelLarge)
    }
}

/** Mensaje de error: SIEMPRE ícono + texto (nunca depender solo del color). */
@Composable
fun MensajeError(texto: String, modifier: Modifier = Modifier, color: Color = NexoTextoError) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.ErrorOutline, contentDescription = "Error", tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = color, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Start)
    }
}

/** Encabezado de sección en MAYÚSCULAS (label medium, texto secundario). */
@Composable
fun EncabezadoSeccion(texto: String, modifier: Modifier = Modifier) {
    Text(
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = NexoTextoSecundario,
        modifier = modifier.padding(bottom = 12.dp)
    )
}

enum class TipoEstado { EXITO, ALERTA, ERROR, NEUTRO }

/** Badge de estado con ícono + texto, usando los colores semánticos (sin colores nuevos). */
@Composable
fun ChipEstado(texto: String, tipo: TipoEstado, modifier: Modifier = Modifier) {
    val (fondo, contenido, icono) = when (tipo) {
        TipoEstado.EXITO -> Triple(NexoExitoFondo, NexoTextoExito, Icons.Filled.CheckCircle)
        TipoEstado.ALERTA -> Triple(NexoAlertaFondo, NexoTextoAlerta, Icons.Filled.WarningAmber)
        TipoEstado.ERROR -> Triple(NexoNeon, NexoDeep, Icons.Filled.ErrorOutline)
        TipoEstado.NEUTRO -> Triple(NexoContenedorSuave, NexoTextoSecundario, Icons.Filled.Schedule)
    }
    Row(
        modifier = modifier
            .background(fondo, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, contentDescription = null, tint = contenido, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = contenido, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Número que hace count-up al aparecer (600 ms, curva de salida fuerte).
 * Con "Quitar animaciones" muestra el valor final de inmediato.
 */
@Composable
fun ContadorAnimado(
    valor: Long,
    modifier: Modifier = Modifier,
    sufijo: String = "",
    estilo: TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = Color.Unspecified
) {
    val animar = rememberAnimacionesActivas()
    val progreso = remember { Animatable(if (animar) 0f else 1f) }
    LaunchedEffect(valor) {
        if (animar) progreso.animateTo(1f, tween(600, easing = NexoEaseOut))
    }
    Text(
        text = "${(valor * progreso.value).toLong()}$sufijo",
        style = estilo.copy(fontFeatureSettings = NUMEROS_TABULARES),
        color = color,
        modifier = modifier
    )
}

/**
 * El dato más importante de una tarjeta: en claro, píldora neón con texto
 * Deep; en oscuro, texto neón (el neón nunca es texto sobre fondo claro).
 */
@Composable
fun DatoClave(texto: String, modifier: Modifier = Modifier) {
    val oscuro = LocalNexoTemaOscuro.current
    if (oscuro) {
        Text(texto, color = NexoNeon, style = MaterialTheme.typography.titleMedium, modifier = modifier)
    } else {
        Text(
            texto,
            color = NexoDeep,
            style = MaterialTheme.typography.titleMedium,
            modifier = modifier
                .background(NexoNeon, CircleShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Preview(name = "Controles — claro", showBackground = true, backgroundColor = 0xFFF5FAFF)
@Composable
private fun ControlesPreviewClaro() {
    NEXOTheme(darkTheme = false) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NexoBotonPrincipal("Continuar", onClick = {})
            ChipEstado("Concedido", TipoEstado.EXITO)
            ChipEstado("Pendiente", TipoEstado.ALERTA)
            MensajeError("PIN incorrecto, intenta de nuevo")
            DatoClave("32 % distracción")
            ContadorAnimado(128, sufijo = " min")
        }
    }
}

@Preview(name = "Controles — oscuro", showBackground = true, backgroundColor = 0xFF061A3F)
@Composable
private fun ControlesPreviewOscuro() {
    NEXOTheme(darkTheme = true) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NexoBotonPrincipal("Continuar", onClick = {})
            ChipEstado("Concedido", TipoEstado.EXITO)
            ChipEstado("Pendiente", TipoEstado.ALERTA)
            MensajeError("PIN incorrecto, intenta de nuevo")
            DatoClave("32 % distracción")
        }
    }
}

/**
 * Aviso de permiso de "Acceso a datos de uso" pendiente. Antes estaba
 * duplicado en Home y Estadísticas; ahora es un solo componente.
 */
@Composable
fun AvisoPermisoUso(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    GlassCard(modifier.fillMaxWidth()) {
        ChipEstado("Pendiente", TipoEstado.ALERTA)
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Falta el permiso de acceso a datos de uso",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Sin este permiso NEXO no puede medir el uso real del teléfono.",
            style = MaterialTheme.typography.bodyMedium,
            color = NexoTextoSecundario,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        NexoBotonPrincipal(
            texto = "Abrir ajustes",
            onClick = { cl.duoc.nexo.monitoring.UsageAccessPermission.openSettings(context) }
        )
    }
}

/** Indicador de pasos del onboarding: el paso actual en neón y más ancho. */
@Composable
fun IndicadorPasos(actual: Int, total: Int, modifier: Modifier = Modifier) {
    val pendiente = NexoContenedorSuave
    val hecho = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = "Paso $actual de $total" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (paso in 1..total) {
            val ancho by animateFloatAsState(if (paso == actual) 28f else 8f, label = "anchoPaso")
            Spacer(
                Modifier
                    .height(8.dp)
                    .width(ancho.dp)
                    .background(
                        when {
                            paso == actual -> NexoNeon
                            paso < actual -> hecho
                            else -> pendiente
                        },
                        CircleShape
                    )
            )
        }
    }
}
