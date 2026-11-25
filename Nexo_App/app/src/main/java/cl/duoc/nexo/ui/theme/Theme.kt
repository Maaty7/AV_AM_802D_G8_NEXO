package cl.duoc.nexo.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema claro: NexoMist ~55 %, azules ~38 %, neón ~7 %.
 * primary = Deep (texto/acciones), secondary = Sky, tertiary = Neon.
 * El "error" usa Deep sobre claro porque el neón no se lee como texto
 * en fondo blanco; los rellenos de error usan errorContainer (neón).
 */
private val NexoLightColorScheme = lightColorScheme(
    primary = NexoDeep,
    onPrimary = NexoMist,
    primaryContainer = NexoSkySoft,
    onPrimaryContainer = NexoDeep,
    inversePrimary = NexoSky,
    secondary = NexoSky,
    onSecondary = NexoDeep,
    secondaryContainer = NexoSkySoft,
    onSecondaryContainer = NexoDeep,
    tertiary = NexoNeon,
    onTertiary = NexoDeep,
    tertiaryContainer = NexoNeon.copy(alpha = 0.20f),
    onTertiaryContainer = NexoDeep,
    background = NexoMist,
    onBackground = NexoDeep,
    surface = NexoMist,
    onSurface = NexoDeep,
    surfaceVariant = NexoSkySoft.copy(alpha = 0.45f),
    onSurfaceVariant = NexoDeep.copy(alpha = 0.65f),
    surfaceTint = NexoSky,
    inverseSurface = NexoDeep,
    inverseOnSurface = NexoMist,
    error = NexoDeep,
    onError = NexoMist,
    errorContainer = NexoNeon,
    onErrorContainer = NexoDeep,
    outline = NexoDeep.copy(alpha = 0.35f),
    outlineVariant = NexoDeep.copy(alpha = 0.12f),
    scrim = NexoAbyss,
    surfaceBright = NexoMist,
    surfaceDim = NexoSkySoft.copy(alpha = 0.60f),
    surfaceContainerLowest = NexoMist,
    surfaceContainerLow = NexoSkySoft.copy(alpha = 0.18f),
    surfaceContainer = NexoSkySoft.copy(alpha = 0.28f),
    surfaceContainerHigh = NexoSkySoft.copy(alpha = 0.40f),
    surfaceContainerHighest = NexoSkySoft.copy(alpha = 0.55f)
)

/**
 * Tema oscuro: NexoNight/Abyss ~60 %, azules ~30 %, neón ~10 %.
 * primary = Sky (el Deep no se lee sobre Night).
 */
private val NexoDarkColorScheme = darkColorScheme(
    primary = NexoSky,
    onPrimary = NexoDeep,
    primaryContainer = NexoDeep,
    onPrimaryContainer = NexoMist,
    inversePrimary = NexoDeep,
    secondary = NexoSky,
    onSecondary = NexoDeep,
    secondaryContainer = NexoDeep,
    onSecondaryContainer = NexoMist,
    tertiary = NexoNeon,
    onTertiary = NexoDeep,
    tertiaryContainer = NexoNeon.copy(alpha = 0.15f),
    onTertiaryContainer = NexoNeon,
    background = NexoNight,
    onBackground = NexoMist,
    surface = NexoNight,
    onSurface = NexoMist,
    surfaceVariant = NexoDeep.copy(alpha = 0.55f),
    onSurfaceVariant = NexoMist.copy(alpha = 0.70f),
    surfaceTint = NexoSky,
    inverseSurface = NexoMist,
    inverseOnSurface = NexoDeep,
    error = NexoNeon,
    onError = NexoDeep,
    errorContainer = NexoNeon,
    onErrorContainer = NexoDeep,
    outline = NexoSky.copy(alpha = 0.35f),
    outlineVariant = NexoSky.copy(alpha = 0.15f),
    scrim = NexoAbyss,
    surfaceBright = NexoDeep,
    surfaceDim = NexoAbyss,
    surfaceContainerLowest = NexoAbyss,
    surfaceContainerLow = NexoDeep.copy(alpha = 0.35f),
    surfaceContainer = NexoDeep.copy(alpha = 0.50f),
    surfaceContainerHigh = NexoDeep.copy(alpha = 0.65f),
    surfaceContainerHighest = NexoDeep.copy(alpha = 0.80f)
)

/** true cuando el tema activo de NEXO es el oscuro (lo elige el apoderado en Configuración). */
val LocalNexoTemaOscuro = staticCompositionLocalOf { false }

/**
 * Sin color dinámico (Material You) a propósito: la marca se ve igual en
 * todos los teléfonos. Las barras del sistema son transparentes
 * (enableEdgeToEdge en MainActivity) y solo se ajusta el color de sus íconos.
 */
@Composable
fun NEXOTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NexoDarkColorScheme else NexoLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNexoTemaOscuro provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

/**
 * Para pantallas con fondo oscuro fijo (Splash, PIN): fuerza íconos claros
 * en las barras del sistema mientras la pantalla está visible y restaura
 * los del tema al salir.
 */
@Composable
fun IconosClarosEnBarrasDelSistema() {
    val view = LocalView.current
    val temaOscuro = LocalNexoTemaOscuro.current
    if (view.isInEditMode) return
    // Dentro de un Dialog el contexto no es la Activity: se busca hacia arriba.
    val activity = remember(view) { view.context.buscarActivity() } ?: return
    val controller = remember(activity) {
        WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    }
    // SideEffect (y no solo DisposableEffect) para correr DESPUÉS del
    // SideEffect de NEXOTheme, que si no pisaría estos valores.
    SideEffect {
        controller.isAppearanceLightStatusBars = false
        controller.isAppearanceLightNavigationBars = false
    }
    DisposableEffect(controller, temaOscuro) {
        onDispose {
            controller.isAppearanceLightStatusBars = !temaOscuro
            controller.isAppearanceLightNavigationBars = !temaOscuro
        }
    }
}

private tailrec fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}
