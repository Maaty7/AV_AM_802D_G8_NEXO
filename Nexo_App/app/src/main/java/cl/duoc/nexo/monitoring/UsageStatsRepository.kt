package cl.duoc.nexo.monitoring

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import cl.duoc.nexo.domain.AppUsageInfo
import java.util.Calendar

object UsageStatsRepository {

    fun obtenerUsoDeHoy(context: Context): List<AppUsageInfo> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        val packageManager = context.packageManager

        return statsList
            .filter { it.totalTimeInForeground > 0 }
            .filterNot { esAppDeSistema(it.packageName, packageManager) }
            .map { stat ->
                AppUsageInfo(
                    packageName = stat.packageName,
                    appName = obtenerNombreApp(stat.packageName, packageManager),
                    totalTimeMs = stat.totalTimeInForeground
                )
            }
            .sortedByDescending { it.totalTimeMs }
    }

    private fun obtenerNombreApp(packageName: String, packageManager: PackageManager): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun esAppDeSistema(packageName: String, packageManager: PackageManager): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: PackageManager.NameNotFoundException) {
            true
        }
    }
}