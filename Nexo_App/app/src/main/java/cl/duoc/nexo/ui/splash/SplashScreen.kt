package cl.duoc.nexo.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.ui.components.CircuitField
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.ModoCircuito
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.NexoLogo
import cl.duoc.nexo.ui.components.PalabraNexo
import cl.duoc.nexo.ui.components.rememberAnimacionesActivas
import cl.duoc.nexo.ui.theme.IconosClarosEnBarrasDelSistema
import cl.duoc.nexo.ui.theme.NexoNight
import cl.duoc.nexo.ui.theme.NexoSky
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * Tamaño del candado en el Splash. Calza con res/drawable/splash_icon.xml:
 * Android 12+ dibuja ese ícono (sin fondo) en un cuadro de 288 dp y la marca
 * ocupa 48 de sus 108 unidades → 100 u × 288 / 108 × 0.48 = 128 dp. Así el
 * primer frame de Compose empalma con el splash del sistema.
 */
private val TAMANO_LOGO_SPLASH = 128.dp

/**
 * Coreografía (≈ 2.1 s, igual que la carga original de 2 s):
 *
 * | Tiempo       | Qué pasa                                                        |
 * |--------------|-----------------------------------------------------------------|
 * | 0 ms         | Splash del sistema: candado abierto estático sobre NexoNight    |
 * | 0–300 ms     | Aparece el gradiente ALTO; el candado ya está, arco abierto     |
 * | 200–900 ms   | CircuitField INTRO: las pistas se trazan hacia los bordes       |
 * | 600–950 ms   | "NEXO" aparece letra por letra (fade + 6 dp, 50 ms entre letras)|
 * | 900 ms → fin | LOOP: el logo respira, pulsos por los circuitos, reflejo        |
 * | fin de carga | Pulsos convergen (350 ms) → el candado se cierra (spring +      |
 * |              | destello + háptico) → onda de luz Sky que revela lo siguiente   |
 *
 * Si la consulta a Room tarda más, se queda en el LOOP hasta que termine.
 * Con "Quitar animaciones": fotograma estático (candado cerrado, circuitos
 * encendidos) y paso a la siguiente pantalla con el fade de navegación.
 */
@Composable
fun SplashScreen(onDecidido: (existeConfiguracion: Boolean) -> Unit) {
    val context = LocalContext.current
    val haptico = LocalHapticFeedback.current
    val animar = rememberAnimacionesActivas()
    IconosClarosEnBarrasDelSistema()

    val fondo = remember { Animatable(if (animar) 0f else 1f) }
    val intro = remember { Animatable(if (animar) 0f else 1f) }
    val letras = remember { Animatable(if (animar) 0f else 1f) }
    val convergencia = remember { Animatable(0f) }
    val apertura = remember { Animatable(if (animar) 1f else 0f) }
    val destello = remember { Animatable(0f) }
    val onda = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        val inicio = System.currentTimeMillis()
        val consulta = async { NexoDatabase.getInstance(context).parentSettingsDao().obtener() }

        if (!animar) {
            val configuracion = consulta.await()
            delay((2000L - (System.currentTimeMillis() - inicio)).coerceAtLeast(0L))
            onDecidido(configuracion != null)
            return@LaunchedEffect
        }

        launch { fondo.animateTo(1f, tween(300)) }
        launch {
            delay(200)
            intro.animateTo(1f, tween(700, easing = LinearEasing))
        }
        launch {
            delay(600)
            letras.animateTo(1f, tween(350, easing = LinearEasing))
        }

        val configuracion = consulta.await()
        delay((1300L - (System.currentTimeMillis() - inicio)).coerceAtLeast(0L))

        // Fin de carga: convergen los pulsos, cierra el candado y sale la onda.
        convergencia.animateTo(1f, tween(350, easing = FastOutLinearInEasing))
        launch {
            apertura.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f))
        }
        delay(110)
        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
        launch {
            destello.animateTo(1f, tween(110))
            destello.animateTo(0f, tween(420))
        }
        onda.animateTo(1f, tween(380))
        onDecidido(configuracion != null)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NexoNight)
            .drawWithContent {
                drawContent()
                // Onda de luz que se expande desde el logo
                val o = onda.value
                if (o > 0f) {
                    val radioMax = hypot(size.width, size.height) / 2f
                    val radio = radioMax * o
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color.Transparent, NexoSky.copy(alpha = 0.35f * (1f - o)), Color.Transparent),
                            center = center,
                            radius = radio.coerceAtLeast(1f)
                        ),
                        radius = radio,
                        center = center
                    )
                    drawCircle(
                        color = NexoSky.copy(alpha = 0.8f * (1f - o)),
                        radius = radio,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                    drawRect(NexoNight.copy(alpha = 0.55f * o))
                }
            }
    ) {
        NexoGradientBackground(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = fondo.value },
            intensidad = IntensidadGradiente.ALTA
        )

        CircuitField(
            modifier = Modifier.fillMaxSize(),
            modo = ModoCircuito.INTRO,
            sobreOscuro = true,
            origen = Offset(0.5f, 0.5f),
            radioOrigen = 0.19f,
            densidad = 14,
            progresoIntro = { intro.value },
            convergencia = { convergencia.value }
        )

        NexoLogo(
            modifier = Modifier.align(Alignment.Center),
            tamanoMarca = TAMANO_LOGO_SPLASH,
            sobreOscuro = true,
            apertura = { apertura.value },
            destello = { destello.value },
            respirar = animar,
            reflejo = animar
        )

        PalabraNexo(
            tamanoLetra = 34.sp,
            sobreOscuro = true,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = TAMANO_LOGO_SPLASH / 2 + 24.dp),
            alfaLetra = { i -> (letras.value * 7f - i).coerceIn(0f, 1f) }
        )
    }
}
