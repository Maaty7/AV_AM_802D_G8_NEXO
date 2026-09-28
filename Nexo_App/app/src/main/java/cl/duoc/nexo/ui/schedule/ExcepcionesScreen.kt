package cl.duoc.nexo.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.ExcepcionJornadaEntity
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.security.VerificarPinDialog
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.ExcepcionesJornadaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExcepcionesScreen(
    navController: NavHostController,
    viewModel: ExcepcionesJornadaViewModel = viewModel()
) {
    var mostrarModal by remember { mutableStateOf(false) }
    var excepcionAEliminar by remember { mutableStateOf<ExcepcionJornadaEntity?>(null) }
    var accionPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }

    PantallaConVolver(titulo = "Días sin clases", navController = navController) { innerPadding ->
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
                    text = "Vacaciones, paros u otros días en que NO corresponde generar informe, aunque coincida con el horario de una jornada.",
                    color = NexoMute,
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                viewModel.errorGuardado?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoCoral,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (viewModel.excepciones.isEmpty()) {
                    Text(
                        text = "Aún no tienes excepciones configuradas.",
                        color = NexoMute,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(viewModel.excepciones) { excepcion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                    .padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = excepcion.motivo,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${excepcion.fechaInicio} – ${excepcion.fechaTermino}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { excepcionAEliminar = excepcion }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Eliminar excepción",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { mostrarModal = true },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text("+ Agregar excepción", color = NexoDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarModal) {
        val sheetState = rememberModalBottomSheetState()
        var motivo by remember { mutableStateOf("") }
        var fechaInicio by remember { mutableStateOf("") }
        var fechaTermino by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { mostrarModal = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Nueva excepción", fontFamily = FontFamily.Serif, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it.replaceFirstChar(Char::uppercase) },
                    label = { Text("Motivo") },
                    placeholder = { Text("Ej. Vacaciones de invierno") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorDeFecha(
                        etiqueta = "Desde",
                        fecha = fechaInicio,
                        onFechaSeleccionada = { fechaInicio = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorDeFecha(
                        etiqueta = "Hasta",
                        fecha = fechaTermino,
                        onFechaSeleccionada = { fechaTermino = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { mostrarModal = false },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (fechaInicio.isNotBlank() && fechaTermino.isNotBlank()) {
                                val motivoFinal = motivo
                                val inicioFinal = fechaInicio
                                val terminoFinal = fechaTermino
                                accionPendiente = { viewModel.agregar(inicioFinal, terminoFinal, motivoFinal) }
                                mostrarModal = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NexoDeep),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar")
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    excepcionAEliminar?.let { excepcion ->
        AlertDialog(
            onDismissRequest = { excepcionAEliminar = null },
            title = { Text("¿Eliminar esta excepción?") },
            text = { Text("Volverá a generarse informe normalmente en ese rango de fechas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accionPendiente = { viewModel.eliminar(excepcion) }
                        excepcionAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = NexoCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { excepcionAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
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

/** Campo de solo lectura con la fecha seleccionada; al tocarlo abre un DatePickerDialog de Material3. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeFecha(
    etiqueta: String,
    fecha: String,
    onFechaSeleccionada: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var mostrarDialogo by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text(
            text = etiqueta,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = NexoMute,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .clickable { mostrarDialogo = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(text = fecha.ifBlank { "Elegir" }, fontSize = 14.sp)
        }
    }

    if (mostrarDialogo) {
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = parsearFechaAMillis(fecha) ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        estado.selectedDateMillis?.let { millis -> onFechaSeleccionada(formatearMillisAFecha(millis)) }
                        mostrarDialogo = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogo = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = estado)
        }
    }
}

private fun formatoFecha(): SimpleDateFormat {
    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
}

private fun formatearMillisAFecha(millis: Long): String = formatoFecha().format(Date(millis))

private fun parsearFechaAMillis(fecha: String): Long? {
    if (fecha.isBlank()) return null
    return try {
        formatoFecha().parse(fecha)?.time
    } catch (e: Exception) {
        null
    }
}
