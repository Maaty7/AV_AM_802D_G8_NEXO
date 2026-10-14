package cl.duoc.nexo.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.domain.abreviacionDia
import cl.duoc.nexo.domain.calcularFechaObjetivo
import cl.duoc.nexo.domain.serializarAppsUsadas
import cl.duoc.nexo.monitoring.ResultadoConsultaUso
import cl.duoc.nexo.monitoring.UsageAccessPermission
import cl.duoc.nexo.monitoring.UsageStatsRepository
import cl.duoc.nexo.utils.FeriadosChile

private const val TAG = "JornadaReportWorker"

/**
 * Se ejecuta en segundo plano (programado por [JornadaReportScheduler]) y
 * guarda un informe en Room con el uso real de una jornada, sin que el
 * apoderado necesite abrir NEXO.
 *
 * La fecha a informar NO es "hoy al momento de ejecutar" — es
 * [calcularFechaObjetivo]: la ocurrencia más reciente de horaTermino que ya
 * pasó. Si el Worker corre atrasado (Doze) y cae después de medianoche,
 * esa fecha objetivo es el día anterior, no el día de hoy — así el rango
 * que se le pide a [UsageStatsRepository] nunca queda invertido.
 *
 * Antes de generar el informe se revisan, en orden, estas condiciones —
 * si cualquiera falla, se omite el informe de esa fecha (no es un error):
 * 1. ¿Esa fecha es uno de los días configurados para esta jornada?
 * 2. ¿Esa fecha NO es feriado en Chile?
 * 3. ¿Esa fecha NO cae dentro de ninguna excepción manual guardada?
 * 4. ¿No existe ya un informe para esta jornada en esa fecha?
 * 5. ¿Está otorgado el permiso de Usage Access?
 * 6. ¿El rango calculado es válido (no solo "sin uso")?
 *
 * Al terminar (se genere o no el informe), SIEMPRE reprograma la próxima
 * ejecución en un `finally` — si no, un salto (feriado, permiso, etc.)
 * dejaría la jornada sin ningún trabajo pendiente.
 */
class JornadaReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getInt(KEY_SCHEDULE_ID, -1)
        if (scheduleId == -1) return Result.failure()

        return try {
            generarInformeSiCorresponde(scheduleId)
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar el informe automático de la jornada $scheduleId", e)
            Result.failure()
        } finally {
            reprogramarProximaEjecucion(scheduleId)
        }
    }

    private suspend fun generarInformeSiCorresponde(scheduleId: Int): Result {
        val db = NexoDatabase.getInstance(applicationContext)
        val jornada = db.scheduleDao().obtenerPorId(scheduleId) ?: return Result.failure()

        val fechaObjetivo = calcularFechaObjetivo(jornada.horaTermino)
        val fechaTexto = fechaObjetivo.toString()

        if (!jornada.dias.contains(abreviacionDia(fechaObjetivo.dayOfWeek))) {
            return Result.success()
        }
        if (FeriadosChile.esFeriado(fechaTexto)) {
            return Result.success()
        }
        if (db.excepcionJornadaDao().obtenerVigentes(jornada.id, fechaTexto).isNotEmpty()) {
            return Result.success()
        }
        if (db.reportDao().contarPorJornadaYFecha(jornada.id, fechaTexto) > 0) {
            return Result.success()
        }
        if (!UsageAccessPermission.isGranted(applicationContext)) {
            return Result.success()
        }

        val resultado = UsageStatsRepository.obtenerUsoEnRango(
            applicationContext,
            jornada.horaInicio,
            jornada.horaTermino,
            fechaObjetivo
        )

        val usos = when (resultado) {
            is ResultadoConsultaUso.RangoInvalido -> return Result.success()
            is ResultadoConsultaUso.Exito -> resultado.apps
        }

        val porCategoria = usos.groupBy { it.categoria }
            .mapValues { (_, apps) -> apps.sumOf { it.totalTimeMinutes } }

        db.reportDao().insertar(
            ReportEntity(
                scheduleId = jornada.id,
                nombreJornada = jornada.nombre,
                fecha = fechaTexto,
                horaInicio = jornada.horaInicio,
                horaTermino = jornada.horaTermino,
                dias = jornada.dias,
                minutosTotales = usos.sumOf { it.totalTimeMinutes },
                minutosEducacion = porCategoria["Educación"] ?: 0L,
                minutosComunicacion = porCategoria["Comunicación"] ?: 0L,
                minutosRedesSociales = porCategoria["Redes Sociales"] ?: 0L,
                minutosEntretenimiento = porCategoria["Entretenimiento"] ?: 0L,
                minutosJuegos = porCategoria["Juegos"] ?: 0L,
                minutosOtros = porCategoria["Otros"] ?: 0L,
                appsUsadas = serializarAppsUsadas(usos),
                generadoEn = System.currentTimeMillis()
            )
        )

        return Result.success()
    }

    /** Siempre deja programada la próxima ejecución, sin importar si esta terminó generando informe, saltándolo, o con error. */
    private suspend fun reprogramarProximaEjecucion(scheduleId: Int) {
        try {
            val db = NexoDatabase.getInstance(applicationContext)
            val jornada = db.scheduleDao().obtenerPorId(scheduleId) ?: return
            if (jornada.activo) {
                JornadaReportScheduler.programar(applicationContext, jornada)
            } else {
                JornadaReportScheduler.cancelar(applicationContext, jornada.id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al reprogramar la próxima ejecución de la jornada $scheduleId", e)
        }
    }

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
    }
}
