package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.components.EncabezadoSeccion
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.MensajeError
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.entradaEnCascada
import cl.duoc.nexo.ui.navigation.NexoBottomBar
import cl.duoc.nexo.ui.navigation.NexoDestinations
import cl.duoc.nexo.ui.theme.NexoAcento
import cl.duoc.nexo.ui.theme.NexoBorde
import cl.duoc.nexo.ui.theme.NexoContenedorSuave
import cl.duoc.nexo.ui.theme.NexoTextoSecundario
import cl.duoc.nexo.ui.theme.nexoSwitchColors
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
    val cuenta = listOf(
        OpcionConfig(Icons.Filled.Lock, "Cambiar PIN") {
            navController.navigate(NexoDestinations.CAMBIAR_PIN)
        },
        OpcionConfig(Icons.Filled.Email, "Correo del apoderado") {
            navController.navigate(NexoDestinations.EDITAR_CORREO)
        },
        OpcionConfig(Icons.Filled.CloudSync, "Vincular cuenta") {
            navController.navigate(NexoDestinations.VINCULAR_CUENTA)
        }
    )
    val supervision = listOf(
        OpcionConfig(Icons.Filled.Apps, "Aplicaciones supervisadas") {
            navController.navigate(NexoDestinations.APPS_SUPERVISADAS)
        },
        OpcionConfig(Icons.Filled.Assessment, "Informes") {
            navController.navigate(NexoDestinations.INFORMES)
        },
        OpcionConfig(Icons.Filled.Schedule, "Horarios") {
            navController.navigate(NexoDestinations.HORARIOS)
        },
        OpcionConfig(Icons.Filled.EventBusy, "Días sin clases") {
            navController.navigate(NexoDestinations.EXCEPCIONES)
        }
    )
    val app = listOf(
        OpcionConfig(Icons.Filled.Info, "Acerca de NEXO") {
            navController.navigate(NexoDestinations.ACERCA_DE)
        }
    )

    NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.BAJA) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = { NexoBottomBar(navController) }
        ) { innerPadding ->
            // Con scroll: antes las 9 filas se cortaban en teléfonos de 5".
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 16.dp)
            ) {
                Text(
                    text = "Configuración",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                EncabezadoSeccion("Apariencia")
                GlassCard(Modifier.fillMaxWidth().entradaEnCascada(0), relleno = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconoFila(Icons.Filled.DarkMode)
                        Text(
                            text = "Tema oscuro",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        )
                        Switch(
                            checked = temaViewModel.temaOscuro,
                            onCheckedChange = { temaViewModel.alternarTema(it) },
                            colors = nexoSwitchColors()
                        )
                    }
                    temaViewModel.errorGuardado?.let { mensaje ->
                        MensajeError(mensaje, Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
                    }
                }

                Spacer(Modifier.height(24.dp))
                EncabezadoSeccion("Cuenta y seguridad")
                GrupoOpciones(cuenta, Modifier.entradaEnCascada(1))

                Spacer(Modifier.height(24.dp))
                EncabezadoSeccion("Supervisión")
                GrupoOpciones(supervision, Modifier.entradaEnCascada(2))

                Spacer(Modifier.height(24.dp))
                GrupoOpciones(app, Modifier.entradaEnCascada(3))
            }
        }
    }
}

@Composable
private fun GrupoOpciones(opciones: List<OpcionConfig>, modifier: Modifier = Modifier) {
    GlassCard(modifier.fillMaxWidth(), relleno = 8.dp) {
        opciones.forEachIndexed { indice, opcion ->
            if (indice > 0) {
                HorizontalDivider(
                    color = NexoBorde,
                    modifier = Modifier.padding(start = 60.dp, end = 8.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clickable(role = Role.Button) { opcion.onClick() }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconoFila(opcion.icono)
                Text(
                    text = opcion.etiqueta,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = NexoTextoSecundario
                )
            }
        }
    }
}

@Composable
private fun IconoFila(icono: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(NexoContenedorSuave, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icono,
            contentDescription = null,
            tint = NexoAcento,
            modifier = Modifier.size(20.dp)
        )
    }
}
