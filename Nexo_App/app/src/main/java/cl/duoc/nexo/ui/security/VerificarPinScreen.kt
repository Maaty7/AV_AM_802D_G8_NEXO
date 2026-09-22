package cl.duoc.nexo.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.viewmodel.PinViewModel

/** Se muestra cada vez que se abre la app, si ya hay una configuración guardada, antes de dejar pasar a Home. */
@Composable
fun VerificarPinScreen(
    onPinCorrecto: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.iniciarVerificacionAcceso()
    }

    PinPad(
        titulo = viewModel.titulo,
        subtitulo = viewModel.subtitulo,
        buffer = viewModel.buffer,
        error = viewModel.error,
        onDigit = { digit -> viewModel.onDigitPressVerificarAcceso(digit = digit, onCorrecto = onPinCorrecto) },
        onBackspace = { viewModel.onBackspace() }
    )
}
