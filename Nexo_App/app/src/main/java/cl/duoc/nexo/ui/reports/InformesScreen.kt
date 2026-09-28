package cl.duoc.nexo.ui.reports

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Visibility
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
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.pdf.InformePdfGenerator
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.InformesViewModel

private const val TAG = "InformesScreen"

@Composable
fun InformesScreen(
    navController: NavHostController,
    viewModel: InformesViewModel = viewModel()
) {
    var informeSeleccionado by remember { mutableStateOf<ReportEntity?>(null) }
    var informeParaDescargar by remember { mutableStateOf<ReportEntity?>(null) }
    var errorPdf by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    // Storage Access Framework: el usuario elige dónde guardar el PDF (por
    // defecto suele ofrecer Descargas, pero puede elegir otra carpeta). Solo
    // se dispara cuando se toca el botón "Descargar" — ver un informe nunca
    // guarda nada por su cuenta.
    val descargarLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uriDestino ->
        val informe = informeParaDescargar
        informeParaDescargar = null
        if (uriDestino != null && informe != null) {
            try {
                errorPdf = null
                val archivo = InformePdfGenerator.generarOReutilizar(context, informe)
                context.contentResolver.openOutputStream(uriDestino)?.use { salida ->
                    archivo.inputStream().use { it.copyTo(salida) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al descargar el informe ${informe.id}", e)
                errorPdf = "No se pudo descargar el informe. Intenta de nuevo."
            }
        }
    }

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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    errorPdf?.let { mensaje ->
                        Text(
                            text = mensaje,
                            color = NexoCoral,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                                    IconButton(
                                        onClick = {
                                            try {
                                                errorPdf = null
                                                verInforme(context, informe)
                                            } catch (e: Exception) {
                                                Log.e(TAG, "Error al abrir el informe ${informe.id}", e)
                                                errorPdf = "No se pudo abrir el PDF. ¿Tienes un visor de PDF instalado?"
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Visibility,
                                            contentDescription = "Ver informe",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            informeParaDescargar = informe
                                            descargarLauncher.launch("informe_${informe.fecha}.pdf")
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Download,
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
    }

    informeSeleccionado?.let { informe ->
        DetalleInformeDialog(informe = informe, onDismiss = { informeSeleccionado = null })
    }
}

/**
 * Genera (o reutiliza) el PDF en el cache interno y lo abre con el visor de
 * PDF del sistema vía ACTION_VIEW — solo para mirarlo, no queda guardado en
 * ningún lugar accesible para el usuario.
 */
private fun verInforme(context: Context, informe: ReportEntity) {
    val archivo = InformePdfGenerator.generarOReutilizar(context, informe)
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        throw IllegalStateException("No hay ninguna app instalada que pueda abrir PDF", e)
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
