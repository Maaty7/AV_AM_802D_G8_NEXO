package cl.duoc.nexo.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

private data class NexoTab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    NexoTab(NexoDestinations.HOME, "Inicio", Icons.Filled.Home),
    NexoTab(NexoDestinations.ESTADISTICAS, "Stats", Icons.Filled.BarChart),
    NexoTab(NexoDestinations.HORARIOS, "Horarios", Icons.Filled.Schedule),
    NexoTab(NexoDestinations.CONFIGURACION, "Ajustes", Icons.Filled.Settings)
)

@Composable
fun NexoBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    NavigationBar {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = {
                    if (currentRoute != tab.route) {
                        navController.navigate(tab.route) {
                            popUpTo(NexoDestinations.HOME) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}