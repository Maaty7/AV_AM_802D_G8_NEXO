package cl.duoc.nexo.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.theme.NexoBorder
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.HorariosViewModel

private val diasDisponibles = listOf("LU", "MA", "MI", "JU", "VI", "SA", "DO")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(
    navController: NavHostController,
    viewModel: HorariosViewModel = viewModel()
) {
    var mostrarModal by remember { mutableStateOf(false) }
    var jornadaEditando by remember { mutableStateOf<ScheduleEntity?>(null) }
    var jornadaAEliminar by remember { mutableStateOf<ScheduleEntity?>(null) }

    Scaffold(
        bottomBar = { NexoBottomBar(navController) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(text = "Mis jornadas", fontFamily = FontFamily.Serif, fontSize = 22.sp)
                Text(
                    text = "Períodos de supervisión configurados",
                    color = NexoMute,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )

                if (viewModel.jornadas.isEmpty()) {
                    Text(
                        text = "Aún no tienes jornadas configuradas.\nToca \"+ Crear jornada\" para agregar la primera.",
                        color = NexoMute,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 40.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(viewModel.jornadas) { jornada ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                                    .clickable {
                                        jornadaEditando = jornada
                                        mostrarModal = true
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = jornada.nombre,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${jornada.dias} · ${jornada.horaInicio}–${jornada.horaTermino}",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(text = "›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        jornadaEditando = null
                        mostrarModal = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Text("+ Crear jornada", color = NexoDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarModal) {
        val jornada = jornadaEditando
        val sheetState = rememberModalBottomSheetState()
        var nombre by remember(jornada) { mutableStateOf(jornada?.nombre ?: "") }
        var horaInicio by remember(jornada) { mutableStateOf(jornada?.horaInicio ?: "08:00") }
        var horaTermino by remember(jornada) { mutableStateOf(jornada?.horaTermino ?: "14:00") }
        val diasSeleccionados = remember(jornada) {
            val iniciales = jornada?.dias
                ?.split(",")
                ?.map { it.trim() }
                ?.toSet()
                ?: setOf("LU", "MA", "MI", "JU", "VI")
            mutableStateOf(iniciales)
        }

        ModalBottomSheet(
            onDismissRequest = { mostrarModal = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (jornada == null) "Nueva jornada" else "Editar jornada",
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it.replaceFirstChar(Char::uppercase) },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej. Horario nocturno") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectorDeHora(
                        etiqueta = "Hora inicio",
                        hora = horaInicio,
                        onHoraSeleccionada = { horaInicio = it },
                        modifier = Modifier.weight(1f)
                    )
                    SelectorDeHora(
                        etiqueta = "Hora término",
                        hora = horaTermino,
                        onHoraSeleccionada = { horaTermino = it },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Días",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = NexoMute,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    diasDisponibles.forEach { dia ->
                        val seleccionado = diasSeleccionados.value.contains(dia)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    if (seleccionado) NexoDeep else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .clickable {
                                    diasSeleccionados.value = if (seleccionado) {
                                        diasSeleccionados.value - dia
                                    } else {
                                        diasSeleccionados.value + dia
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dia,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (seleccionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (jornada == null) {
                        OutlinedButton(
                            onClick = { mostrarModal = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancelar")
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                jornadaAEliminar = jornada
                                mostrarModal = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NexoCoral),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Eliminar")
                        }
                    }
                    Button(
                        onClick = {
                            val diasTexto = diasDisponibles
                                .filter { diasSeleccionados.value.contains(it) }
                                .joinToString(", ")
                            if (jornada == null) {
                                viewModel.agregarJornada(nombre, horaInicio, horaTermino, diasTexto)
                            } else {
                                viewModel.actualizarJornada(jornada, nombre, horaInicio, horaTermino, diasTexto)
                            }
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

    jornadaAEliminar?.let { jornada ->
        AlertDialog(
            onDismissRequest = { jornadaAEliminar = null },
            title = { Text("¿Eliminar esta jornada?") },
            text = { Text("Se cancelará su supervisión automática.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.eliminarJornada(jornada)
                        jornadaAEliminar = null
                    }
                ) {
                    Text("Eliminar", color = NexoCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { jornadaAEliminar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/** Campo de solo lectura con la hora seleccionada; al tocarlo abre un TimePickerDialog (formato 24h). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorDeHora(
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
        val horaInicial = partes.getOrNull(0)?.toIntOrNull() ?: 8
        val minutoInicial = partes.getOrNull(1)?.toIntOrNull() ?: 0
        val estado = rememberTimePickerState(
            initialHour = horaInicial,
            initialMinute = minutoInicial,
            is24Hour = true
        )

        TimePickerDialog(
            onDismissRequest = { mostrarDialogo = false },
            onConfirmar = {
                onHoraSeleccionada("%02d:%02d".format(estado.hour, estado.minute))
                mostrarDialogo = false
            }
        ) {
            TimePicker(state = estado)
        }
    }
}

/** Material3 no trae un TimePickerDialog listo para usar (a diferencia de DatePickerDialog); este es el patrón recomendado oficialmente para envolver un TimePicker en un diálogo. */
@Composable
private fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onConfirmar: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                content()
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    TextButton(onClick = onConfirmar) {
                        Text("Aceptar")
                    }
                }
            }
        }
    }
}