package cl.duoc.nexo.ui.reports

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.InformesViewModel

@Composable
fun InformesScreen(
    navController: NavHostController,
    viewModel: InformesViewModel = viewModel()
) {
    var informeSeleccionado by remember { mutableStateOf<ReportEntity?>(null) }
    val context = LocalContext.current

    PantallaConVolver(titulo = "Informes", navController = navController) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (viewModel.informes.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text(
                        text = "Aún no hay informes generados. Se crean automáticamente al terminar cada jornada.",
                        color = NexoMute,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    items(viewModel.informes) { informe ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .clickable { informeSeleccionado = informe }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = informe.nombreJornada,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${informe.fecha} · ${informe.minutosTotales} min totales",
                                        fontSize = 11.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { compartirInforme(context, informe) }) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = "Descargar informe",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    informeSeleccionado?.let { informe ->
        DetalleInformeDialog(informe = informe, onDismiss = { informeSeleccionado = null })
    }
}

@Composable
private fun DetalleInformeDialog(informe: ReportEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(informe.nombreJornada) },
        text = {
            Column {
                Text(
                    text = "${informe.fecha} · ${informe.horaInicio}–${informe.horaTermino}",
                    fontSize = 12.5.sp,
                    color = NexoMute,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                FilaCategoria("Educación", informe.minutosEducacion)
                FilaCategoria("Comunicación", informe.minutosComunicacion)
                FilaCategoria("Redes Sociales", informe.minutosRedesSociales)
                FilaCategoria("Entretenimiento", informe.minutosEntretenimiento)
                FilaCategoria("Juegos", informe.minutosJuegos)
                FilaCategoria("Otros", informe.minutosOtros)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
private fun FilaCategoria(nombre: String, minutos: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = nombre, fontSize = 13.sp)
        Text(text = "$minutos min", fontSize = 12.5.sp, color = NexoMute)
    }
}

private fun construirResumenTexto(informe: ReportEntity): String = buildString {
    appendLine("Informe de jornada — ${informe.nombreJornada}")
    appendLine("Fecha: ${informe.fecha}")
    appendLine("Horario: ${informe.horaInicio}–${informe.horaTermino}")
    appendLine("Tiempo total: ${informe.minutosTotales} min")
    appendLine()
    appendLine("Por categoría:")
    appendLine("Educación: ${informe.minutosEducacion} min")
    appendLine("Comunicación: ${informe.minutosComunicacion} min")
    appendLine("Redes Sociales: ${informe.minutosRedesSociales} min")
    appendLine("Entretenimiento: ${informe.minutosEntretenimiento} min")
    appendLine("Juegos: ${informe.minutosJuegos} min")
    appendLine("Otros: ${informe.minutosOtros} min")
    appendLine()
    append("Generado automáticamente por NEXO.")
}

private fun compartirInforme(context: Context, informe: ReportEntity) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Informe NEXO — ${informe.nombreJornada}")
        putExtra(Intent.EXTRA_TEXT, construirResumenTexto(informe))
    }
    context.startActivity(Intent.createChooser(intent, "Compartir informe"))
}
