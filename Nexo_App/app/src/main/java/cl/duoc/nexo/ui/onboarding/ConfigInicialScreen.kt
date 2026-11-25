package cl.duoc.nexo.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IndicadorPasos
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.NexoBotonPrincipal
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.NexoLogo
import cl.duoc.nexo.ui.components.VarianteLogo
import cl.duoc.nexo.ui.components.entradaEnCascada
import cl.duoc.nexo.ui.theme.NexoTextoSecundario
import cl.duoc.nexo.viewmodel.ConfigInicialViewModel

@Composable
fun ConfigInicialScreen(
    onContinuar: (nombre: String, correo: String) -> Unit,
    viewModel: ConfigInicialViewModel = viewModel()
) {
    NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.MEDIA) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                IndicadorPasos(actual = 1, total = 3)
                Spacer(Modifier.height(32.dp))
                NexoLogo(variante = VarianteLogo.HORIZONTAL, tamanoMarca = 44.dp)
                Spacer(Modifier.height(24.dp))
                Text(text = "Bienvenido", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Configura la supervisión digital de tu hogar en un par de minutos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoTextoSecundario,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                GlassCard(Modifier.fillMaxWidth().entradaEnCascada(0)) {
                    Etiqueta("Nombre del apoderado")
                    OutlinedTextField(
                        value = viewModel.nombreApoderado,
                        onValueChange = viewModel::onNombreChange,
                        placeholder = { Text("Ej. María Contreras") },
                        isError = viewModel.nombreError != null,
                        supportingText = { viewModel.nombreError?.let { Text(it) } },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = coloresCampo(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )

                    Etiqueta("Correo electrónico")
                    OutlinedTextField(
                        value = viewModel.correoApoderado,
                        onValueChange = viewModel::onCorreoChange,
                        placeholder = { Text("maria@correo.cl") },
                        isError = viewModel.correoError != null,
                        supportingText = { viewModel.correoError?.let { Text(it) } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = coloresCampo(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(24.dp))
                NexoBotonPrincipal(
                    texto = "Continuar",
                    icono = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        viewModel.validarYContinuar { nombre, correo ->
                            onContinuar(nombre, correo)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelMedium,
        color = NexoTextoSecundario,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun coloresCampo() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedBorderColor = MaterialTheme.colorScheme.primary
)
