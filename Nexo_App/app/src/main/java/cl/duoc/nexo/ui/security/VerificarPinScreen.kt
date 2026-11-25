package cl.duoc.nexo.ui.security

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.components.rememberAnimacionesActivas
import cl.duoc.nexo.viewmodel.PinViewModel
import kotlinx.coroutines.delay

/**
 * Tiempo que se deja ver el "clic" del candado al acertar el PIN antes de
 * pasar a la siguiente pantalla. Es solo presentación: la verificación ya
 * ocurrió en el ViewModel.
 */
internal const val MS_ANIMACION_DESBLOQUEO = 520L

/** Se muestra cada vez que se abre la app, si ya hay una configuración guardada, antes de dejar pasar a Home. */
@Composable
fun VerificarPinScreen(
    onPinCorrecto: () -> Unit,
    viewModel: PinViewModel = viewModel()
) {
    val animar = rememberAnimacionesActivas()
    var desbloqueado by remember { mutableStateOf(false) }
    val alTerminar by rememberUpdatedState(onPinCorrecto)

    LaunchedEffect(Unit) {
        viewModel.iniciarVerificacionAcceso()
    }
    LaunchedEffect(desbloqueado) {
        if (desbloqueado) {
            if (animar) delay(MS_ANIMACION_DESBLOQUEO)
            alTerminar()
        }
    }

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
