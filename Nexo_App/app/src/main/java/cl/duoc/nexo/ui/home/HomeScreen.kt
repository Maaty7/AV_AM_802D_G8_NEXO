package cl.duoc.nexo.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.components.AvisoPermisoUso
import cl.duoc.nexo.ui.components.ChipEstado
import cl.duoc.nexo.ui.components.ContadorAnimado
import cl.duoc.nexo.ui.components.DatoClave
import cl.duoc.nexo.ui.components.EncabezadoSeccion
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.NexoBotonPrincipal
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.NexoLogo
import cl.duoc.nexo.ui.components.TipoEstado
import cl.duoc.nexo.ui.components.entradaEnCascada
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.navigation.NexoDestinations
import cl.duoc.nexo.ui.theme.NUMEROS_TABULARES
import cl.duoc.nexo.ui.theme.NexoTextoSecundario
import cl.duoc.nexo.viewmodel.JornadaActivaViewModel
import java.util.Calendar

/** Categorías que cuentan como "distracción" para el dato clave de la Home. */
private val CATEGORIAS_DISTRACCION = setOf("Redes Sociales", "Entretenimiento", "Juegos")

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: JornadaActivaViewModel = viewModel()
) {
    val jornada = viewModel.jornadaReferencia
    val categorias = viewModel.minutosPorCategoria()

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

    val saludo = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Buenos días"
            in 12..19 -> "Buenas tardes"
            else -> "Buenas noches"
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(text = saludo, style = MaterialTheme.typography.bodyMedium, color = NexoTextoSecundario)
                        Text(text = "Hola", style = MaterialTheme.typography.headlineMedium)
                    }
                    NexoLogo(tamanoMarca = 40.dp, respirar = viewModel.jornadaEstaActiva)
                }

                Spacer(Modifier.height(24.dp))

                if (!viewModel.permisoUsoConcedido) {
                    AvisoPermisoUso(Modifier.entradaEnCascada(0))
                } else if (jornada == null) {
                    GlassCard(Modifier.fillMaxWidth().entradaEnCascada(0)) {
                        Text("Aún no tienes jornadas", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Crea la primera (por ejemplo, la jornada escolar) para empezar a ver el uso del teléfono.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexoTextoSecundario,
                            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                        )
                        NexoBotonPrincipal(
                            texto = "Crear jornada",
                            icono = Icons.Filled.Add,
                            onClick = { navController.navigate(NexoDestinations.HORARIOS) }
                        )
                    }
                } else {
                    // Estado de la jornada: la tarjeta protagonista
                    GlassCard(Modifier.fillMaxWidth().entradaEnCascada(0)) {
                        Text(
                            text = jornada.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            color = NexoTextoSecundario
                        )
                        Text(
                            text = "${jornada.horaInicio} – ${jornada.horaTermino}",
                            style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = NUMEROS_TABULARES),
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.jornadaEstaActiva) {
                                ChipEstado("Jornada activa", TipoEstado.EXITO)
                            } else {
                                ChipEstado("Fuera de horario", TipoEstado.NEUTRO)
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    EncabezadoSeccion("Resumen de hoy")

                    val total = viewModel.minutosTotales
                    val distraccion = CATEGORIAS_DISTRACCION.sumOf { categorias[it] ?: 0L }
                    val porcentajeDistraccion = if (total > 0) (distraccion * 100 / total) else 0L

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        MiniCard(
                            valor = total,
                            sufijo = " min",
                            etiqueta = "Tiempo total",
                            modifier = Modifier.weight(1f).entradaEnCascada(1)
                        )
                        MiniCard(
                            valor = categorias["Educación"] ?: 0L,
                            sufijo = " min",
                            etiqueta = "Educación",
                            modifier = Modifier.weight(1f).entradaEnCascada(2)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    GlassCard(Modifier.fillMaxWidth().entradaEnCascada(3)) {
                        Text("Distracción", style = MaterialTheme.typography.labelMedium, color = NexoTextoSecundario)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DatoClave("$porcentajeDistraccion %")
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "${distraccion} min en redes, juegos y entretenimiento",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexoTextoSecundario
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCard(valor: Long, sufijo: String, etiqueta: String, modifier: Modifier = Modifier) {
    GlassCard(modifier, relleno = 16.dp) {
        ContadorAnimado(valor = valor, sufijo = sufijo, estilo = MaterialTheme.typography.headlineSmall)
        Text(text = etiqueta, style = MaterialTheme.typography.bodySmall, color = NexoTextoSecundario)
    }
}
