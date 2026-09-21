package cl.duoc.nexo.ui.schedule

import androidx.compose.foundation.background
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
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.HorariosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorariosScreen(
    navController: NavHostController,
    viewModel: HorariosViewModel = viewModel()
) {
    var mostrarModal by remember { mutableStateOf(false) }

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
                                    .background(NexoIce, RoundedCornerShape(14.dp))
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
                                        color = NexoMute
                                    )
                                }
                                Text(text = "›", color = NexoMute, fontSize = 16.sp)
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
                    Text("+ Crear jornada", color = NexoDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (mostrarModal) {
        val sheetState = rememberModalBottomSheetState()
        var nombre by remember { mutableStateOf("") }
        var horaInicio by remember { mutableStateOf("") }
        var horaTermino by remember { mutableStateOf("") }
        val diasSeleccionados = remember { mutableStateOf(setOf("L", "M", "Mi", "J", "V")) }
        val diasDisponibles = listOf("L", "M", "Mi", "J", "V", "S", "D")

        ModalBottomSheet(
            onDismissRequest = { mostrarModal = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Nueva jornada", fontFamily = FontFamily.Serif, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    placeholder = { Text("Ej. Horario nocturno") },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = NexoIce,
                        focusedContainerColor = NexoIce
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = horaInicio,
                        onValueChange = { horaInicio = it },
                        label = { Text("Hora inicio") },
                        placeholder = { Text("08:00") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoIce,
                            focusedContainerColor = NexoIce
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = horaTermino,
                        onValueChange = { horaTermino = it },
                        label = { Text("Hora término") },
                        placeholder = { Text("14:00") },
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = NexoIce,
                            focusedContainerColor = NexoIce
                        ),
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
                                .background(if (seleccionado) NexoDeep else NexoIce, CircleShape)
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
                                text = dia.take(1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (seleccionado) Color.White else NexoMute
                            )
                        }
                    }
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
                            val diasTexto = diasDisponibles
                                .filter { diasSeleccionados.value.contains(it) }
                                .joinToString("")
                            viewModel.agregarJornada(nombre, horaInicio, horaTermino, diasTexto)
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
}