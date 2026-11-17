package cl.duoc.nexo.ui.security

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.components.CircuitField
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.MensajeError
import cl.duoc.nexo.ui.components.ModoCircuito
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.NexoLogo
import cl.duoc.nexo.ui.components.rememberAnimacionesActivas
import cl.duoc.nexo.ui.theme.IconosClarosEnBarrasDelSistema
import cl.duoc.nexo.ui.theme.NexoMist
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoSky
import cl.duoc.nexo.viewmodel.PinViewModel
import kotlinx.coroutines.launch

@Composable
fun CrearPinScreen(
    nombre: String,
    correo: String,
    onPinCreado: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    PinPad(
        titulo = viewModel.titulo,
        subtitulo = viewModel.subtitulo,
        buffer = viewModel.buffer,
        error = viewModel.error,
        onDigit = { digit ->
            viewModel.onDigitPress(
                digit = digit,
                nombre = nombre,
                correo = correo,
                onGuardado = onPinCreado
            )
        },
        onBackspace = { viewModel.onBackspace() }
    )
}

/**
 * Teclado numérico de 4 dígitos + puntos de progreso, compartido entre
 * crear/confirmar PIN (onboarding), verificar PIN y cambiar PIN.
 *
 * Fondo: gradiente ALTO + circuitos TENUES. Candado pequeño respirando con
 * el arco abierto.
 * - PIN incorrecto (error != null): los puntos tiemblan y el arco hace el
 *   gesto de "no abre" (sube un poco, tiembla y vuelve) + vibración.
 * - PIN correcto (desbloqueado = true): el candado se cierra con un "clic"
 *   (spring + destello neón + vibración de confirmación).
 *
 * @param aplicarInsets false cuando el PinPad vive dentro de un Scaffold que
 *   ya agrega el padding de las barras del sistema (Cambiar PIN).
 */
@Composable
internal fun PinPad(
    titulo: String,
    subtitulo: String,
    buffer: String,
    error: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    desbloqueado: Boolean = false,
    aplicarInsets: Boolean = true
) {
    val animar = rememberAnimacionesActivas()
    val haptico = LocalHapticFeedback.current
    IconosClarosEnBarrasDelSistema()

    val apertura = remember { Animatable(1f) }
    val temblorArco = remember { Animatable(0f) }
    val temblorPuntos = remember { Animatable(0f) }
    val destello = remember { Animatable(0f) }

    LaunchedEffect(error) {
        if (error == null) return@LaunchedEffect
        haptico.performHapticFeedback(HapticFeedbackType.Reject)
        if (!animar) return@LaunchedEffect
        launch {
            for (x in listOf(-12f, 12f, -9f, 9f, -4f, 0f)) {
                temblorPuntos.animateTo(x, tween(45))
            }
        }
        launch {
            apertura.animateTo(1.3f, tween(90))
            for (x in listOf(-2.5f, 2.5f, -2f, 2f, 0f)) {
                temblorArco.animateTo(x, tween(40))
            }
            apertura.animateTo(1f, spring(dampingRatio = 0.6f))
        }
    }

    LaunchedEffect(desbloqueado) {
        if (!desbloqueado) return@LaunchedEffect
        haptico.performHapticFeedback(HapticFeedbackType.Confirm)
        if (!animar) {
            apertura.snapTo(0f)
            return@LaunchedEffect
        }
        launch {
            destello.animateTo(1f, tween(110))
            destello.animateTo(0f, tween(400))
        }
        apertura.animateTo(0f, spring(dampingRatio = 0.55f, stiffness = 900f))
    }

    NexoGradientBackground(
        modifier = Modifier.fillMaxSize(),
        intensidad = IntensidadGradiente.ALTA
    ) {
        CircuitField(
            modifier = Modifier.fillMaxSize(),
            modo = ModoCircuito.TENUE,
            sobreOscuro = true,
            origen = Offset(0.5f, 0.16f),
            radioOrigen = 0.12f,
            densidad = 10,
            brillo = if (desbloqueado) 1f else 0.7f
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(if (aplicarInsets) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NexoLogo(
                tamanoMarca = 72.dp,
                sobreOscuro = true,
                respirar = !desbloqueado,
                apertura = { apertura.value },
                destello = { destello.value },
                temblor = { temblorArco.value }
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                color = NexoMist,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = NexoMist.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            GlassCard(
                modifier = Modifier.widthIn(max = 360.dp).fillMaxWidth(),
                sobreOscuro = true,
                relleno = 24.dp
            ) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .graphicsLayer { translationX = temblorPuntos.value * density }
                        .semantics { contentDescription = "${buffer.length} de 4 dígitos ingresados" },
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    repeat(4) { index -> PuntoPin(lleno = index < buffer.length || desbloqueado) }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 32.dp)
                        .padding(top = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (error != null) {
                        MensajeError(texto = error, color = NexoNeon)
                    }
                }

                Spacer(Modifier.height(8.dp))

                val teclas = listOf(
                    "1", "2", "3",
                    "4", "5", "6",
                    "7", "8", "9",
                    "", "0", "⌫"
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    teclas.chunked(3).forEach { fila ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            fila.forEach { tecla ->
                                TeclaPin(
                                    tecla = tecla,
                                    habilitada = !desbloqueado,
                                    onClick = {
                                        when (tecla) {
                                            "⌫" -> onBackspace()
                                            else -> onDigit(tecla)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Punto de progreso: se enciende en neón (con un pequeño "pop") al ingresar cada dígito. */
@Composable
private fun PuntoPin(lleno: Boolean) {
    val color by animateColorAsState(
        targetValue = if (lleno) NexoNeon else NexoNeon.copy(alpha = 0f),
        animationSpec = tween(160),
        label = "colorPunto"
    )
    val escala by animateFloatAsState(
        targetValue = if (lleno) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaPunto"
    )
    Box(
        modifier = Modifier
            .size(16.dp)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .background(color, CircleShape)
            .border(
                width = 1.5.dp,
                color = if (lleno) NexoNeon else NexoMist.copy(alpha = 0.45f),
                shape = CircleShape
            )
    )
}

/** Tecla circular de vidrio (≥ 64 dp). Escala 0.92 al presionar. */
@Composable
private fun TeclaPin(tecla: String, habilitada: Boolean, onClick: () -> Unit) {
    val tamano = 68.dp
    if (tecla.isEmpty()) {
        Spacer(Modifier.size(tamano))
        return
    }
    val interaccion = remember { MutableInteractionSource() }
    val presionada by interaccion.collectIsPressedAsState()
    val escala by animateFloatAsState(
        targetValue = if (presionada) 0.92f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "escalaTecla"
    )
    val esBorrar = tecla == "⌫"
    Box(
        modifier = Modifier
            .size(tamano)
            .graphicsLayer {
                scaleX = escala
                scaleY = escala
            }
            .clip(CircleShape)
            .background(if (presionada) NexoSky.copy(alpha = 0.22f) else NexoMist.copy(alpha = if (esBorrar) 0f else 0.06f))
            .border(1.dp, if (esBorrar) NexoMist.copy(alpha = 0f) else NexoSky.copy(alpha = 0.25f), CircleShape)
            .clickable(
                interactionSource = interaccion,
                indication = null,
                enabled = habilitada,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (esBorrar) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Borrar último dígito",
                tint = NexoMist,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(
                text = tecla,
                style = MaterialTheme.typography.headlineSmall,
                color = NexoMist
            )
        }
    }
}
