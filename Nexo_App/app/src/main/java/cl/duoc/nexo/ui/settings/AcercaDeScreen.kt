package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.theme.NexoMute

@Composable
fun AcercaDeScreen(navController: NavHostController) {
    PantallaConVolver(titulo = "Acerca de NEXO", navController = navController) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Versión 1.0 · MVP académico",
                    color = NexoMute,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                Text(
                    text = "NEXO es un proyecto Capstone de Ingeniería en Informática (DuocUC) que ayuda a los " +
                        "apoderados a supervisar el uso del teléfono durante jornadas específicas (como el " +
                        "horario escolar), usando únicamente datos oficiales de Android: apps usadas, duración " +
                        "y horario. Nunca se recopilan mensajes, contraseñas, capturas de pantalla ni contenido " +
                        "privado.",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(bottom = 22.dp)
                )
                Text(
                    text = "Equipo",
                    color = NexoMute,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "Matías Esquerra · Cristopher Paredes · Martín Muñoz · Cristóbal Monsalves",
                    fontSize = 12.5.sp
                )
            }
        }
    }
}
