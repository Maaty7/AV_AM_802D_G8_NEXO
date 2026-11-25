package cl.duoc.nexo.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NexoContenedorSuave
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.colorDeCategoria
import cl.duoc.nexo.ui.theme.esCategoriaNeon

/** Progreso 0→1 que arranca al aparecer (para barras y dona). */
@Composable
fun rememberProgresoAparicion(duracionMs: Int = 700, retrasoMs: Int = 0): Animatable<Float, AnimationVector1D> {
    val animar = rememberAnimacionesActivas()
    val progreso = remember { Animatable(if (animar) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (animar) {
            kotlinx.coroutines.delay(retrasoMs.toLong())
            progreso.animateTo(1f, tween(duracionMs, easing = NexoEaseOut))
        }
    }
    return progreso
}

/**
 * Dona de distribución por categoría, con los colores de CategoriaColores.
 * Se dibuja animada (barrido) al aparecer. En tema claro los segmentos
 * amarillos llevan un borde fino Deep 20 % para no perderse sobre blanco.
 */
@Composable
fun DonaCategorias(
    minutosPorCategoria: Map<String, Long>,
    modifier: Modifier = Modifier,
    tamano: Dp = 140.dp,
    grosor: Dp = 18.dp
) {
    val progreso = rememberProgresoAparicion(900)
    val oscuro = LocalNexoTemaOscuro.current
    val pista = NexoContenedorSuave
    val total = minutosPorCategoria.values.sum().coerceAtLeast(1L)
    val segmentos = minutosPorCategoria.entries.filter { it.value > 0 }.sortedByDescending { it.value }
    val descripcion = segmentos.joinToString { "${it.key} ${it.value * 100 / total} %" }

    Canvas(
        modifier
            .size(tamano)
            .semantics { contentDescription = "Distribución por categoría: $descripcion" }
    ) {
        val g = grosor.toPx()
        val arcoSize = Size(size.width - g, size.height - g)
        val topLeft = Offset(g / 2f, g / 2f)
        drawArc(pista, 0f, 360f, false, topLeft, arcoSize, style = Stroke(g))

        val barridoTotal = 360f * progreso.value
        var inicio = -90f
        val separacion = if (segmentos.size > 1) 2.5f else 0f
        segmentos.forEach { (categoria, minutos) ->
            val barrido = (minutos.toFloat() / total) * 360f
            val visible = (barridoTotal - (inicio + 90f)).coerceIn(0f, barrido)
            if (visible > separacion) {
                if (!oscuro && esCategoriaNeon(categoria)) {
                    drawArc(
                        NexoDeep.copy(alpha = 0.20f), inicio, visible - separacion, false,
                        topLeft, arcoSize, style = Stroke(g + 2.dp.toPx(), cap = StrokeCap.Butt)
                    )
                }
                drawArc(
                    colorDeCategoria(categoria), inicio, visible - separacion, false,
                    topLeft, arcoSize, style = Stroke(g, cap = StrokeCap.Butt)
                )
            }
            inicio += barrido
        }
    }
}

/**
 * Barra horizontal que crece al aparecer, con relleno degradado de la
 * paleta (el color de la categoría → una versión más clara).
 */
@Composable
fun BarraAnimada(
    fraccion: Float,
    color: Color,
    modifier: Modifier = Modifier,
    alto: Dp = 10.dp,
    bordeNeon: Boolean = false,
    retrasoMs: Int = 0
) {
    val progreso = rememberProgresoAparicion(700, retrasoMs)
    val pista = NexoContenedorSuave
    val oscuro = LocalNexoTemaOscuro.current
    Box(
        modifier
            .fillMaxWidth()
            .height(alto)
            .clip(CircleShape)
            .drawBehind {
                drawRect(pista)
                val ancho = size.width * fraccion.coerceIn(0f, 1f) * progreso.value
                if (ancho > 0f) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(color, color.copy(alpha = color.alpha * 0.7f)), endX = ancho),
                        size = Size(ancho, size.height),
                        cornerRadius = CornerRadius(size.height / 2f)
                    )
                    if (bordeNeon && !oscuro) {
                        drawRoundRect(
                            NexoDeep.copy(alpha = 0.20f),
                            size = Size(ancho, size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                            style = Stroke(1.dp.toPx())
                        )
                    }
                }
            }
    )
}
