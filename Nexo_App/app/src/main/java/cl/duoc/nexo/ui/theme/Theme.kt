package cl.duoc.nexo.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val NexoLightColorScheme = lightColorScheme(
    primary = NexoDeep,
    onPrimary = Color.White,
    secondary = NexoTeal,
    onSecondary = Color.White,
    background = Color.White,
    onBackground = NexoInk,
    surface = Color.White,
    onSurface = NexoInk,
    surfaceVariant = NexoIce,
    onSurfaceVariant = NexoMute,
    outline = NexoBorder,
    error = NexoCoral,
    onError = Color.White
)

private val NexoDarkColorScheme = darkColorScheme(
    primary = NexoTeal,
    onPrimary = Color.White,
    secondary = NexoDeep,
    onSecondary = Color.White,
    background = NexoMidnight,
    onBackground = Color.White,
    surface = NexoMidnight,
    onSurface = Color.White,
    surfaceVariant = NexoIce,
    onSurfaceVariant = NexoMute,
    outline = NexoBorder,
    error = NexoCoral,
    onError = Color.White
)

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
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}