package cl.duoc.nexo.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import cl.duoc.nexo.ui.theme.LocalNexoTemaOscuro
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoMist
import cl.duoc.nexo.ui.theme.NexoNeon
import cl.duoc.nexo.ui.theme.NexoNight
import cl.duoc.nexo.ui.theme.NexoSky
import cl.duoc.nexo.ui.theme.NexoTextoSecundario

private data class NexoTab(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val tabs = listOf(
    NexoTab(NexoDestinations.HOME, "Inicio", Icons.Filled.Home),
    NexoTab(NexoDestinations.ESTADISTICAS, "Estadísticas", Icons.Filled.BarChart),
    NexoTab(NexoDestinations.HORARIOS, "Horarios", Icons.Filled.Schedule),
    NexoTab(NexoDestinations.CONFIGURACION, "Ajustes", Icons.Filled.Settings)
)

/**
 * Barra inferior flotante (cápsula de vidrio). El indicador del tab activo
 * es una píldora neón que se desliza con spring entre pestañas; el ícono y
 * el texto del tab activo van en Deep sobre el neón (contraste AA).
 */
@Composable
fun NexoBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val indiceActual = tabs.indexOfFirst { it.route == currentRoute }

    val oscuro = LocalNexoTemaOscuro.current
    val tinte = if (oscuro) NexoNight.copy(alpha = 0.88f) else NexoMist.copy(alpha = 0.92f)
    val borde = if (oscuro) NexoSky.copy(alpha = 0.25f) else NexoDeep.copy(alpha = 0.10f)

    val indicador by animateFloatAsState(
        targetValue = indiceActual.coerceAtLeast(0).toFloat(),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "indicadorTab"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(16.dp, CircleShape, ambientColor = NexoDeep.copy(alpha = 0.25f), spotColor = NexoDeep.copy(alpha = 0.30f))
                .clip(CircleShape)
                .drawBehind {
                    drawRect(tinte)
                    drawRect(
                        Brush.verticalGradient(
                            listOf(if (oscuro) NexoSky.copy(alpha = 0.08f) else NexoMist, Color.Transparent),
                            endY = size.height * 0.6f
                        )
                    )
                    if (indiceActual >= 0) {
                        val inset = 6.dp.toPx()
                        val anchoTab = size.width / tabs.size
                        drawRoundRect(
                            color = NexoNeon,
                            topLeft = Offset(indicador * anchoTab + inset, inset),
                            size = Size(anchoTab - inset * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(size.height)
                        )
                    }
                }
                .border(1.dp, borde, CircleShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { indice, tab ->
                val seleccionado = indice == indiceActual
                val colorContenido = if (seleccionado) NexoDeep else NexoTextoSecundario
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .semantics { selected = seleccionado }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab
                        ) {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(NexoDestinations.HOME) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(tab.icon, contentDescription = null, tint = colorContenido, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                        color = colorContenido,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
