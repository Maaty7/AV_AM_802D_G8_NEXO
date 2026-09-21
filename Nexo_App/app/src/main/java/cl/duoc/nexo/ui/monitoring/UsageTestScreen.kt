package cl.duoc.nexo.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.UsageTestViewModel

@Composable
fun UsageTestScreen(
    viewModel: UsageTestViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.cargarUsoDeHoy()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = "Actividad",
                fontFamily = FontFamily.Serif,
                fontSize = 22.sp
            )
            Text(
                text = "Uso de hoy (prueba de factibilidad)",
                color = NexoMute,
                fontSize = 13.sp,
                modifier = Modifier.padding(bottom = 20.dp, top = 4.dp)
            )

            if (viewModel.apps.isEmpty()) {
                Text(
                    text = "No hay datos de uso disponibles.\nVerifica que el permiso de Usage Access esté activo, y que hayas usado alguna app hoy.",
                    color = NexoMute,
                    fontSize = 13.sp
                )
            } else {
                LazyColumn {
                    items(viewModel.apps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(NexoDeep, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = app.appName.take(1).uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "${app.totalTimeMinutes} min",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = NexoMute.copy(alpha = 0.15f))
                    }
                }
            }
        }
    }
}