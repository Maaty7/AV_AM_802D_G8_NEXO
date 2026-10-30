package cl.duoc.nexo.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.PausaJornadaEntity
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.security.VerificarPinDialog
import cl.duoc.nexo.ui.theme.NexoBorder
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.PausasJornadaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PausasJornadaScreen(
    navController: NavHostController,
    scheduleId: Int,
    viewModel: PausasJornadaViewModel = viewModel()
) {
    LaunchedEffect(scheduleId) {
        viewModel.cargar(scheduleId)
    }

    var mostrarModal by remember { mutableStateOf(false) }
    var pausaAEliminar by remember { mutableStateOf<PausaJornadaEntity?>(null) }
    var accionPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }

    PantallaConVolver(titulo = "Recreos y descansos", navController = navController) { innerPadding ->
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
                    text = "El uso de apps durante estos tramos no se cuenta en el tiempo total de la jornada.",
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

                if (viewModel.pausas.isEmpty()) {
                    Text(
                        text = "Aún no tienes recreos ni descansos configurados para esta jornada.",
                        color = NexoMute,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(viewModel.pausas) { pausa ->
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
                                        text = pausa.nombre,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${pausa.horaInicio} – ${pausa.horaTermino}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { pausaAEliminar = pausa }) {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = "Eliminar pausa",
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
                    Text("+ Agregar pausa", color = NexoDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarModal) {
        val sheetState = rememberModalBottomSheetState()
        var nombre by remember { mutableStateOf("") }
        var horaInicio by remember { mutableStateOf("10:00") }
        var horaTermino by remember { mutableStateOf("10:15") }

        ModalBottomSheet(
            onDismissRequest = { mostrarModal = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Nueva pausa", fontFamily = FontFamily.Serif, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.replaceFirstChar(Char::uppercase) },
                    label = { Text("Nombre") },
                    placeholder = { Text("Recreo") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorDeHoraPausa(
                        etiqueta = "Hora inicio",
                        hora = horaInicio,
                        onHoraSeleccionada = { horaInicio = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorDeHoraPausa(
                        etiqueta = "Hora término",
                        hora = horaTermino,
                        onHoraSeleccionada = { horaTermino = it },
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
                            val nombreFinal = nombre
                            val horaInicioFinal = horaInicio
                            val horaTerminoFinal = horaTermino
                            accionPendiente = { viewModel.agregar(nombreFinal, horaInicioFinal, horaTerminoFinal) }
                            mostrarModal = false
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

    pausaAEliminar?.let { pausa ->
        AlertDialog(
            onDismissRequest = { pausaAEliminar = null },
            title = { Text("¿Eliminar esta pausa?") },
            text = { Text("El uso de apps en ese tramo volverá a contar en el total de la jornada.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        accionPendiente = { viewModel.eliminar(pausa) }
                        pausaAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = NexoCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { pausaAEliminar = null }) {
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

/** Campo de solo lectura con la hora seleccionada; al tocarlo abre un TimePickerDialog (formato 24h). Igual a SelectorDeHora de HorariosScreen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeHoraPausa(
    etiqueta: String,
    hora: String,
    onHoraSeleccionada: (String) -> Unit,
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
                .border(1.dp, NexoBorder, RoundedCornerShape(10.dp))
                .clickable { mostrarDialogo = true }
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(text = hora, fontSize = 14.sp)
        }
    }

    if (mostrarDialogo) {
        val partes = hora.split(":")
        val horaInicial = partes.getOrNull(0)?.toIntOrNull() ?: 10
        val minutoInicial = partes.getOrNull(1)?.toIntOrNull() ?: 0
        val estado = rememberTimePickerState(
            initialHour = horaInicial,
            initialMinute = minutoInicial,
            is24Hour = true
        )

        Dialog(onDismissRequest = { mostrarDialogo = false }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = estado)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { mostrarDialogo = false }) {
                            Text("Cancelar")
                        }
                        TextButton(
                            onClick = {
                                onHoraSeleccionada("%02d:%02d".format(estado.hour, estado.minute))
                                mostrarDialogo = false
                            }
                        ) {
                            Text("Aceptar")
                        }
                    }
                }
            }
        }
    }
}
