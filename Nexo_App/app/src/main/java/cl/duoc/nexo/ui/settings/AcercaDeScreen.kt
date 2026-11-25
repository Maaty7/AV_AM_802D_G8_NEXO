package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.components.CircuitField
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.ModoCircuito
import cl.duoc.nexo.ui.components.NexoLogo
import cl.duoc.nexo.ui.components.VarianteLogo
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NexoTextoSecundario

@Composable
fun AcercaDeScreen(navController: NavHostController) {
    PantallaConVolver(titulo = "Acerca de NEXO", navController = navController) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Logo grande con reflejo y circuitos en LOOP lento
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircuitField(
                    modifier = Modifier.fillMaxSize(),
                    modo = ModoCircuito.LOOP,
                    sobreOscuro = LocalNexoTemaOscuro.current,
                    origen = Offset(0.5f, 0.42f),
                    radioOrigen = 0.30f,
                    densidad = 10,
                    velocidad = 0.5f,
                    brillo = 0.8f
                )
                NexoLogo(variante = VarianteLogo.VERTICAL, tamanoMarca = 112.dp, respirar = true, reflejo = true)
            }

            Text(
                text = "Versión 1.0 · MVP académico",
                style = MaterialTheme.typography.bodySmall,
                color = NexoTextoSecundario,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 24.dp)
            )

            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    text = "NEXO es un proyecto Capstone de Ingeniería en Informática (DuocUC) que ayuda a los " +
                        "apoderados a supervisar el uso del teléfono durante jornadas específicas (como el " +
                        "horario escolar), usando únicamente datos oficiales de Android: apps usadas, duración " +
                        "y horario. Nunca se recopilan mensajes, contraseñas, capturas de pantalla ni contenido " +
                        "privado.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "EQUIPO",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexoTextoSecundario,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Matías Esquerra · Cristopher Paredes · Martín Muñoz · Cristóbal Monsalves",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
