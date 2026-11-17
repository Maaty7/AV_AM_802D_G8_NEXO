package cl.duoc.nexo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import cl.duoc.nexo.R

/**
 * Inter (SIL Open Font License 1.1) para texto y números.
 * Incluida en res/font, así no depende de las fuentes del teléfono.
 */
val NexoFuenteTexto = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold)
)

/**
 * Inter Display para títulos: trazo más apretado y aire tecnológico.
 * Ojo: el peso "Normal" usa el archivo SemiBold a propósito, para que un
 * título sin fontWeight explícito ya se vea con jerarquía de título.
 */
val NexoFuenteTitulos = FontFamily(
    Font(R.font.inter_display_semibold, FontWeight.Normal),
    Font(R.font.inter_display_semibold, FontWeight.Medium),
    Font(R.font.inter_display_semibold, FontWeight.SemiBold),
    Font(R.font.inter_display_bold, FontWeight.Bold),
    Font(R.font.inter_display_extrabold, FontWeight.ExtraBold)
)

/** Números tabulares: cada dígito ocupa lo mismo, así los contadores no "bailan". */
const val NUMEROS_TABULARES = "tnum"

private fun titulo(size: Int, line: Int, weight: FontWeight = FontWeight.Bold, tracking: Double = -0.01) =
    TextStyle(
        fontFamily = NexoFuenteTitulos,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.em
    )

private fun texto(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, tracking: Double = 0.0) =
    TextStyle(
        fontFamily = NexoFuenteTexto,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = tracking.em
    )

val Typography = Typography(
    // Display: números grandes de las tarjetas y el logo de texto
    displayLarge = titulo(52, 56, FontWeight.ExtraBold, -0.02).copy(fontFeatureSettings = NUMEROS_TABULARES),
    displayMedium = titulo(40, 44, FontWeight.ExtraBold, -0.02).copy(fontFeatureSettings = NUMEROS_TABULARES),
    displaySmall = titulo(32, 36, FontWeight.Bold, -0.02).copy(fontFeatureSettings = NUMEROS_TABULARES),
    // Headline: título de cada pantalla
    headlineLarge = titulo(30, 36),
    headlineMedium = titulo(26, 32),
    headlineSmall = titulo(24, 28),
    // Title: títulos de tarjetas y secciones
    titleLarge = titulo(20, 24, FontWeight.SemiBold),
    titleMedium = texto(16, 20, FontWeight.SemiBold),
    titleSmall = texto(14, 20, FontWeight.SemiBold),
    // Body
    bodyLarge = texto(16, 24),
    bodyMedium = texto(14, 20),
    bodySmall = texto(12, 16),
    // Label: botones, chips, encabezados de sección (MAYÚSCULAS)
    labelLarge = texto(14, 20, FontWeight.SemiBold, 0.01),
    labelMedium = texto(12, 16, FontWeight.SemiBold, 0.06),
    labelSmall = texto(11, 16, FontWeight.Medium, 0.04)
)
