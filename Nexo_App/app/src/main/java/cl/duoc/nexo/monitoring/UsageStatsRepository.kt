package cl.duoc.nexo.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.FranjaUso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

object UsageStatsRepository {

    suspend fun obtenerUsoDeHoy(context: Context): List<AppUsageInfo> = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        consultar(context, startTime, endTime)
    }

    suspend fun obtenerUsoEnRango(context: Context, horaInicio: String, horaTermino: String): List<AppUsageInfo> =
        withContext(Dispatchers.IO) {
            val (inicio, fin) = calcularRangoJornada(horaInicio, horaTermino)
            consultar(context, inicio, fin)
        }

    /**
     * Divide la jornada en bloques de tamaño fijo ([minutosPorFranja]) y suma,
     * para cada bloque, cuántos minutos hubo alguna app en primer plano — para
     * graficar cómo se distribuyó el uso del teléfono a lo largo de la jornada.
     */
    suspend fun obtenerUsoPorFranjas(
        context: Context,
        horaInicio: String,
        horaTermino: String,
        minutosPorFranja: Int
    ): List<FranjaUso> = withContext(Dispatchers.IO) {
        val (inicio, fin) = calcularRangoJornada(horaInicio, horaTermino)
        if (fin <= inicio) return@withContext emptyList()

        val franjaMs = minutosPorFranja * 60_000L
        val cantidadFranjas = (((fin - inicio) - 1) / franjaMs + 1).toInt()
        val minutosPorIndice = LongArray(cantidadFranjas)

        calcularSesiones(context, inicio, fin).forEach { sesion ->
            var actual = sesion.inicio.coerceAtLeast(inicio)
            val limite = sesion.fin.coerceAtMost(fin)
            while (actual < limite) {
                val indice = ((actual - inicio) / franjaMs).toInt().coerceIn(0, cantidadFranjas - 1)
                val finFranja = inicio + (indice + 1) * franjaMs
                val corte = minOf(limite, finFranja)
                minutosPorIndice[indice] += (corte - actual) / 60_000
                actual = corte
            }
        }

        (0 until cantidadFranjas).map { i ->
            val horaFranja = Calendar.getInstance().apply { timeInMillis = inicio + i * franjaMs }
            FranjaUso(
                etiqueta = "%02d:%02d".format(horaFranja.get(Calendar.HOUR_OF_DAY), horaFranja.get(Calendar.MINUTE)),
                minutos = minutosPorIndice[i]
            )
        }
    }

    private fun calcularRangoJornada(horaInicio: String, horaTermino: String): Pair<Long, Long> {
        val (hInicio, mInicio) = parsearHora(horaInicio)
        val (hTermino, mTermino) = parsearHora(horaTermino)

        val calendarInicio = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hInicio)
            set(Calendar.MINUTE, mInicio)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val calendarTermino = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hTermino)
            set(Calendar.MINUTE, mTermino)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val ahora = System.currentTimeMillis()
        val fin = minOf(calendarTermino.timeInMillis, ahora)

        return calendarInicio.timeInMillis to fin
    }

    private fun parsearHora(hora: String): Pair<Int, Int> {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return h to m
    }

    private suspend fun consultar(context: Context, startTime: Long, endTime: Long): List<AppUsageInfo> {
        if (endTime <= startTime) return emptyList()

        val totales = calcularTiemposViaEventos(context, startTime, endTime)
        val packageManager = context.packageManager

        return totales.entries
            .filter { it.value > 0 }
            .filterNot { esAppDeSistema(it.key, packageManager) }
            .map { (pkg, tiempoMs) ->
                AppUsageInfo(
                    packageName = pkg,
                    appName = obtenerNombreApp(pkg, packageManager),
                    totalTimeMs = tiempoMs,
                    categoria = AppCategoryClassifier.clasificar(pkg, context)
                )
            }
            .sortedByDescending { it.totalTimeMs }
    }

    private data class Sesion(val packageName: String, val inicio: Long, val fin: Long)

    /**
     * Lee el registro crudo de eventos (queryEvents) y arma la lista de
     * sesiones en primer plano (paquete + inicio + fin), en vez de depender
     * de los resúmenes pre-calculados de queryUsageStats (que pueden estar
     * desactualizados para el día en curso). Excluye las apps que el
     * apoderado desmarcó en "Aplicaciones supervisadas" — este es el único
     * punto donde se calculan las sesiones (tanto el total por app como la
     * línea de tiempo por franjas pasan por acá), así que el filtro queda
     * aplicado en todos lados a la vez: Inicio, Estadísticas e Informes.
     */
    private suspend fun calcularSesiones(context: Context, startTime: Long, endTime: Long): List<Sesion> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val eventos = usageStatsManager.queryEvents(startTime, endTime)

        val tiemposInicio = mutableMapOf<String, Long>()
        val sesiones = mutableListOf<Sesion>()
        val evento = UsageEvents.Event()

        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            when (evento.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    tiemposInicio[evento.packageName] = evento.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val inicio = tiemposInicio.remove(evento.packageName)
                    if (inicio != null && evento.timeStamp > inicio) {
                        sesiones.add(Sesion(evento.packageName, inicio, evento.timeStamp))
                    }
                }
            }
        }

        // Si alguna app quedó abierta sin haberse cerrado dentro del rango,
        // contamos su tiempo hasta el final del rango consultado.
        tiemposInicio.forEach { (pkg, inicio) ->
            if (endTime > inicio) {
                sesiones.add(Sesion(pkg, inicio, endTime))
            }
        }

        val noSupervisadas = NexoDatabase.getInstance(context).supervisedAppDao()
            .obtenerPackagesNoSupervisados()
            .toSet()

        return sesiones.filterNot { it.packageName in noSupervisadas }
    }

    private suspend fun calcularTiemposViaEventos(context: Context, startTime: Long, endTime: Long): Map<String, Long> {
        return calcularSesiones(context, startTime, endTime)
            .groupBy { it.packageName }
            .mapValues { (_, sesiones) -> sesiones.sumOf { it.fin - it.inicio } }
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
