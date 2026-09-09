package cl.duoc.nexo.ui.usagetest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.viewmodel.UsageTestViewModel
import java.util.concurrent.TimeUnit

/**
 * Pantalla de prueba de factibilidad: pide Usage Access, detecta si fue
 * otorgado, y muestra las apps usadas hoy con su duración. No forma parte
 * del flujo final del MVP, es la validación técnica previa (prioridad #1).
 */
@Composable
fun UsageTestScreen(
    modifier: Modifier = Modifier,
    viewModel: UsageTestViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando el usuario vuelve de Ajustes (tras otorgar o denegar el permiso),
    // el proceso no se destruye, así que hay que revisar el estado en onResume.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionState()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Prueba de factibilidad — UsageStatsManager",
            style = MaterialTheme.typography.titleLarge
        )

        Text(
            text = "Permiso Usage Access: " +
                if (uiState.isPermissionGranted) "OTORGADO" else "NO OTORGADO",
            style = MaterialTheme.typography.bodyLarge
        )

        if (!uiState.isPermissionGranted) {
            Text(
                text = "NEXO necesita este permiso para leer cuánto tiempo se usa " +
                    "cada app. Se otorga manualmente en Ajustes de Android.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { viewModel.requestUsageAccess() }) {
                Text("Otorgar acceso a datos de uso")
            }
        } else {
            Button(
                onClick = { viewModel.loadTodayUsage() },
                enabled = !uiState.isLoading
            ) {
                Text("Consultar uso de hoy")
            }
        }

        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }

        if (uiState.hasQueried && !uiState.isLoading) {
            if (uiState.appUsageList.isEmpty()) {
                Text(
                    text = "No se encontró uso registrado hoy para ninguna app.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = "${uiState.appUsageList.size} apps con uso hoy:",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(count = uiState.appUsageList.size) { index ->
                        AppUsageRow(uiState.appUsageList[index])
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUsageRow(usage: AppUsageInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = usage.appName, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = usage.packageName,
                style = MaterialTheme.typography.bodySmall
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = "Tiempo en uso: ${formatDuration(usage.totalTimeForegroundMs)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/** Formatea una duración en milisegundos como "Hh Mm Ss". */
private fun formatDuration(durationMs: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
    return buildString {
        if (hours > 0) append("${hours}h ")
        if (hours > 0 || minutes > 0) append("${minutes}m ")
        append("${seconds}s")
    }
}
