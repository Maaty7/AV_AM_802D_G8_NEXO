package cl.duoc.nexo.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.nexo.ui.home.HomeScreen
import cl.duoc.nexo.ui.monitoring.UsageTestScreen
import cl.duoc.nexo.ui.onboarding.ConfigInicialScreen
import cl.duoc.nexo.ui.permissions.PermisosScreen
import cl.duoc.nexo.ui.schedule.HorariosScreen
import cl.duoc.nexo.ui.security.CrearPinScreen
import cl.duoc.nexo.ui.settings.ConfiguracionScreen
import cl.duoc.nexo.ui.splash.SplashScreen
import cl.duoc.nexo.ui.stats.EstadisticasScreen

@Composable
fun NexoNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NexoDestinations.SPLASH
    ) {
        composable(NexoDestinations.SPLASH) {
            SplashScreen(onDecidido = { existeConfiguracion ->
                val destino = if (existeConfiguracion) {
                    NexoDestinations.HOME
                } else {
                    NexoDestinations.CONFIG_INICIAL
                }
                navController.navigate(destino) {
                    popUpTo(NexoDestinations.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NexoDestinations.CONFIG_INICIAL) {
            ConfigInicialScreen(onContinuar = { nombre, correo ->
                navController.navigate(
                    "${NexoDestinations.CREAR_PIN}/${Uri.encode(nombre)}/${Uri.encode(correo)}"
                )
            })
        }
        composable(
            route = "${NexoDestinations.CREAR_PIN}/{nombre}/{correo}",
            arguments = listOf(
                navArgument("nombre") { type = NavType.StringType },
                navArgument("correo") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val nombre = backStackEntry.arguments?.getString("nombre") ?: ""
            val correo = backStackEntry.arguments?.getString("correo") ?: ""
            CrearPinScreen(
                nombre = nombre,
                correo = correo,
                onPinCreado = {
                    navController.navigate(NexoDestinations.PERMISOS)
                }
            )
        }
        composable(NexoDestinations.PERMISOS) {
            PermisosScreen(onContinuar = {
                navController.navigate(NexoDestinations.HOME) {
                    popUpTo(NexoDestinations.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NexoDestinations.HOME) {
            HomeScreen(navController)
        }
        composable(NexoDestinations.USAGE_TEST) {
            Scaffold(
                bottomBar = { NexoBottomBar(navController) }
            ) { innerPadding ->
                UsageTestScreen(modifier = Modifier.padding(innerPadding))
            }
        }
        composable(NexoDestinations.HORARIOS) {
            HorariosScreen(navController)
        }
        composable(NexoDestinations.ESTADISTICAS) {
            EstadisticasScreen(navController)
        }
        composable(NexoDestinations.CONFIGURACION) {
            ConfiguracionScreen(navController)
        }
    }
}