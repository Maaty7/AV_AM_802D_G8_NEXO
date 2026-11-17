package cl.duoc.nexo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoSky
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
import kotlin.random.Random

enum class ModoCircuito {
    /** Las pistas se trazan desde el logo hacia los bordes (Splash). */
    INTRO,

    /** Pulsos de luz continuos viajando por las pistas. */
    LOOP,

    /** Fondo casi estático (PIN): pistas tenues y 2 pulsos lentos. */
    TENUE
}

/** Una pista de la placa: path ya construido + su medidor + datos de animación. */
private class Pista(
    val path: Path,
    val medidor: PathMeasure,
    val largo: Float,
    val fin: Offset,
    val retraso: Float,      // 0..1 dentro de la intro (cascada)
    val esRama: Boolean
)

private class Red(val pistas: List<Pista>)

private const val SEMILLA = 2026

/**
 * Genera la red PCB de forma procedural con semilla fija (siempre igual
 * para un mismo tamaño): pistas ortogonales con quiebres a 45° que nacen en
 * un anillo alrededor del logo y se ramifican hacia los bordes.
 */
private fun generarRed(size: Size, origen: Offset, radioInicial: Float, densidad: Int, paso: Float): Red {
    val rnd = Random(SEMILLA)
    val pistas = mutableListOf<Pista>()
    val n = densidad.coerceIn(4, 16)
    val margen = paso * 0.5f

    fun crear(path: Path, fin: Offset, retraso: Float, rama: Boolean) {
        val medidor = PathMeasure()
        medidor.setPath(path, false)
        pistas += Pista(path, medidor, medidor.length, fin, retraso, rama)
    }

    for (i in 0 until n) {
        // Ángulos repartidos con un poco de ruido: simétrico pero no idéntico.
        val angulo = (i.toFloat() / n) * 2f * Math.PI.toFloat() + (rnd.nextFloat() - 0.5f) * 0.35f
        val dx = cos(angulo)
        val dy = sin(angulo)
        val horizontal = abs(dx) >= abs(dy)
        val sx = sign(dx).let { if (it == 0f) 1f else it }
        val sy = sign(dy).let { if (it == 0f) 1f else it }

        var p = Offset(origen.x + dx * radioInicial, origen.y + dy * radioInicial)
        val path = Path().apply { moveTo(p.x, p.y) }

        // Tramo recto inicial en el eje dominante
        val a = paso * (1.2f + rnd.nextFloat() * 1.6f)
        p = if (horizontal) Offset(p.x + sx * a, p.y) else Offset(p.x, p.y + sy * a)
        path.lineTo(p.x, p.y)

        // Quiebre a 45°
        val b = paso * (0.6f + rnd.nextFloat() * 1.2f)
        p = Offset(p.x + sx * b, p.y + sy * b)
        path.lineTo(p.x, p.y)
        val quiebre = p

        // Tramo final hacia el borde (sin salirse)
        val c = paso * (2f + rnd.nextFloat() * 4f)
        p = if (horizontal) Offset(p.x + sx * c, p.y) else Offset(p.x, p.y + sy * c)
        p = Offset(p.x.coerceIn(margen, size.width - margen), p.y.coerceIn(margen, size.height - margen))
        path.lineTo(p.x, p.y)
        crear(path, p, retraso = i.toFloat() / n * 0.45f, rama = false)

        // Rama corta perpendicular desde el quiebre (en ~60 % de las pistas)
        if (rnd.nextFloat() < 0.6f) {
            val largoRama = paso * (0.8f + rnd.nextFloat() * 1.2f)
            val signo = if (rnd.nextBoolean()) 1f else -1f
            val q1 = if (horizontal) Offset(quiebre.x, quiebre.y + signo * largoRama) else Offset(quiebre.x + signo * largoRama, quiebre.y)
            val q2 = if (horizontal) Offset(q1.x + sx * largoRama * 0.7f, q1.y + signo * largoRama * 0.7f)
            else Offset(q1.x + signo * largoRama * 0.7f, q1.y + sy * largoRama * 0.7f)
            val fin = Offset(q2.x.coerceIn(margen, size.width - margen), q2.y.coerceIn(margen, size.height - margen))
            val rama = Path().apply {
                moveTo(quiebre.x, quiebre.y)
                lineTo(q1.x, q1.y)
                lineTo(fin.x, fin.y)
            }
            crear(rama, fin, retraso = min(0.9f, i.toFloat() / n * 0.45f + 0.25f), rama = true)
        }
    }
    return Red(pistas)
}

/**
 * Red de circuitos brillantes (estilo placa PCB) que sale del logo.
 * Todo se dibuja en Canvas (no usa un segundo shader) y la red se cachea:
 * solo se recalcula si cambia el tamaño. El tiempo se lee en la fase de
 * dibujo, así no hay recomposición por frame.
 *
 * @param origen centro del logo, relativo al tamaño (0..1).
 * @param radioOrigen distancia desde el centro donde nacen las pistas.
 * @param progresoIntro 0..1 para el modo INTRO.
 * @param convergencia 0..1: todos los pulsos vuelven hacia el logo (fin del Splash).
 */
@Composable
fun CircuitField(
    modifier: Modifier = Modifier,
    modo: ModoCircuito = ModoCircuito.LOOP,
    sobreOscuro: Boolean = true,
    origen: Offset = Offset(0.5f, 0.5f),
    radioOrigen: Float = 0.12f,
    densidad: Int = 12,
    velocidad: Float = 1f,
    brillo: Float = 1f,
    progresoIntro: () -> Float = { 1f },
    convergencia: () -> Float = { 0f }
) {
    val animar = rememberAnimacionesActivas()
    val reloj by rememberRelojAnimacion(animar)

    val colorBase = if (sobreOscuro) NexoSky.copy(alpha = if (modo == ModoCircuito.TENUE) 0.14f else 0.22f)
    else NexoDeep.copy(alpha = 0.12f)
    val colorTrazo = if (sobreOscuro) NexoSky else NexoDeep

    Box(
        modifier.drawWithCache {
            val minLado = size.minDimension
            val paso = (minLado / 14f).coerceAtLeast(8f)
            val centro = Offset(size.width * origen.x, size.height * origen.y)
            val red = generarRed(size, centro, minLado * radioOrigen, densidad, paso)
            val grosor = 1.8.dp.toPx()
            val trazo = Stroke(grosor, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val trazoGlow = Stroke(grosor * 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val segmento = Path()
            val radioVia = grosor * 1.6f

            onDrawBehind {
                val t = if (animar) reloj * velocidad else 0f
                val intro = if (modo == ModoCircuito.INTRO) progresoIntro().coerceIn(0f, 1f) else 1f
                val conv = convergencia().coerceIn(0f, 1f)

                red.pistas.forEachIndexed { i, pista ->
                    // Progreso local de esta pista dentro de la intro (cascada)
                    val local = ((intro - pista.retraso) / 0.45f).coerceIn(0f, 1f)
                    if (local <= 0f) return@forEachIndexed

                    // 1) Pista base
                    if (local >= 1f) {
                        drawPath(pista.path, colorBase, style = trazo)
                    } else {
                        segmento.reset()
                        pista.medidor.getSegment(0f, pista.largo * local, segmento, true)
                        drawPath(segmento, colorTrazo.copy(alpha = 0.9f * brillo), style = trazo)
                        drawPath(segmento, colorTrazo.copy(alpha = 0.18f * brillo), style = trazoGlow)
                    }

                    // 2) Vía en el extremo: se enciende al llegar el trazo
                    val encendidoIntro = if (modo == ModoCircuito.INTRO && local >= 1f) {
                        exp(-((intro - pista.retraso - 0.45f).coerceAtLeast(0f)) * 6f)
                    } else 0f

                    // 3) Pulsos
                    var encendidoPulso = 0f
                    if (modo != ModoCircuito.INTRO || intro >= 1f) {
                        val cantidad = when (modo) {
                            ModoCircuito.TENUE -> if (i % 6 == 0) 1 else 0
                            else -> if (pista.esRama) 0 else 1
                        }
                        if (cantidad > 0 && animar) {
                            val vel = if (modo == ModoCircuito.TENUE) 0.12f else 0.32f
                            val fase = ((t * vel + i * 0.37f) % 1f)
                            val haciaAfuera = i % 4 != 3
                            val cabeza = if (conv > 0f) 1f - conv else if (haciaAfuera) fase else 1f - fase
                            val esNeon = i % 3 == 0
                            dibujarPulso(
                                pista, segmento, cabeza,
                                color = if (esNeon) NexoNeon else NexoSky,
                                alfa = (if (modo == ModoCircuito.TENUE) 0.5f else 1f) * brillo,
                                trazo = trazo, trazoGlow = trazoGlow
                            )
                            // El nodo destella al pasar el pulso y se apaga lento.
                            encendidoPulso = if (haciaAfuera && conv == 0f) {
                                if (fase > 0.9f) (fase - 0.9f) * 10f else max(0f, 1f - fase * 4f)
                            } else 0f
                        }
                    }

                    val encendido = max(encendidoIntro, encendidoPulso)
                    val escala = 1f + 0.4f * encendido
                    if (encendido > 0.02f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(NexoSky.copy(alpha = 0.45f * encendido * brillo), Color.Transparent),
                                center = pista.fin,
                                radius = radioVia * 5f
                            ),
                            radius = radioVia * 5f,
                            center = pista.fin
                        )
                    }
                    drawCircle(
                        color = if (encendido > 0.02f) NexoSky else colorBase,
                        radius = radioVia * escala,
                        center = pista.fin
                    )
                }
            }
        }
    )
}

/** Pulso tipo cometa: cabeza brillante + cola con degradado (3 tramos) + glow. */
private fun DrawScope.dibujarPulso(
    pista: Pista,
    segmento: Path,
    cabeza: Float,
    color: Color,
    alfa: Float,
    trazo: Stroke,
    trazoGlow: Stroke
) {
    val largoCola = min(pista.largo * 0.35f, 36.dp.toPx())
    val fin = pista.largo * cabeza.coerceIn(0f, 1f)
    val tramos = 3
    for (k in 0 until tramos) {
        val desde = (fin - largoCola * (k + 1) / tramos).coerceAtLeast(0f)
        val hasta = (fin - largoCola * k / tramos).coerceAtLeast(0f)
        if (hasta - desde < 0.5f) continue
        segmento.reset()
        pista.medidor.getSegment(desde, hasta, segmento, true)
        val a = alfa * (1f - k / tramos.toFloat())
        drawPath(segmento, color.copy(alpha = 0.22f * a), style = trazoGlow)
        drawPath(segmento, color.copy(alpha = a), style = trazo)
    }
}

/** Indicador de carga de NEXO: logo respirando + 6 pistas cortas con pulsos. Reemplaza al CircularProgressIndicator. */
@Composable
fun NexoCargando(
    modifier: Modifier = Modifier,
    sobreOscuro: Boolean = cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro.current
) {
    Box(modifier.size(120.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        CircuitField(
            modifier = Modifier.fillMaxSize(),
            modo = ModoCircuito.LOOP,
            sobreOscuro = sobreOscuro,
            radioOrigen = 0.22f,
            densidad = 6,
            velocidad = 1.6f
        )
        NexoLogo(tamanoMarca = 40.dp, respirar = true, sobreOscuro = sobreOscuro)
    }
}

@Preview(name = "CircuitField — oscuro", widthDp = 360, heightDp = 640, showBackground = true, backgroundColor = 0xFF061A3F)
@Composable
private fun CircuitFieldPreview() {
    Box(Modifier.fillMaxSize()) {
        CircuitField(Modifier.fillMaxSize(), modo = ModoCircuito.LOOP)
        NexoLogo(Modifier.align(androidx.compose.ui.Alignment.Center), tamanoMarca = 107.dp, sobreOscuro = true)
    }
}

@Preview(name = "Cargando — claro", showBackground = true, backgroundColor = 0xFFF5FAFF)
@Composable
private fun NexoCargandoPreview() {
    NexoCargando(sobreOscuro = false)
}
