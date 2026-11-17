package cl.duoc.nexo.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.components.rememberAnimacionesActivas
import cl.duoc.nexo.viewmodel.PinViewModel
import kotlinx.coroutines.delay

/**
 * Overlay de pantalla completa que exige el PIN antes de dejar pasar una
 * acción sensible (crear/editar/eliminar jornada, cambiar correo, activar
 * o desactivar una app supervisada). Reutiliza el mismo [PinPad] del resto
 * de la app y el mismo mecanismo de verificación que ya usa el Splash al
 * abrir NEXO — solo confirma, nunca pide un PIN nuevo.
 */
@Composable
fun VerificarPinDialog(
    onVerificado: () -> Unit,
    onCancelar: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    val animar = rememberAnimacionesActivas()
    var desbloqueado by remember { mutableStateOf(false) }
    val alTerminar by rememberUpdatedState(onVerificado)

    LaunchedEffect(Unit) {
        viewModel.iniciarVerificacionAcceso()
    }
    LaunchedEffect(desbloqueado) {
        if (desbloqueado) {
            if (animar) delay(MS_ANIMACION_DESBLOQUEO)
            alTerminar()
        }
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        PinPad(
            titulo = viewModel.titulo,
            subtitulo = viewModel.subtitulo,
            buffer = viewModel.buffer,
            error = viewModel.error,
            desbloqueado = desbloqueado,
            onDigit = { digit ->
                viewModel.onDigitPressVerificarAcceso(digit = digit, onCorrecto = { desbloqueado = true })
            },
            onBackspace = { viewModel.onBackspace() }
        )
    }
}
