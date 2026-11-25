package cl.duoc.nexo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NEXOTheme
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoFuenteTitulos
import cl.duoc.nexo.ui.theme.NexoMist
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoSky
import kotlin.math.PI
import kotlin.math.sin

enum class VarianteLogo { MARK, VERTICAL, HORIZONTAL }

/**
 * Geometría del candado en un lienzo de 100 × 100 unidades — idéntica a
 * res/drawable/logo_mark.xml y los SVG de design/logo (generados desde el mismo
 * script), para que el logo animado y el estático calcen al píxel.
 */
internal object GeometriaCandado {
    const val CUERPO_IZQ = 20f
    const val CUERPO_DER = 80f
    const val CUERPO_ARRIBA = 44f
    const val CUERPO_ABAJO = 90f
    const val CUERPO_RADIO = 13f
    const val ARCO_GROSOR = 9f
    const val ARCO_X_IZQ = 33f
    const val ARCO_X_DER = 67f
    const val ARCO_Y_BASE = 47f
    const val ARCO_Y_HOMBRO = 31f
    const val ARCO_RADIO = 17f

    /** Cuánto sube el arco cuando el candado está abierto. */
    const val APERTURA = 10f

    val NODO = Offset(67f, 37f)
    const val NODO_RADIO = 4.6f
    const val PISTA_GROSOR = 3f

    /** Extremos de las pistas del logo: aquí nace la red de CircuitField. */
    val VIAS = listOf(Offset(94.5f, 29f), Offset(94.5f, 40f))
    const val VIA_RADIO = 2.6f

    /** Centro visual del candado (sin contar las pistas). */
    val CENTRO = Offset(50f, 52f)

    fun pistas(): List<Path> = listOf(
        Path().apply {
            moveTo(71.5f, 34f); lineTo(80f, 34f); lineTo(85f, 29f); lineTo(92f, 29f)
        },
        Path().apply {
            moveTo(71.5f, 40f); lineTo(92f, 40f)
        }
    )

    fun arco(): Path = Path().apply {
        moveTo(ARCO_X_IZQ, ARCO_Y_BASE)
        lineTo(ARCO_X_IZQ, ARCO_Y_HOMBRO)
        arcTo(
            rect = Rect(ARCO_X_IZQ, ARCO_Y_HOMBRO - ARCO_RADIO, ARCO_X_DER, ARCO_Y_HOMBRO + ARCO_RADIO),
            startAngleDegrees = 180f,
            sweepAngleDegrees = 180f,
            forceMoveTo = false
        )
        lineTo(ARCO_X_DER, ARCO_Y_BASE)
    }

    fun cuerpo(): Path = Path().apply {
        addRoundRect(
            RoundRect(
                left = CUERPO_IZQ, top = CUERPO_ARRIBA, right = CUERPO_DER, bottom = CUERPO_ABAJO,
                cornerRadius = CornerRadius(CUERPO_RADIO, CUERPO_RADIO)
            )
        )
    }

    /** Ojo de cerradura clásico: círculo + ranura que se ensancha. */
    fun ojo(): Path = Path().apply {
        moveTo(50f, 56f)
        arcTo(Rect(44f, 56f, 56f, 68f), -90f, 150f, false)
        lineTo(54f, 76f)
        lineTo(46f, 76f)
        lineTo(47f, 67.2f)
        arcTo(Rect(44f, 56f, 56f, 68f), 120f, 150f, false)
        close()
    }
}

/** Paths del candado ya construidos (una sola vez por composición). */
internal class PathsCandado {
    val arco = GeometriaCandado.arco()
    val cuerpo = GeometriaCandado.cuerpo()
    val ojo = GeometriaCandado.ojo()
    val pistas = GeometriaCandado.pistas()
}

/**
 * Dibuja el candado ocupando el lado menor del DrawScope.
 *
 * @param apertura 0 = cerrado, 1 = arco arriba (abierto). Admite valores
 *   intermedios y un poco negativos (rebote del spring al encajar).
 * @param brilloOjo 0..1, intensidad del glow neón del ojo de cerradura.
 * @param destello 0..1, destello neón al cerrar (PIN correcto / fin de carga).
 * @param reflejo posición 0..1 de la franja de brillo diagonal (o < 0 = sin franja).
 */
internal fun DrawScope.dibujarCandado(
    paths: PathsCandado,
    sobreOscuro: Boolean,
    apertura: Float = 0f,
    brilloOjo: Float = 0.5f,
    destello: Float = 0f,
    reflejo: Float = -1f,
    temblor: Float = 0f
) {
    val unidad = size.minDimension / 100f
    val origenX = (size.width - 100f * unidad) / 2f
    val origenY = (size.height - 100f * unidad) / 2f

    withTransform({
        translate(origenX, origenY)
        scale(unidad, unidad, pivot = Offset.Zero)
    }) {
        // Pistas del logo (detrás de todo)
        paths.pistas.forEach { pista ->
            drawPath(
                pista, NexoSky,
                style = Stroke(GeometriaCandado.PISTA_GROSOR, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        GeometriaCandado.VIAS.forEach { via ->
            drawCircle(NexoSky, GeometriaCandado.VIA_RADIO, via)
        }

        // Arco: se desplaza hacia arriba según la apertura (+ temblor lateral)
        translate(left = temblor, top = -GeometriaCandado.APERTURA * apertura) {
            drawPath(
                paths.arco,
                brush = Brush.verticalGradient(
                    colors = listOf(NexoSky, NexoDeep),
                    startY = 14f,
                    endY = GeometriaCandado.ARCO_Y_BASE
                ),
                style = Stroke(GeometriaCandado.ARCO_GROSOR, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Cuerpo
        drawPath(paths.cuerpo, NexoDeep)
        if (sobreOscuro) {
            drawPath(paths.cuerpo, NexoSky.copy(alpha = 0.6f), style = Stroke(1.6f))
        }

        // Reflejo: franja diagonal de luz que cruza el cuerpo (respaldo sin shader)
        if (reflejo in 0f..1f) {
            clipPath(paths.cuerpo) {
                val x = -40f + reflejo * 160f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, NexoMist.copy(alpha = 0.22f), Color.Transparent),
                        start = Offset(x, 40f),
                        end = Offset(x + 24f, 64f)
                    ),
                    topLeft = Offset(GeometriaCandado.CUERPO_IZQ, GeometriaCandado.CUERPO_ARRIBA),
                    size = Size(60f, 46f)
                )
            }
        }

        // Nodo de circuito en la pata derecha del arco
        drawCircle(NexoSky, GeometriaCandado.NODO_RADIO, GeometriaCandado.NODO)

        // Glow del ojo de cerradura + destello
        val glow = (brilloOjo * 0.55f + destello).coerceIn(0f, 1.2f)
        if (glow > 0.01f) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NexoNeon.copy(alpha = 0.55f * glow.coerceAtMost(1f)), Color.Transparent),
                    center = Offset(50f, 66f),
                    radius = 14f + 18f * destello
                ),
                radius = 14f + 18f * destello,
                center = Offset(50f, 66f)
            )
        }
        drawPath(paths.ojo, NexoNeon)
    }
}

/**
 * Logotipo NEXO (candado + palabra). Un solo componente para todas las
 * variantes. Las animaciones son opcionales y se leen en la fase de dibujo.
 *
 * @param apertura lambda 0..1 (0 = cerrado). Para el gesto de "cierre" o
 *   "no abre" del PIN y del Splash.
 * @param respirar escala 1.00 ↔ 1.035 + glow del ojo, ciclo de 3.2 s.
 */
@Composable
fun NexoLogo(
    modifier: Modifier = Modifier,
    variante: VarianteLogo = VarianteLogo.MARK,
    tamanoMarca: Dp = 48.dp,
    sobreOscuro: Boolean = LocalNexoTemaOscuro.current,
    apertura: () -> Float = { 0f },
    destello: () -> Float = { 0f },
    temblor: () -> Float = { 0f },
    respirar: Boolean = false,
    reflejo: Boolean = false,
    alfaPalabra: (indiceLetra: Int) -> Float = { 1f }
) {
    val animar = rememberAnimacionesActivas()
    val reloj by rememberRelojAnimacion(animar && (respirar || reflejo))
    val paths = androidx.compose.runtime.remember { PathsCandado() }

    val marca: @Composable () -> Unit = {
        Canvas(
            modifier = Modifier
                .size(tamanoMarca)
                .graphicsLayer {
                    if (respirar && animar) {
                        // Seno suave: se siente vivo, no rebota.
                        val s = 1f + 0.0175f * (1f - kotlin.math.cos(2f * PI.toFloat() * reloj / 3.2f))
                        scaleX = s
                        scaleY = s
                    }
                }
        ) {
            val respiro = if (respirar && animar) 0.5f + 0.5f * sin(2f * PI.toFloat() * reloj / 3.2f) else 0.5f
            val franja = if (reflejo && animar) ((reloj % 4f) / 1.2f).let { if (it <= 1f) it else -1f } else -1f
            dibujarCandado(
                paths = paths,
                sobreOscuro = sobreOscuro,
                apertura = apertura(),
                brilloOjo = respiro,
                destello = destello(),
                reflejo = franja,
                temblor = temblor()
            )
        }
    }

    val palabra: @Composable () -> Unit = {
        PalabraNexo(
            tamanoLetra = (tamanoMarca.value * 0.34f).sp,
            sobreOscuro = sobreOscuro,
            alfaLetra = alfaPalabra
        )
    }

    val semantica = Modifier.semantics(mergeDescendants = true) { contentDescription = "Logo de NEXO" }
    when (variante) {
        VarianteLogo.MARK -> Column(modifier.then(semantica)) { marca() }
        VarianteLogo.VERTICAL -> Column(
            modifier = modifier.then(semantica),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            marca()
            Spacer(Modifier.height(tamanoMarca * 0.08f))
            Column(Modifier.clearAndSetSemantics { }) { palabra() }
        }
        VarianteLogo.HORIZONTAL -> Row(
            modifier = modifier.then(semantica),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            marca()
            Spacer(Modifier.width(tamanoMarca * 0.06f))
            Column(Modifier.clearAndSetSemantics { }) { palabra() }
        }
    }
}

/**
 * La palabra "NEXO" del logotipo (Inter Display ExtraBold, tracking +6 %),
 * letra por letra para poder animar su aparición (fade + subida de 6 dp).
 */
@Composable
fun PalabraNexo(
    tamanoLetra: TextUnit,
    sobreOscuro: Boolean = LocalNexoTemaOscuro.current,
    modifier: Modifier = Modifier,
    alfaLetra: (indiceLetra: Int) -> Float = { 1f }
) {
    val estilo = TextStyle(
        fontFamily = NexoFuenteTitulos,
        fontWeight = FontWeight.ExtraBold,
        fontSize = tamanoLetra,
        letterSpacing = 0.06.em,
        color = if (sobreOscuro) NexoMist else NexoDeep
    )
    Row(modifier) {
        "NEXO".forEachIndexed { i, letra ->
            Text(
                text = letra.toString(),
                style = estilo,
                modifier = Modifier.graphicsLayer {
                    val a = alfaLetra(i).coerceIn(0f, 1f)
                    alpha = a
                    translationY = (1f - a) * 6.dp.toPx()
                }
            )
        }
    }
}

@Preview(name = "Logo — claro", showBackground = true, backgroundColor = 0xFFF5FAFF)
@Composable
private fun NexoLogoPreviewClaro() {
    NEXOTheme(darkTheme = false) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NexoLogo(variante = VarianteLogo.VERTICAL, tamanoMarca = 96.dp)
            NexoLogo(variante = VarianteLogo.HORIZONTAL, tamanoMarca = 48.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                NexoLogo(tamanoMarca = 24.dp)
                NexoLogo(tamanoMarca = 48.dp)
            }
        }
    }
}

@Preview(name = "Logo — oscuro", showBackground = true, backgroundColor = 0xFF061A3F)
@Composable
private fun NexoLogoPreviewOscuro() {
    NEXOTheme(darkTheme = true) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            NexoLogo(variante = VarianteLogo.VERTICAL, tamanoMarca = 96.dp, apertura = { 1f })
            NexoLogo(variante = VarianteLogo.HORIZONTAL, tamanoMarca = 48.dp)
        }
    }
}
