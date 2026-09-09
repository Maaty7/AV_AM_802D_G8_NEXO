package cl.duoc.nexo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import cl.duoc.nexo.ui.onboarding.ConfigInicialScreen
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
                    // Elimina el Splash del back stack para que
                    // el botón "atrás" no vuelva a él
                    popUpTo(NexoDestinations.SPLASH) { inclusive = true }
                }
            })
        }
        composable(NexoDestinations.CONFIG_INICIAL) {
            ConfigInicialScreen()
        }
    }
}