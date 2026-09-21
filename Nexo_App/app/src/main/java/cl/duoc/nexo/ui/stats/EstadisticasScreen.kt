package cl.duoc.nexo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.ui.theme.NexoTeal

private data class Categoria(val nombre: String, val minutos: Int, val color: Color)

@Composable
fun EstadisticasScreen(navController: NavHostController) {
    // TODO: reemplazar con datos reales de UsageStatsRepository filtrados por categoría
    val categorias = listOf(
        Categoria("Educación", 50, NexoDeep),
        Categoria("Comunicación", 13, NexoTeal),
        Categoria("Entretenimiento", 21, NexoCoral)
    )
    val maxMinutos = categorias.maxOf { it.minutos }.coerceAtLeast(1)

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
                Text(text = "Estadísticas", fontFamily = FontFamily.Serif, fontSize = 22.sp)
                Text(
                    text = "Distribución por categoría — jornada de hoy",
                    color = NexoMute,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                categorias.forEach { categoria ->
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = categoria.nombre,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(text = "${categoria.minutos} min", fontSize = 12.5.sp)
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
                                    .fillMaxWidth(categoria.minutos.toFloat() / maxMinutos)
                                    .fillMaxSize()
                                    .background(categoria.color, RoundedCornerShape(5.dp))
                            ) {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SEMANA",
                    color = NexoMute,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Lun" to "2h 10m", "Mar" to "1h 42m", "Mié" to "2h 20m").forEach { (dia, tiempo) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(NexoIce, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = dia, fontFamily = FontFamily.Serif, fontSize = 16.sp, color = NexoDeep)
                            Text(text = tiempo, fontSize = 10.5.sp, color = NexoMute)
                        }
                    }
                }
            }
        }
    }
}