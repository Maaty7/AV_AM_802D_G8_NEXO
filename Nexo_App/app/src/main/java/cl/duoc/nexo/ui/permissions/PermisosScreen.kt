package cl.duoc.nexo.ui.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import cl.duoc.nexo.monitoring.UsageAccessPermission
import cl.duoc.nexo.ui.components.ChipEstado
import cl.duoc.nexo.ui.components.GlassCard
import cl.duoc.nexo.ui.components.IndicadorPasos
import cl.duoc.nexo.ui.components.IntensidadGradiente
import cl.duoc.nexo.ui.components.NexoBotonPrincipal
import cl.duoc.nexo.ui.components.NexoGradientBackground
import cl.duoc.nexo.ui.components.TipoEstado
import cl.duoc.nexo.ui.components.entradaEnCascada
import cl.duoc.nexo.ui.theme.NexoAcento
import cl.duoc.nexo.ui.theme.NexoContenedorSuave
import cl.duoc.nexo.ui.theme.NexoTextoSecundario

@Composable
fun PermisosScreen(onContinuar: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permisoConcedido by remember {
        mutableStateOf(UsageAccessPermission.isGranted(context))
    }

    // Vuelve a revisar el permiso cada vez que la app regresa a primer plano
    // (por ejemplo, al volver de Ajustes de Android)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permisoConcedido = UsageAccessPermission.isGranted(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    NexoGradientBackground(Modifier.fillMaxSize(), IntensidadGradiente.MEDIA) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IndicadorPasos(actual = 3, total = 3)
                Spacer(Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(NexoContenedorSuave, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        tint = NexoAcento,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(text = "Permisos necesarios", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Para obtener estadísticas de uso, NEXO necesita el acceso especial de Android:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexoTextoSecundario,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                GlassCard(Modifier.fillMaxWidth().entradaEnCascada(0)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = "Acceso a datos de uso", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Usage Access",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexoTextoSecundario
                            )
                        }
                        if (permisoConcedido) {
                            ChipEstado("Concedido", TipoEstado.EXITO)
                        } else {
                            ChipEstado("Pendiente", TipoEstado.ALERTA)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (!permisoConcedido) {
                    NexoBotonPrincipal(
                        texto = "Ir a configuración",
                        icono = Icons.Filled.Settings,
                        onClick = { UsageAccessPermission.openSettings(context) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    NexoBotonPrincipal(
                        texto = "Continuar",
                        icono = Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = onContinuar,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
