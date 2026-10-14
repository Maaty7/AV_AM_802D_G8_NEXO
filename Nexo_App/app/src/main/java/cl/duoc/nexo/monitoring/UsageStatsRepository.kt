package cl.duoc.nexo.monitoring

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.util.Log
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.FranjaUso
import cl.duoc.nexo.domain.RangoJornada
import cl.duoc.nexo.domain.calcularRangoJornada
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

// TEMPORAL — diagnóstico de informes vacíos, quitar cuando se confirme el arreglo.
private const val DEBUG_TAG = "NEXO_DEBUG"
private fun formatearFechaLegible(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

/** Resultado de consultar el uso de una jornada: distingue "sin uso real" de "rango inválido" (nunca se debe guardar un informe por esto último). */
sealed interface ResultadoConsultaUso {
    data class Exito(val apps: List<AppUsageInfo>) : ResultadoConsultaUso
    data object RangoInvalido : ResultadoConsultaUso
}

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

    /**
     * [fechaBase] es el día al que pertenece la jornada (el día en que
     * TERMINA, no necesariamente "hoy" si se llama desde un contexto que
     * puede ejecutarse atrasado — ver [cl.duoc.nexo.domain.calcularFechaObjetivo]).
     * El camino en vivo (dashboard) no necesita pasarlo: por defecto es
     * "hoy", que es siempre correcto mientras se está viendo en el momento.
     */
    suspend fun obtenerUsoEnRango(
        context: Context,
        horaInicio: String,
        horaTermino: String,
        fechaBase: LocalDate = LocalDate.now()
    ): ResultadoConsultaUso = withContext(Dispatchers.IO) {
        when (val rango = calcularRangoJornada(fechaBase, horaInicio, horaTermino)) {
            is RangoJornada.Invalido -> {
                Log.w(
                    DEBUG_TAG,
                    "[UsageStatsRepository] obtenerUsoEnRango RANGO INVALIDO fechaBase=$fechaBase " +
                        "horaInicio=$horaInicio horaTermino=$horaTermino"
                )
                ResultadoConsultaUso.RangoInvalido
            }
            is RangoJornada.Valido -> {
                Log.d(
                    DEBUG_TAG,
                    "[UsageStatsRepository] obtenerUsoEnRango fechaBase=$fechaBase horaInicio=$horaInicio horaTermino=$horaTermino " +
                        "-> inicio=${formatearFechaLegible(rango.inicioMillis)} fin=${formatearFechaLegible(rango.finMillis)}"
                )
                ResultadoConsultaUso.Exito(consultar(context, rango.inicioMillis, rango.finMillis))
            }
        }
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
        minutosPorFranja: Int,
        fechaBase: LocalDate = LocalDate.now()
    ): List<FranjaUso> = withContext(Dispatchers.IO) {
        val rango = calcularRangoJornada(fechaBase, horaInicio, horaTermino)
        if (rango !is RangoJornada.Valido) return@withContext emptyList()
        val inicio = rango.inicioMillis
        val fin = rango.finMillis

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

    private suspend fun consultar(context: Context, startTime: Long, endTime: Long): List<AppUsageInfo> {
        val totales = calcularTiemposViaEventos(context, startTime, endTime)
        Log.d(DEBUG_TAG, "[UsageStatsRepository] consultar: packages con tiempo>0 antes de filtrar sistema = ${totales.count { it.value > 0 }} de ${totales.size} totales")
        val packageManager = context.packageManager

        val resultado = totales.entries
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

        Log.d(DEBUG_TAG, "[UsageStatsRepository] consultar: apps finales tras filtro de apps de sistema = ${resultado.size}")
        return resultado
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
        var eventosLeidos = 0

        while (eventos.hasNextEvent()) {
            eventos.getNextEvent(evento)
            eventosLeidos++
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

        Log.d(
            DEBUG_TAG,
            "[UsageStatsRepository] calcularSesiones rango=${formatearFechaLegible(startTime)}..${formatearFechaLegible(endTime)} " +
                "eventosLeidos=$eventosLeidos sesionesCrudas=${sesiones.size} appsAbiertasSinCerrar=${tiemposInicio.size}"
        )

        val noSupervisadas = NexoDatabase.getInstance(context).supervisedAppDao()
            .obtenerPackagesNoSupervisados()
            .toSet()

        val sesionesFiltradas = sesiones.filterNot { it.packageName in noSupervisadas }
        Log.d(
            DEBUG_TAG,
            "[UsageStatsRepository] calcularSesiones appsNoSupervisadas=${noSupervisadas.size} sesionesTrasFiltroSupervision=${sesionesFiltradas.size}"
        )

        return sesionesFiltradas
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
