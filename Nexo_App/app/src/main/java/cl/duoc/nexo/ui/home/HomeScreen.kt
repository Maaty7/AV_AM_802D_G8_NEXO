package cl.duoc.nexo.ui.home

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
import androidx.compose.ui.graphics.Brush
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
import cl.duoc.nexo.ui.theme.NexoMidnight
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.JornadaActivaViewModel

@Composable
fun HomeScreen(
    navController: NavHostController,
    viewModel: JornadaActivaViewModel = viewModel()
) {
    val jornada = viewModel.jornadaReferencia
    val categorias = viewModel.minutosPorCategoria()

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
                Text(text = "Buenos días", color = NexoMute, fontSize = 12.sp)
                Text(
                    text = "Hola 👋",
                    fontFamily = FontFamily.Serif,
                    fontSize = 21.sp,
                    modifier = Modifier.padding(bottom = 18.dp)
                )

                if (jornada == null) {
                    Text(
                        text = "Aún no tienes jornadas configuradas.\nVe a la pestaña Horarios para crear la primera.",
                        color = NexoMute,
                        fontSize = 13.sp
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(listOf(NexoDeep, NexoMidnight)),
                                RoundedCornerShape(16.dp)
                            )
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "🏫 ${jornada.nombre}",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.5.sp
                        )
                        Text(
                            text = "${jornada.horaInicio} — ${jornada.horaTermino}",
                            color = Color.White,
                            fontFamily = FontFamily.Serif,
                            fontSize = 22.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )
                        Row(
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (viewModel.jornadaEstaActiva) "🟢 ACTIVA" else "⚪ INACTIVA",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "RESUMEN",
                        color = NexoMute,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        MiniCard(
                            valor = "${viewModel.minutosTotales}m",
                            etiqueta = "Tiempo total",
                            modifier = Modifier.weight(1f)
                        )
                        MiniCard(
                            valor = "${categorias["Educación"] ?: 0}m",
                            etiqueta = "Educación",
                            modifier = Modifier.weight(1f)
                        )
                        MiniCard(
                            valor = "${categorias["Entretenimiento"] ?: 0}m",
                            etiqueta = "Entreten.",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCard(valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(NexoIce, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = valor, fontFamily = FontFamily.Serif, fontSize = 18.sp, color = NexoDeep)
        Text(text = etiqueta, fontSize = 10.5.sp, color = NexoMute)
    }
}