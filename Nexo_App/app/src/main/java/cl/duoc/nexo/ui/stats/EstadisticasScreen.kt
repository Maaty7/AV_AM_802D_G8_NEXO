package cl.duoc.nexo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.FranjaUso
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoGreen
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.ui.theme.NexoTeal
import cl.duoc.nexo.viewmodel.InformesViewModel
import cl.duoc.nexo.viewmodel.JornadaActivaViewModel

private val coloresPorCategoria = mapOf(
    "Educación" to NexoDeep,
    "Comunicación" to NexoTeal,
    "Entretenimiento" to NexoCoral,
    "Juegos" to Color(0xFFB45309),
    "Otros" to NexoMute
)

private val NexoUtilidad = NexoGreen
private val NexoEntretenimiento = NexoCoral
private val NexoOtrosColor = NexoMute

@Composable
fun EstadisticasScreen(
    navController: NavHostController,
    viewModel: JornadaActivaViewModel = viewModel(),
    informesViewModel: InformesViewModel = viewModel()
) {
    val minutosRegistrados = viewModel.minutosPorCategoria()
    // Se muestran las 5 categorías siempre (aunque estén en 0) para que la
    // distribución se vea completa, no solo las categorías con actividad.
    val categorias = coloresPorCategoria.keys.associateWith { minutosRegistrados[it] ?: 0L }
    val hayActividad = categorias.values.any { it > 0 }

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
                    .verticalScroll(rememberScrollState())
                    .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 24.dp)
            ) {
                Text(text = "Estadísticas", fontFamily = FontFamily.Serif, fontSize = 22.sp)
                Text(
                    text = if (viewModel.jornadaReferencia != null) {
                        "Dashboard de actividad — ${viewModel.jornadaReferencia!!.nombre}"
                    } else {
                        "Sin jornada configurada"
                    },
                    color = NexoMute,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                if (!hayActividad) {
                    Text(
                        text = "Aún no hay actividad registrada en esta jornada.",
                        color = NexoMute,
                        fontSize = 13.sp
                    )
                } else {
                    SeccionResumen(
                        appsUsadas = viewModel.appsUsadas.size,
                        minutosTotales = viewModel.minutosTotales,
                        appMasUsada = viewModel.appsUsadas.firstOrNull()?.appName ?: "—"
                    )

                    Spacer(modifier = Modifier.height(22.dp))
                    SeccionUtilidadVsEntretenimiento(viewModel.utilidadVsEntretenimiento())

                    Spacer(modifier = Modifier.height(22.dp))
                    SeccionCategorias(categorias)

                    Spacer(modifier = Modifier.height(22.dp))
                    SeccionAppsMasUsadas(viewModel.appsUsadas)

                    Spacer(modifier = Modifier.height(22.dp))
                    SeccionLineaDeTiempo(viewModel.usoPorFranjas)
                }

                if (informesViewModel.informes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(22.dp))
                    SeccionHistorialInformes(informesViewModel.informes)
                }
            }
        }
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        color = NexoMute,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun SeccionResumen(appsUsadas: Int, minutosTotales: Long, appMasUsada: String) {
    Column {
        TituloSeccion("RESUMEN")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ResumenCard(valor = "$appsUsadas", etiqueta = "Apps usadas", modifier = Modifier.weight(1f))
            ResumenCard(valor = "${minutosTotales}m", etiqueta = "Tiempo total", modifier = Modifier.weight(1f))
            ResumenCard(valor = appMasUsada, etiqueta = "Más usada", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ResumenCard(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(NexoIce, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor,
            fontFamily = FontFamily.Serif,
            fontSize = 16.sp,
            color = NexoDeep,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = etiqueta, fontSize = 10.5.sp, color = NexoMute)
    }
}

@Composable
private fun SeccionUtilidadVsEntretenimiento(minutos: Triple<Long, Long, Long>) {
    val (utilidad, entretenimiento, otros) = minutos
    val total = (utilidad + entretenimiento + otros).coerceAtLeast(1L)

    Column {
        TituloSeccion("UTILIDAD VS. ENTRETENIMIENTO")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(NexoIce, RoundedCornerShape(7.dp))
        ) {
            BarraSegmento(utilidad, total, NexoUtilidad, esPrimero = true)
            BarraSegmento(entretenimiento, total, NexoEntretenimiento)
            BarraSegmento(otros, total, NexoOtrosColor, esUltimo = true)
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda(NexoUtilidad, "Utilidad", utilidad, total)
            Leyenda(NexoEntretenimiento, "Entreten.", entretenimiento, total)
            Leyenda(NexoOtrosColor, "Otros", otros, total)
        }
    }
}

@Composable
private fun RowScope.BarraSegmento(
    valor: Long,
    total: Long,
    color: Color,
    esPrimero: Boolean = false,
    esUltimo: Boolean = false
) {
    if (valor <= 0L) return
    val forma = RoundedCornerShape(
        topStart = if (esPrimero) 7.dp else 0.dp,
        bottomStart = if (esPrimero) 7.dp else 0.dp,
        topEnd = if (esUltimo) 7.dp else 0.dp,
        bottomEnd = if (esUltimo) 7.dp else 0.dp
    )
    Row(
        modifier = Modifier
            .fillMaxWidth(valor.toFloat() / total)
            .fillMaxSize()
            .background(color, forma)
    ) {}
}

@Composable
private fun Leyenda(color: Color, etiqueta: String, valor: Long, total: Long) {
    val porcentaje = (valor * 100 / total).toInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier
                .height(8.dp)
                .width(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        ) {}
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "$etiqueta $porcentaje%", fontSize = 11.sp, color = NexoMute)
    }
}

@Composable
private fun SeccionCategorias(categorias: Map<String, Long>) {
    val maxMinutos = (categorias.values.maxOrNull() ?: 0L).coerceAtLeast(1L)

    Column {
        TituloSeccion("DISTRIBUCIÓN POR CATEGORÍA")
        categorias.entries.sortedByDescending { it.value }.forEach { (nombre, minutos) ->
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = nombre, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "$minutos min", fontSize = 12.5.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(NexoIce, RoundedCornerShape(5.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(minutos.toFloat() / maxMinutos)
                            .fillMaxSize()
                            .background(
                                coloresPorCategoria[nombre] ?: NexoMute,
                                RoundedCornerShape(5.dp)
                            )
                    ) {}
                }
            }
        }
    }
}

@Composable
private fun SeccionAppsMasUsadas(apps: List<AppUsageInfo>) {
    val top = apps.take(5)
    val maxMinutos = (top.maxOfOrNull { it.totalTimeMinutes } ?: 0L).coerceAtLeast(1L)

    Column {
        TituloSeccion("APPS MÁS USADAS")
        top.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = app.appName,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(text = "${app.totalTimeMinutes} min", fontSize = 11.5.sp, color = NexoMute)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .background(NexoIce, RoundedCornerShape(4.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(app.totalTimeMinutes.toFloat() / maxMinutos)
                                .fillMaxSize()
                                .background(
                                    coloresPorCategoria[app.categoria] ?: NexoMute,
                                    RoundedCornerShape(4.dp)
                                )
                        ) {}
                    }
                }
            }
        }
    }
}

@Composable
private fun SeccionLineaDeTiempo(franjas: List<FranjaUso>) {
    if (franjas.isEmpty()) return
    val maxMinutos = (franjas.maxOfOrNull { it.minutos } ?: 0L).coerceAtLeast(1L)
    // Muestra la etiqueta de hora cada cierto número de barras para que no se amontonen.
    val pasoEtiquetas = (franjas.size / 6).coerceAtLeast(1)

    Column {
        TituloSeccion("USO A LO LARGO DE LA JORNADA")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            franjas.forEach { franja ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((franja.minutos.toFloat() / maxMinutos * 52).dp.coerceAtLeast(2.dp))
                            .background(
                                if (franja.minutos > 0) NexoDeep else NexoIce,
                                RoundedCornerShape(3.dp)
                            )
                    ) {}
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            franjas.forEachIndexed { indice, franja ->
                Text(
                    text = if (indice % pasoEtiquetas == 0) franja.etiqueta else "",
                    fontSize = 8.5.sp,
                    color = NexoMute,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SeccionHistorialInformes(informes: List<ReportEntity>) {
    Column {
        TituloSeccion("HISTORIAL DE INFORMES")
        Text(
            text = "Generados automáticamente al terminar cada jornada.",
            fontSize = 11.sp,
            color = NexoMute,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        informes.take(10).forEach { informe ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(NexoIce, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = informe.nombreJornada, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = informe.fecha, fontSize = 11.sp, color = NexoMute)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${informe.horaInicio}–${informe.horaTermino} · ${informe.minutosTotales} min totales",
                    fontSize = 11.5.sp,
                    color = NexoMute
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Educación ${informe.minutosEducacion}m · Comunicación ${informe.minutosComunicacion}m · " +
                        "Entreten. ${informe.minutosEntretenimiento}m · Juegos ${informe.minutosJuegos}m · " +
                        "Otros ${informe.minutosOtros}m",
                    fontSize = 10.5.sp,
                    color = NexoMute
                )
            }
        }
    }
}
