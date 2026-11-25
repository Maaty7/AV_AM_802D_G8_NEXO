package cl.duoc.nexo.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NEXOTheme
import cl.duoc.nexo.ui.theme.NexoAbyss
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMist
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoNight
import cl.duoc.nexo.ui.theme.NexoSky
import cl.duoc.nexo.ui.theme.NexoSkySoft
import kotlin.math.cos
import kotlin.math.sin

/**
 * ALTA: Splash y PIN (pantalla completa, Night → Deep → Sky).
 * MEDIA: onboarding.
 * BAJA: dashboards (solo arriba, se desvanece al color de fondo).
 */
enum class IntensidadGradiente { ALTA, MEDIA, BAJA }

/**
 * Malla de gradiente animada (estilo shadergradient) hecha con Canvas:
 * 3–4 manchas radiales que se desplazan lento + una veta neón muy fina.
 * Funciona igual en API 26+ (el shader AGSL queda como trabajo futuro).
 * El reloj se lee solo en la fase de dibujo.
 */
@Composable
fun NexoGradientBackground(
    modifier: Modifier = Modifier,
    intensidad: IntensidadGradiente = IntensidadGradiente.BAJA,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val animar = rememberAnimacionesActivas()
    val reloj by rememberRelojAnimacion(animar)
    val oscuro = LocalNexoTemaOscuro.current
    val fondo = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier.drawBehind {
            val t = if (animar) reloj else 0f
            when (intensidad) {
                IntensidadGradiente.ALTA -> {
                    drawRect(
                        Brush.verticalGradient(listOf(NexoAbyss, NexoNight, NexoDeep))
                    )
                    manchas(t, sky = 0.42f, deep = 0.9f, mist = 0.10f)
                    vetaNeon(t, alfa = 0.22f)
                }
                IntensidadGradiente.MEDIA -> {
                    drawRect(fondo)
                    if (oscuro) manchas(t, sky = 0.30f, deep = 0.8f, mist = 0.05f)
                    else manchas(t, sky = 0.34f, deep = 0.18f, mist = 0f, suave = true)
                    vetaNeon(t, alfa = if (oscuro) 0.16f else 0.0f)
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to fondo,
                            startY = size.height * 0.35f,
                            endY = size.height * 0.75f
                        )
                    )
                }
                IntensidadGradiente.BAJA -> {
                    drawRect(fondo)
                    if (oscuro) manchas(t, sky = 0.26f, deep = 0.8f, mist = 0f)
                    else manchas(t, sky = 0.30f, deep = 0.14f, mist = 0f, suave = true)
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to fondo,
                            startY = size.height * 0.12f,
                            endY = size.height * 0.42f
                        )
                    )
                }
            }
        },
        content = content
    )
}

private fun DrawScope.mancha(centro: Offset, radio: Float, color: Color) {
    drawCircle(
        brush = Brush.radialGradient(listOf(color, Color.Transparent), center = centro, radius = radio),
        radius = radio,
        center = centro
    )
}

/** Manchas de luz que se desplazan lento (ciclos de 20–40 s): la "tela líquida". */
private fun DrawScope.manchas(t: Float, sky: Float, deep: Float, mist: Float, suave: Boolean = false) {
    val w = size.width
    val h = size.height
    val r = maxOf(w, h)
    mancha(
        Offset(w * (0.18f + 0.10f * sin(t * 0.21f)), h * (0.12f + 0.06f * cos(t * 0.17f))),
        r * 0.62f,
        (if (suave) NexoSkySoft else NexoSky).copy(alpha = sky)
    )
    mancha(
        Offset(w * (0.88f + 0.08f * cos(t * 0.15f)), h * (0.28f + 0.08f * sin(t * 0.13f))),
        r * 0.55f,
        NexoDeep.copy(alpha = deep)
    )
    mancha(
        Offset(w * (0.55f + 0.18f * sin(t * 0.11f + 1.3f)), h * (0.62f + 0.10f * cos(t * 0.09f))),
        r * 0.50f,
        NexoSky.copy(alpha = sky * 0.7f)
    )
    if (mist > 0f) {
        mancha(
            Offset(w * (0.30f + 0.12f * cos(t * 0.19f)), h * (0.86f + 0.04f * sin(t * 0.23f))),
            r * 0.35f,
            NexoMist.copy(alpha = mist)
        )
    }
}

/** Veta neón muy fina (≤ 5 % del área) que ondula como un reflejo. */
private fun DrawScope.vetaNeon(t: Float, alfa: Float) {
    if (alfa <= 0f) return
    val w = size.width
    val h = size.height
    val y0 = h * (0.70f + 0.03f * sin(t * 0.25f))
    val path = Path().apply {
        moveTo(-w * 0.1f, y0)
        cubicTo(
            w * 0.30f, y0 - h * (0.10f + 0.04f * sin(t * 0.31f)),
            w * 0.62f, y0 + h * (0.06f + 0.03f * cos(t * 0.27f)),
            w * 1.1f, y0 - h * 0.12f
        )
    }
    drawPath(path, NexoNeon.copy(alpha = alfa * 0.35f), style = Stroke(width = 10.dp.toPx()))
    drawPath(path, NexoNeon.copy(alpha = alfa), style = Stroke(width = 1.2.dp.toPx()))
}

/**
 * Tarjeta de vidrio para contenido grande. Tinte translúcido + borde fino +
 * sombra suave teñida de Deep, esquinas de 24 dp. Siempre va sobre un
 * NexoGradientBackground/CircuitField para que se note la transparencia.
 *
 * Nota: el desenfoque real del fondo (backdrop blur / lens) requiere la
 * librería io.github.kyant0:backdrop y queda como trabajo futuro; aquí se
 * usa la variante "sin blur" de la guía (tinte alto), que funciona igual en
 * API 26–36 y mantiene contraste WCAG AA.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    sobreOscuro: Boolean = LocalNexoTemaOscuro.current,
    radio: Dp = 24.dp,
    relleno: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val forma = RoundedCornerShape(radio)
    val tinte = if (sobreOscuro) NexoNight.copy(alpha = 0.72f) else NexoMist.copy(alpha = 0.84f)
    val borde = if (sobreOscuro) NexoSky.copy(alpha = 0.25f) else NexoMist.copy(alpha = 0.9f)
    val brillo = if (sobreOscuro) NexoSky.copy(alpha = 0.10f) else NexoMist.copy(alpha = 0.6f)
    Column(
        modifier = modifier
            .shadow(
                elevation = if (sobreOscuro) 0.dp else 12.dp,
                shape = forma,
                ambientColor = NexoDeep.copy(alpha = 0.18f),
                spotColor = NexoDeep.copy(alpha = 0.22f)
            )
            .clip(forma)
            .drawBehind {
                drawRect(tinte)
                // Reflejo superior sutil (luz que entra por arriba del vidrio)
                drawRect(
                    Brush.verticalGradient(
                        listOf(brillo, Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
            }
            .border(1.dp, borde, forma)
            .padding(relleno),
        content = content
    )
}

@Preview(name = "Gradiente + GlassCard — claro", widthDp = 360, heightDp = 400)
@Composable
private fun GlassCardPreviewClaro() {
    NEXOTheme(darkTheme = false) {
        NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.BAJA) {
            GlassCard(Modifier.padding(20.dp)) { Text("Jornada escolar", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Preview(name = "Gradiente + GlassCard — oscuro", widthDp = 360, heightDp = 400)
@Composable
private fun GlassCardPreviewOscuro() {
    NEXOTheme(darkTheme = true) {
        NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.ALTA) {
            GlassCard(Modifier.padding(20.dp), sobreOscuro = true) {
                Text("Jornada escolar", style = MaterialTheme.typography.titleLarge, color = NexoMist)
            }
        }
    }
}
