package cl.duoc.nexo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.nexo.ui.monitoring.UsageTestScreen
import cl.duoc.nexo.ui.onboarding.ConfigInicialScreen
import cl.duoc.nexo.ui.permissions.PermisosScreen
import cl.duoc.nexo.ui.security.CrearPinScreen
import cl.duoc.nexo.ui.splash.SplashScreen

@Composable
fun NexoNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = NexoDestinations.SPLASH
    ) {
        composable(NexoDestinations.SPLASH) {
            SplashScreen(onTimeout = {
                navController.navigate(NexoDestinations.CONFIG_INICIAL) {
                    popUpTo(NexoDestinations.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NexoDestinations.CONFIG_INICIAL) {
            ConfigInicialScreen(onContinuar = {
                navController.navigate(NexoDestinations.CREAR_PIN)
            })
        }
        composable(NexoDestinations.CREAR_PIN) {
            CrearPinScreen(onPinCreado = {
                navController.navigate(NexoDestinations.PERMISOS)
            })
        }
        composable(NexoDestinations.PERMISOS) {
            PermisosScreen(onContinuar = {
                navController.navigate(NexoDestinations.USAGE_TEST)
            })
        }
        composable(NexoDestinations.USAGE_TEST) {
            UsageTestScreen()
        }
    }
}