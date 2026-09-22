package cl.duoc.nexo.domain

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val totalTimeMs: Long,
    val categoria: String = "Otros"
) {
    val totalTimeMinutes: Long
        get() = totalTimeMs / 1000 / 60
}