package cl.duoc.nexo.ui.onboarding

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.ConfigInicialViewModel

@Composable
fun ConfigInicialScreen(
    onContinuar: (nombre: String, correo: String) -> Unit,
    viewModel: ConfigInicialViewModel = viewModel()
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Bienvenido",
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp,
                modifier = Modifier.padding(top = 64.dp, bottom = 4.dp)
            )
            Text(
                text = "Configura la supervisión digital de tu hogar en un par de minutos.",
                color = NexoMute,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 22.dp)
            )

            Text(
                text = "Nombre del apoderado",
                color = NexoMute,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = viewModel.nombreApoderado,
                onValueChange = viewModel::onNombreChange,
                placeholder = { Text("Ej. María Contreras") },
                isError = viewModel.nombreError != null,
                supportingText = { viewModel.nombreError?.let { Text(it) } },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "Correo electrónico",
                color = NexoMute,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = viewModel.correoApoderado,
                onValueChange = viewModel::onCorreoChange,
                placeholder = { Text("maria@correo.cl") },
                isError = viewModel.correoError != null,
                supportingText = { viewModel.correoError?.let { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )

            Button(
                onClick = {
                    viewModel.validarYContinuar { nombre, correo ->
                        onContinuar(nombre, correo)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Text("Continuar")
            }
        }
    }
}