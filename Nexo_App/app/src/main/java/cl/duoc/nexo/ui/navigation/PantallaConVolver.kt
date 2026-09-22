package cl.duoc.nexo.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

/**
 * Scaffold con flecha de retroceso para las pantallas secundarias a las que
 * se navega desde Configuración (Cambiar PIN, Correo, Apps supervisadas,
 * Acerca de). No se usa en las pestañas del bottom nav — esas ya tienen su
 * propia navegación visible vía NexoBottomBar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConVolver(
    titulo: String,
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}
