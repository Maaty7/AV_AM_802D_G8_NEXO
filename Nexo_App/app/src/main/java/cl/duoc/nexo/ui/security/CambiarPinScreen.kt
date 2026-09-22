package cl.duoc.nexo.ui.security

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.viewmodel.PinViewModel

@Composable
fun CambiarPinScreen(
    navController: NavHostController,
    viewModel: PinViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.iniciarCambioPin()
    }

    PantallaConVolver(titulo = "Cambiar PIN", navController = navController) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            PinPad(
                titulo = viewModel.titulo,
                subtitulo = viewModel.subtitulo,
                buffer = viewModel.buffer,
                error = viewModel.error,
                onDigit = { digit ->
                    viewModel.onDigitPressCambioPin(
                        digit = digit,
                        onGuardado = { navController.popBackStack() }
                    )
                },
                onBackspace = { viewModel.onBackspace() }
            )
        }
    }
}
