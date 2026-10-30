package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.security.VerificarPinDialog
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoGreen
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.VincularCuentaViewModel

@Composable
fun VincularCuentaScreen(
    navController: NavHostController,
    viewModel: VincularCuentaViewModel = viewModel()
) {
    var accionPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }

    PantallaConVolver(titulo = "Vincular cuenta", navController = navController) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Sincroniza los informes de jornada con tu cuenta — útil si usas NEXO en más de un teléfono o quieres respaldar los informes fuera del dispositivo.",
                    color = NexoMute,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                viewModel.error?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoCoral,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                viewModel.mensajeSincronizacion?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoGreen,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                viewModel.mensajeCodigo?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoGreen,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                when {
                    viewModel.vinculada -> SeccionVinculada(
                        email = viewModel.emailVinculado ?: "—",
                        cargando = viewModel.cargando,
                        onSincronizar = { viewModel.sincronizarAhora() },
                        onDesvincular = { accionPendiente = { viewModel.desvincular() } }
                    )
                    viewModel.desafioPendiente != null -> SeccionVerificarCodigo(
                        cargando = viewModel.cargando,
                        onVerificar = { codigo -> viewModel.verificarCodigo(codigo) },
                        onReenviar = { viewModel.reenviarCodigo() },
                        onCancelar = { viewModel.cancelarVerificacion() }
                    )
                    else -> SeccionSinVincular(
                        cargando = viewModel.cargando,
                        onRegistrar = { email, password, nombre ->
                            accionPendiente = { viewModel.registrar(email, password, nombre) }
                        },
                        onIniciarSesion = { email, password ->
                            accionPendiente = { viewModel.iniciarSesion(email, password) }
                        }
                    )
                }
            }
        }
    }

    accionPendiente?.let { accion ->
        VerificarPinDialog(
            onVerificado = {
                accion()
                accionPendiente = null
            },
            onCancelar = { accionPendiente = null }
        )
    }
}

@Composable
private fun SeccionVinculada(
    email: String,
    cargando: Boolean,
    onSincronizar: () -> Unit,
    onDesvincular: () -> Unit
) {
    Text(
        text = "Vinculada como $email",
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 20.dp)
    )

    Button(
        onClick = onSincronizar,
        enabled = !cargando,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexoDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (cargando) "Sincronizando…" else "Sincronizar ahora")
    }

    Spacer(modifier = Modifier.height(10.dp))

    OutlinedButton(
        onClick = onDesvincular,
        enabled = !cargando,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoCoral),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Desvincular")
    }
}

@Composable
private fun SeccionVerificarCodigo(
    cargando: Boolean,
    onVerificar: (codigo: String) -> Unit,
    onReenviar: () -> Unit,
    onCancelar: () -> Unit
) {
    var codigo by remember { mutableStateOf("") }

    Text(
        text = "Ingresa el código de 6 dígitos que te enviamos por correo.",
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 16.dp)
    )

    OutlinedTextField(
        value = codigo,
        onValueChange = { nuevoValor -> if (nuevoValor.length <= 6 && nuevoValor.all(Char::isDigit)) codigo = nuevoValor },
        label = { Text("Código") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = { onVerificar(codigo) },
        enabled = !cargando && codigo.length == 6,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexoDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(if (cargando) "Verificando…" else "Verificar")
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onCancelar, enabled = !cargando) {
            Text("Cancelar", fontSize = 12.5.sp)
        }
        TextButton(onClick = onReenviar, enabled = !cargando) {
            Text("Reenviar código", fontSize = 12.5.sp)
        }
    }
}

@Composable
private fun SeccionSinVincular(
    cargando: Boolean,
    onRegistrar: (email: String, password: String, nombre: String) -> Unit,
    onIniciarSesion: (email: String, password: String) -> Unit
) {
    var modoRegistro by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it },
        label = { Text("Email") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it },
        label = { Text("Contraseña") },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    )

    if (modoRegistro) {
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Tu nombre") },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = {
            if (modoRegistro) onRegistrar(email, password, nombre) else onIniciarSesion(email, password)
        },
        enabled = !cargando && email.isNotBlank() && password.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = NexoDeep),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            when {
                cargando -> "Conectando…"
                modoRegistro -> "Crear cuenta"
                else -> "Iniciar sesión"
            }
        )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = { modoRegistro = !modoRegistro }) {
            Text(
                if (modoRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate",
                fontSize = 12.5.sp
            )
        }
    }
}
