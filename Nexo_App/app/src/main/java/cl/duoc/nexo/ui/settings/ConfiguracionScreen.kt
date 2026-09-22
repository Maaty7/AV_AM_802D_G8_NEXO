package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.navigation.NexoDestinations
import cl.duoc.nexo.ui.theme.NexoChevron
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.viewmodel.TemaViewModel

private data class OpcionConfig(
    val icono: ImageVector,
    val etiqueta: String,
    val onClick: () -> Unit
)

@Composable
fun ConfiguracionScreen(
    navController: NavHostController,
    temaViewModel: TemaViewModel = viewModel()
) {
    val opciones = listOf(
        OpcionConfig(Icons.Filled.Lock, "Cambiar PIN") {
            navController.navigate(NexoDestinations.CAMBIAR_PIN)
        },
        OpcionConfig(Icons.Filled.Email, "Correo del apoderado") {
            navController.navigate(NexoDestinations.EDITAR_CORREO)
        },
        OpcionConfig(Icons.Filled.Apps, "Aplicaciones supervisadas") {
            navController.navigate(NexoDestinations.APPS_SUPERVISADAS)
        },
        OpcionConfig(Icons.Filled.Assessment, "Informes") {
            navController.navigate(NexoDestinations.INFORMES)
        },
        OpcionConfig(Icons.Filled.Schedule, "Horarios") {
            navController.navigate(NexoDestinations.HORARIOS)
        },
        OpcionConfig(Icons.Filled.Info, "Acerca de NEXO") {
            navController.navigate(NexoDestinations.ACERCA_DE)
        }
    )

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
                Text(
                    text = "Configuración",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(NexoIce, RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DarkMode,
                            contentDescription = null,
                            tint = NexoDeep,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Tema oscuro",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 13.dp)
                    )
                    Switch(
                        checked = temaViewModel.temaOscuro,
                        onCheckedChange = { temaViewModel.alternarTema(it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = NexoDeep)
                    )
                }
                temaViewModel.errorGuardado?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoCoral,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                opciones.forEach { opcion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { opcion.onClick() }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(NexoIce, RoundedCornerShape(9.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = opcion.icono,
                                contentDescription = null,
                                tint = NexoDeep,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = opcion.etiqueta,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 13.dp)
                        )
                        Text(text = "›", color = NexoChevron, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}