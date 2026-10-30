package cl.duoc.nexo.ui.navigation

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cl.duoc.nexo.security.SesionDesbloqueo
import cl.duoc.nexo.ui.home.HomeScreen
import cl.duoc.nexo.ui.onboarding.ConfigInicialScreen
import cl.duoc.nexo.ui.permissions.PermisosScreen
import cl.duoc.nexo.ui.reports.InformesScreen
import cl.duoc.nexo.ui.schedule.ExcepcionesScreen
import cl.duoc.nexo.ui.schedule.HorariosScreen
import cl.duoc.nexo.ui.schedule.PausasJornadaScreen
import cl.duoc.nexo.ui.security.CambiarPinScreen
import cl.duoc.nexo.ui.security.CrearPinScreen
import cl.duoc.nexo.ui.security.VerificarPinScreen
import cl.duoc.nexo.ui.settings.AcercaDeScreen
import cl.duoc.nexo.ui.settings.AplicacionesSupervisadasScreen
import cl.duoc.nexo.ui.settings.ConfiguracionScreen
import cl.duoc.nexo.ui.settings.EditarCorreoScreen
import cl.duoc.nexo.ui.settings.VincularCuentaScreen
import cl.duoc.nexo.ui.splash.SplashScreen
import cl.duoc.nexo.ui.stats.EstadisticasScreen
import kotlinx.coroutines.flow.first

/**
 * Rutas alcanzables ANTES de verificar el PIN en este proceso. Es una lista
 * BLANCA a propósito (no una lista negra de "pantallas protegidas"): así,
 * cualquier pantalla nueva que se agregue más adelante queda protegida por
 * defecto si alguien olvida clasificarla, en vez de quedar desprotegida.
 */
private val RUTAS_SIN_PIN_PREVIO = setOf(
    NexoDestinations.SPLASH,
    NexoDestinations.CONFIG_INICIAL,
    "${NexoDestinations.CREAR_PIN}/{nombre}/{correo}",
    NexoDestinations.PERMISOS,
    NexoDestinations.VERIFICAR_PIN
)

@Composable
fun NexoNavHost(navController: NavHostController = rememberNavController()) {
    // Si Android mata el proceso en segundo plano y lo recrea al reabrir la
    // app desde Recientes, el NavHost restaura su backstack tal cual estaba
    // — incluida la pantalla protegida en la que había quedado — sin volver
    // a pasar por SPLASH/VERIFICAR_PIN. SesionDesbloqueo es un flag que vive
    // solo en memoria: en un proceso nuevo siempre arranca en `false`, así
    // que lo usamos para tapar cualquier pantalla restaurada hasta confirmar
    // que corresponde mostrarla o forzar el PIN primero.
    var bloqueando by remember { mutableStateOf(!SesionDesbloqueo.desbloqueada) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = NexoDestinations.SPLASH
        ) {
            composable(NexoDestinations.SPLASH) {
                SplashScreen(onDecidido = { existeConfiguracion ->
                    // Si ya existe una ParentSettingsEntity guardada, el onboarding se
                    // completó (nombre + correo + PIN se guardan juntos al final de ese
                    // flujo), así que solo falta verificar el PIN antes de dejar pasar.
                    val destino = if (existeConfiguracion) {
                        NexoDestinations.VERIFICAR_PIN
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
                    SesionDesbloqueo.desbloqueada = true
                    navController.navigate(NexoDestinations.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                })
            }
            composable(NexoDestinations.VERIFICAR_PIN) {
                VerificarPinScreen(onPinCorrecto = {
                    SesionDesbloqueo.desbloqueada = true
                    navController.navigate(NexoDestinations.HOME) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                })
            }
            composable(NexoDestinations.HOME) {
                HomeScreen(navController)
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
            composable(NexoDestinations.CAMBIAR_PIN) {
                CambiarPinScreen(navController)
            }
            composable(NexoDestinations.EDITAR_CORREO) {
                EditarCorreoScreen(navController)
            }
            composable(NexoDestinations.APPS_SUPERVISADAS) {
                AplicacionesSupervisadasScreen(navController)
            }
            composable(NexoDestinations.ACERCA_DE) {
                AcercaDeScreen(navController)
            }
            composable(NexoDestinations.INFORMES) {
                InformesScreen(navController)
            }
            composable(NexoDestinations.EXCEPCIONES) {
                ExcepcionesScreen(navController)
            }
            composable(
                route = "${NexoDestinations.PAUSAS_JORNADA}/{scheduleId}",
                arguments = listOf(navArgument("scheduleId") { type = NavType.IntType })
            ) { backStackEntry ->
                val scheduleId = backStackEntry.arguments?.getInt("scheduleId") ?: -1
                PausasJornadaScreen(navController = navController, scheduleId = scheduleId)
            }
            composable(NexoDestinations.VINCULAR_CUENTA) {
                VincularCuentaScreen(navController)
            }
        }

        LaunchedEffect(Unit) {
            if (SesionDesbloqueo.desbloqueada) {
                bloqueando = false
                return@LaunchedEffect
            }
            // Espera la primera ruta ya resuelta por el NavHost (restaurada o
            // el startDestination normal de un proceso nuevo), en vez de leer
            // currentDestination directo — así no hay carrera con la propia
            // inicialización del NavHost.
            val ruta = navController.currentBackStackEntryFlow.first().destination.route
            if (ruta == null || ruta !in RUTAS_SIN_PIN_PREVIO) {
                navController.navigate(NexoDestinations.VERIFICAR_PIN) {
                    popUpTo(navController.graph.id) { inclusive = true }
                }
            }
            bloqueando = false
        }

        if (bloqueando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}