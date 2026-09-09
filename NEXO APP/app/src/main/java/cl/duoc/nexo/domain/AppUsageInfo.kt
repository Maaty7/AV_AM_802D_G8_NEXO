package cl.duoc.nexo.domain

/**
 * Representa el uso de una aplicación durante el período consultado.
 * Modelo de dominio simple para la prueba de factibilidad de UsageStatsManager
 * (aún no corresponde a la entidad Room "AppUsage" definitiva del MVP).
 */
data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeForegroundMs: Long
)
