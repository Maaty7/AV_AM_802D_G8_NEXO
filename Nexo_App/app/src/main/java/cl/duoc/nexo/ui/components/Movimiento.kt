package cl.duoc.nexo.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay

/** Curva de salida fuerte para entradas de UI (Guía B: emil-design-eng). */
val NexoEaseOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)

/** Curva para movimiento en pantalla (morph, respiración). */
val NexoEaseInOut = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)

/**
 * false si el usuario activó "Quitar animaciones" (escala de duración del
 * animador = 0). En ese caso todas las animaciones en bucle se apagan y se
 * muestran fotogramas estáticos.
 */
@Composable
fun rememberAnimacionesActivas(): Boolean {
    if (LocalInspectionMode.current) return true
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) != 0f
    }
}

/**
 * Reloj en segundos para animaciones infinitas (fondos, circuitos, logo).
 * Solo avanza mientras la pantalla está en RESUMED: se pausa sola cuando la
 * app pasa a segundo plano. Leer `.value` SOLO en la fase de dibujo
 * (drawBehind / drawWithCache / graphicsLayer) para no recomponer por frame.
 */
@Composable
fun rememberRelojAnimacion(activo: Boolean = true): State<Float> {
    val tiempo = remember { mutableFloatStateOf(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(activo, lifecycleOwner) {
        if (!activo) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var ultimo = -1L
            while (true) {
                withFrameNanos { ahora ->
                    if (ultimo >= 0L) {
                        tiempo.floatValue += (ahora - ultimo) / 1_000_000_000f
                    }
                    ultimo = ahora
                }
            }
        }
    }
    return tiempo
}

/**
 * Entrada de tarjetas en cascada: fade + subida de 12 dp, 70 ms entre cada
 * una (máx. 5 escalones, para no hacer esperar). Se lee en graphicsLayer.
 */
@Composable
fun Modifier.entradaEnCascada(indice: Int): Modifier {
    val animar = rememberAnimacionesActivas()
    val progreso = remember { Animatable(if (animar) 0f else 1f) }
    val desplazamiento = with(LocalDensity.current) { 12.dp.toPx() }
    LaunchedEffect(Unit) {
        if (!animar) return@LaunchedEffect
        delay(indice.coerceAtMost(5) * 70L)
        progreso.animateTo(1f, tween(durationMillis = 420, easing = NexoEaseOut))
    }
    return this.graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * desplazamiento
    }
}
