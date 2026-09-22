package cl.duoc.nexo.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.viewmodel.PinViewModel

@Composable
fun CambiarPinScreen(
    onPinCambiado: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.iniciarCambioPin()
    }

    PinPad(
        titulo = viewModel.titulo,
        subtitulo = viewModel.subtitulo,
        buffer = viewModel.buffer,
        error = viewModel.error,
        onDigit = { digit -> viewModel.onDigitPressCambioPin(digit = digit, onGuardado = onPinCambiado) },
        onBackspace = { viewModel.onBackspace() }
    )
}
