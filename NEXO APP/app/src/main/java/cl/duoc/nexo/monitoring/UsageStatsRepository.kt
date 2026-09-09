package cl.duoc.nexo.monitoring

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import cl.duoc.nexo.domain.AppUsageInfo
import java.util.Calendar

/**
 * Consulta UsageStatsManager para obtener el tiempo en primer plano de cada app
 * durante el día en curso. Es la prueba de factibilidad técnica #1 del proyecto:
 * confirmar que la API entrega datos reales y usables en un dispositivo físico.
 *
 * No lee contenido de las apps (mensajes, pantallas, etc.), solo metadatos
 * agregados que el propio Android expone (paquete + duración total de uso).
 */
class UsageStatsRepository(private val context: Context) {

    /**
     * Retorna el uso de hoy (desde las 00:00 hasta ahora) por aplicación,
     * excluyendo apps de sistema sin nombre visible y ordenado de mayor a
     * menor tiempo de uso. Requiere que [UsageAccessPermission.isGranted]
     * sea true; si no, retorna lista vacía.
     */
    fun getTodayUsage(): List<AppUsageInfo> {
        if (!UsageAccessPermission.isGranted(context)) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = Calendar.getInstance().apply {
            timeInMillis = endTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // queryAndAggregateUsageStats junta el uso por paquete en un solo rango,
        // a diferencia de queryUsageStats que puede devolver varias entradas
        // fragmentadas por el mismo paquete.
        val statsByPackage = usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        val packageManager = context.packageManager

        return statsByPackage.values
            .filter { it.totalTimeInForeground > 0 }
            .mapNotNull { usageStats ->
                val appName = resolveAppName(packageManager, usageStats.packageName) ?: return@mapNotNull null
                AppUsageInfo(
                    packageName = usageStats.packageName,
                    appName = appName,
                    totalTimeForegroundMs = usageStats.totalTimeInForeground
                )
            }
            .sortedByDescending { it.totalTimeForegroundMs }
    }

    /**
     * Resuelve el nombre visible de una app a partir de su packageName.
     * Retorna null para paquetes de sistema sin ícono/launcher (para no llenar
     * el listado con procesos internos de Android que no son "apps" para el usuario).
     */
    private fun resolveAppName(packageManager: PackageManager, packageName: String): String? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            if (!isUserFacingApp(appInfo)) return null
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isUserFacingApp(appInfo: ApplicationInfo): Boolean {
        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val wasUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        return !isSystemApp || wasUpdated
    }
}
