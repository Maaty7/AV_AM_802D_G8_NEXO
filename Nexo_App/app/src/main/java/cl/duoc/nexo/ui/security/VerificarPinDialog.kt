package cl.duoc.nexo.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.viewmodel.PinViewModel

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
    LaunchedEffect(Unit) {
        viewModel.iniciarVerificacionAcceso()
    }

    Dialog(
        onDismissRequest = onCancelar,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PinPad(
            titulo = viewModel.titulo,
            subtitulo = viewModel.subtitulo,
            buffer = viewModel.buffer,
            error = viewModel.error,
            onDigit = { digit ->
                viewModel.onDigitPressVerificarAcceso(digit = digit, onCorrecto = onVerificado)
            },
            onBackspace = { viewModel.onBackspace() }
        )
    }
}
