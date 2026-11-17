package cl.duoc.nexo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.FranjaUso
import cl.duoc.nexo.ui.components.AvisoPermisoUso
import cl.duoc.nexo.ui.components.BarraAnimada
import cl.duoc.nexo.ui.components.ContadorAnimado
import cl.duoc.nexo.ui.components.DatoClave
import cl.duoc.nexo.ui.components.DonaCategorias
import cl.duoc.nexo.ui.components.EncabezadoSeccion
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.NexoCargando
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.entradaEnCascada
import cl.duoc.nexo.ui.components.rememberProgresoAparicion
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NexoContenedorSuave
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoSky
import cl.duoc.nexo.ui.theme.NexoTextoSecundario
import cl.duoc.nexo.ui.theme.colorDeCategoria
import cl.duoc.nexo.ui.theme.coloresPorCategoria
import cl.duoc.nexo.ui.theme.esCategoriaNeon
import cl.duoc.nexo.viewmodel.JornadaActivaViewModel

// Utilidad en azul (productivo) y entretenimiento en neón (lo que hay que vigilar).
private val ColorUtilidad = NexoSky
private val ColorEntretenimiento = NexoNeon
private val ColorOtros = NexoSky.copy(alpha = 0.40f)

private val CATEGORIAS_DISTRACCION = setOf("Redes Sociales", "Entretenimiento", "Juegos")

@Composable
fun EstadisticasScreen(
    navController: NavHostController,
    viewModel: JornadaActivaViewModel = viewModel()
) {
    val minutosRegistrados = viewModel.minutosPorCategoria()
    // Se muestran todas las categorías siempre (aunque estén en 0) para que
    // la distribución se vea completa, no solo las categorías con actividad.
    val categorias = coloresPorCategoria.keys.associateWith { minutosRegistrados[it] ?: 0L }
    val hayActividad = categorias.values.any { it > 0 }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refrescarPermisoUso()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.BAJA) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { NexoBottomBar(navController) }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                Text(text = "Estadísticas", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = viewModel.jornadaReferencia?.let { "Actividad de la jornada «${it.nombre}»" }
                        ?: "Sin jornada configurada",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoTextoSecundario,
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                if (!viewModel.permisoUsoConcedido) {
                    AvisoPermisoUso()
                } else if (!hayActividad) {
                    GlassCard(Modifier.fillMaxWidth()) {
                        NexoCargando(Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = "Aún no hay actividad registrada en esta jornada.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoTextoSecundario,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                } else {
                    SeccionResumen(
                        appsUsadas = viewModel.appsUsadas.size,
                        minutosTotales = viewModel.minutosTotales,
                        appMasUsada = viewModel.appsUsadas.firstOrNull()?.appName ?: "—"
                    )

                    Spacer(Modifier.height(16.dp))
                    SeccionDistribucion(categorias, Modifier.entradaEnCascada(2))

                    Spacer(Modifier.height(16.dp))
                    SeccionUtilidadVsEntretenimiento(viewModel.utilidadVsEntretenimiento(), Modifier.entradaEnCascada(3))

                    Spacer(Modifier.height(16.dp))
                    SeccionCategorias(categorias, viewModel.appsUsadas, Modifier.entradaEnCascada(4))

                    Spacer(Modifier.height(16.dp))
                    SeccionAppsMasUsadas(viewModel.appsUsadas, Modifier.entradaEnCascada(5))

                    Spacer(Modifier.height(16.dp))
                    SeccionLineaDeTiempo(viewModel.usoPorFranjas, Modifier.entradaEnCascada(5))
                }
            }
        }
    }
}

@Composable
private fun SeccionResumen(appsUsadas: Int, minutosTotales: Long, appMasUsada: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassCard(Modifier.weight(1f).entradaEnCascada(0), relleno = 16.dp) {
            ContadorAnimado(appsUsadas.toLong(), estilo = MaterialTheme.typography.headlineSmall)
            Text("Apps usadas", style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
        }
        GlassCard(Modifier.weight(1f).entradaEnCascada(1), relleno = 16.dp) {
            ContadorAnimado(minutosTotales, sufijo = " min", estilo = MaterialTheme.typography.headlineSmall)
            Text("Tiempo total", style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
        }
    }
    Spacer(Modifier.height(12.dp))
    GlassCard(Modifier.fillMaxWidth().entradaEnCascada(1), relleno = 16.dp) {
        Text("Más usada", style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
        Text(
            text = appMasUsada,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SeccionDistribucion(categorias: Map<String, Long>, modifier: Modifier = Modifier) {
    val total = categorias.values.sum().coerceAtLeast(1L)
    val distraccion = CATEGORIAS_DISTRACCION.sumOf { categorias[it] ?: 0L }
    GlassCard(modifier.fillMaxWidth()) {
        EncabezadoSeccion("Distribución por categoría")
        Row(verticalAlignment = Alignment.CenterVertically) {
            DonaCategorias(categorias, tamano = 132.dp)
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Distracción", style = MaterialTheme.typography.labelMedium, color = NexoTextoSecundario)
                DatoClave("${distraccion * 100 / total} %")
                Spacer(Modifier.height(4.dp))
                categorias.entries.filter { it.value > 0 }.sortedByDescending { it.value }.take(4).forEach { (nombre, minutos) ->
                    Leyenda(colorDeCategoria(nombre), nombre, minutos, total, conBorde = esCategoriaNeon(nombre))
                }
            }
        }
    }
}

@Composable
private fun SeccionUtilidadVsEntretenimiento(minutos: Triple<Long, Long, Long>, modifier: Modifier = Modifier) {
    val (utilidad, entretenimiento, otros) = minutos
    val total = (utilidad + entretenimiento + otros).coerceAtLeast(1L)
    val progreso = rememberProgresoAparicion(800)
    val pista = NexoContenedorSuave

    GlassCard(modifier.fillMaxWidth()) {
        EncabezadoSeccion("Utilidad vs. entretenimiento")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(CircleShape)
                .drawBehind { drawRect(pista) }
        ) {
            val p = progreso.value
            val fu = utilidad.toFloat() / total * p
            val fe = entretenimiento.toFloat() / total * p
            val fo = otros.toFloat() / total * p
            BarraSegmento(fu, ColorUtilidad)
            BarraSegmento(fe, ColorEntretenimiento)
            BarraSegmento(fo, ColorOtros)
            val resto = 1f - (fu + fe + fo)
            if (resto > 0.001f) Spacer(Modifier.weight(resto))
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Leyenda(ColorUtilidad, "Utilidad", utilidad, total)
            Leyenda(ColorEntretenimiento, "Entreten.", entretenimiento, total, conBorde = true)
            Leyenda(ColorOtros, "Otros", otros, total)
        }
    }
}

@Composable
private fun RowScope.BarraSegmento(fraccion: Float, color: Color) {
    if (fraccion <= 0.001f) return
    Box(
        modifier = Modifier
            .weight(fraccion)
            .fillMaxHeight()
            .background(color)
    )
}

@Composable
private fun Leyenda(color: Color, etiqueta: String, valor: Long, total: Long, conBorde: Boolean = false) {
    val porcentaje = (valor * 100 / total).toInt()
    val borde = if (conBorde && !LocalNexoTemaOscuro.current) NexoDeep.copy(alpha = 0.20f) else Color.Transparent
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(borde, RoundedCornerShape(3.dp))
                .padding(1.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$etiqueta $porcentaje %",
            style = MaterialTheme.typography.bodySmall,
            color = NexoTextoSecundario,
            maxLines = 1
        )
    }
}

@Composable
private fun SeccionCategorias(categorias: Map<String, Long>, apps: List<AppUsageInfo>, modifier: Modifier = Modifier) {
    val maxMinutos = (categorias.values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    var categoriaSeleccionada by remember { mutableStateOf<String?>(null) }

    GlassCard(modifier.fillMaxWidth()) {
        EncabezadoSeccion("Minutos por categoría")
        categorias.entries.sortedByDescending { it.value }.forEachIndexed { i, (nombre, minutos) ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClickLabel = "Ver apps de $nombre") { categoriaSeleccionada = nombre }
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = nombre, style = MaterialTheme.typography.titleSmall)
                    Text(text = "$minutos min", style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
                }
                Spacer(modifier = Modifier.height(8.dp))
                BarraAnimada(
                    fraccion = minutos.toFloat() / maxMinutos,
                    color = colorDeCategoria(nombre),
                    bordeNeon = esCategoriaNeon(nombre),
                    retrasoMs = i * 50
                )
            }
        }
    }

    categoriaSeleccionada?.let { categoria ->
        DetalleCategoriaDialog(
            categoria = categoria,
            apps = apps.filter { it.categoria == categoria }.sortedByDescending { it.totalTimeMinutes },
            onDismiss = { categoriaSeleccionada = null }
        )
    }
}

@Composable
private fun DetalleCategoriaDialog(categoria: String, apps: List<AppUsageInfo>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(categoria, style = MaterialTheme.typography.titleLarge) },
        text = {
            if (apps.isEmpty()) {
                Text("Sin apps registradas en esta categoría.", style = MaterialTheme.typography.bodyMedium, color = NexoTextoSecundario)
            } else {
                Column {
                    apps.forEach { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f, fill = false),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${app.totalTimeMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexoTextoSecundario
                            )
                        }
                    }
                }
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
private fun SeccionAppsMasUsadas(apps: List<AppUsageInfo>, modifier: Modifier = Modifier) {
    val top = apps.take(5)
    val maxMinutos = (top.maxOfOrNull { it.totalTimeMinutes } ?: 0L).coerceAtLeast(1L)

    GlassCard(modifier.fillMaxWidth()) {
        EncabezadoSeccion("Apps más usadas")
        top.forEachIndexed { i, app ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(text = "${app.totalTimeMinutes} min", style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
                }
                Spacer(modifier = Modifier.height(8.dp))
                BarraAnimada(
                    fraccion = app.totalTimeMinutes.toFloat() / maxMinutos,
                    color = colorDeCategoria(app.categoria),
                    alto = 8.dp,
                    bordeNeon = esCategoriaNeon(app.categoria),
                    retrasoMs = i * 50
                )
            }
        }
    }
}

@Composable
private fun SeccionLineaDeTiempo(franjas: List<FranjaUso>, modifier: Modifier = Modifier) {
    if (franjas.isEmpty()) return
    val maxMinutos = (franjas.maxOfOrNull { it.minutos } ?: 0L).coerceAtLeast(1L)
    // Muestra la etiqueta de hora cada cierto número de barras para que no se amontonen.
    val pasoEtiquetas = (franjas.size / 6).coerceAtLeast(1)
    val progreso = rememberProgresoAparicion(800)
    val pista = NexoContenedorSuave
    val colorBarra = MaterialTheme.colorScheme.primary

    GlassCard(modifier.fillMaxWidth()) {
        EncabezadoSeccion("Uso a lo largo de la jornada")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            franjas.forEach { franja ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .drawBehind {
                            val fraccion = (franja.minutos.toFloat() / maxMinutos) * progreso.value
                            val alto = (size.height * fraccion).coerceAtLeast(3.dp.toPx())
                            drawRoundRect(
                                brush = if (franja.minutos > 0) {
                                    Brush.verticalGradient(
                                        listOf(NexoSky, colorBarra),
                                        startY = size.height - alto,
                                        endY = size.height
                                    )
                                } else Brush.verticalGradient(listOf(pista, pista)),
                                topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - alto),
                                size = androidx.compose.ui.geometry.Size(size.width, alto),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
                            )
                        }
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            franjas.forEachIndexed { indice, franja ->
                Text(
                    text = if (indice % pasoEtiquetas == 0) franja.etiqueta else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexoTextoSecundario,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
