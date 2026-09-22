package cl.duoc.nexo.ui.security

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoDotOutline
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.ui.theme.NexoKeypadBorder
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.PinViewModel

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

/** Teclado numérico de 4 dígitos + puntos de progreso, compartido entre crear/confirmar PIN (onboarding) y cambiar PIN. */
@Composable
internal fun PinPad(
    titulo: String,
    subtitulo: String,
    buffer: String,
    error: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(NexoIce, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = NexoDeep,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = titulo,
                fontFamily = FontFamily.Serif,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitulo,
                color = NexoMute,
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { index ->
                    val filled = index < buffer.length
                    Box(
                        modifier = Modifier
                            .size(15.dp)
                            .background(
                                color = if (filled) NexoDeep else Color.Transparent,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.6.dp,
                                color = if (filled) NexoDeep else NexoDotOutline,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = error ?: "",
                color = NexoCoral,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            val teclas = listOf(
                "1", "2", "3",
                "4", "5", "6",
                "7", "8", "9",
                "", "0", "⌫"
            )

            Column(
                modifier = Modifier.width(220.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                teclas.chunked(3).forEach { fila ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        fila.forEach { tecla ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(CircleShape)
                                    .then(
                                        if (tecla.isNotEmpty() && tecla != "⌫") {
                                            Modifier.border(1.5.dp, NexoKeypadBorder, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable(enabled = tecla.isNotEmpty()) {
                                        when (tecla) {
                                            "⌫" -> onBackspace()
                                            else -> onDigit(tecla)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (tecla.isNotEmpty()) {
                                    Text(
                                        text = tecla,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (tecla == "⌫") NexoCoral else MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
