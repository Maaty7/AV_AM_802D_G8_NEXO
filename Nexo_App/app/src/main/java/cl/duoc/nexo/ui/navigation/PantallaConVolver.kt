package cl.duoc.nexo.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import cl.duoc.nexo.ui.theme.NexoMist
import cl.duoc.nexo.ui.theme.NexoNight

/**
 * Scaffold con flecha de retroceso para las pantallas secundarias a las que
 * se navega desde Configuración (Cambiar PIN, Correo, Apps supervisadas,
 * Acerca de). No se usa en las pestañas del bottom nav — esas ya tienen su
 * propia navegación visible vía NexoBottomBar.
 *
 * @param sobreFondoOscuro para pantallas con fondo oscuro fijo (Cambiar PIN):
 *   barra transparente sobre NexoNight con texto/íconos NexoMist.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaConVolver(
    titulo: String,
    navController: NavHostController,
    sobreFondoOscuro: Boolean = false,
    content: @Composable (PaddingValues) -> Unit
) {
    val colores = if (sobreFondoOscuro) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = NexoNight,
            titleContentColor = NexoMist,
            navigationIconContentColor = NexoMist
        )
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            navigationIconContentColor = MaterialTheme.colorScheme.primary
        )
    }
    Scaffold(
        containerColor = if (sobreFondoOscuro) NexoNight else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
                colors = colores,
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
