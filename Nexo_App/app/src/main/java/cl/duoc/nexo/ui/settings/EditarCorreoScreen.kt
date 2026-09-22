package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.security.VerificarPinDialog
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.EditarCorreoViewModel

@Composable
fun EditarCorreoScreen(
    navController: NavHostController,
    viewModel: EditarCorreoViewModel = viewModel()
) {
    var mostrarVerificacion by remember { mutableStateOf(false) }

    PantallaConVolver(titulo = "Correo del apoderado", navController = navController) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "A este correo llegarán los informes cuando NEXO tenga envío por correo.",
                    color = NexoMute,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 22.dp)
                )
                OutlinedTextField(
                    value = viewModel.correo,
                    onValueChange = viewModel::onCorreoChange,
                    placeholder = { Text("maria@correo.cl") },
                    isError = viewModel.error != null,
                    supportingText = { viewModel.error?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                Button(
                    onClick = { if (viewModel.validar()) mostrarVerificacion = true },
                    enabled = !viewModel.guardando,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar")
                }
            }
        }
    }

    if (mostrarVerificacion) {
        VerificarPinDialog(
            onVerificado = {
                mostrarVerificacion = false
                viewModel.guardar { navController.popBackStack() }
            },
            onCancelar = { mostrarVerificacion = false }
        )
    }
}
