package cl.duoc.nexo.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.navigation.PantallaConVolver
import cl.duoc.nexo.ui.security.VerificarPinDialog
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.viewmodel.AplicacionesSupervisadasViewModel

@Composable
fun AplicacionesSupervisadasScreen(
    navController: NavHostController,
    viewModel: AplicacionesSupervisadasViewModel = viewModel()
) {
    var accionPendiente by remember { mutableStateOf<(() -> Unit)?>(null) }

    PantallaConVolver(titulo = "Aplicaciones supervisadas", navController = navController) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "Elige qué apps se incluyen en los informes de actividad.",
                    color = NexoMute,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                viewModel.errorGuardado?.let { mensaje ->
                    Text(
                        text = mensaje,
                        color = NexoCoral,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                LazyColumn {
                    items(viewModel.apps) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.nombre,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )
                            Switch(
                                checked = app.supervisada,
                                onCheckedChange = { nuevoValor ->
                                    accionPendiente = { viewModel.alternar(app.packageName, nuevoValor) }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = NexoDeep)
                            )
                        }
                    }
                }
            }
        }
    }

    accionPendiente?.let { accion ->
        VerificarPinDialog(
            onVerificado = {
                accion()
                accionPendiente = null
            },
            onCancelar = { accionPendiente = null }
        )
    }
}
