package cl.duoc.nexo.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.domain.serializarAppsUsadas
import cl.duoc.nexo.monitoring.UsageStatsRepository
import cl.duoc.nexo.utils.FeriadosChile
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val TAG = "JornadaReportWorker"

/**
 * Se ejecuta en segundo plano cuando termina una jornada configurada
 * (programado por [JornadaReportScheduler]) y guarda un informe en Room con
 * el uso real de esa jornada, sin que el apoderado necesite abrir NEXO.
 *
 * Antes de generar el informe se revisan, en orden, 3 condiciones — si
 * cualquiera falla, se omite el informe de hoy (no es un error, es un
 * no-op correcto):
 * 1. ¿Hoy es uno de los días configurados para esta jornada?
 * 2. ¿Hoy NO es feriado en Chile?
 * 3. ¿Hoy NO cae dentro de ninguna excepción manual guardada (vacaciones, paro, etc.)?
 */
class JornadaReportWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val scheduleId = inputData.getInt(KEY_SCHEDULE_ID, -1)
        if (scheduleId == -1) return Result.failure()

        return try {
            val db = NexoDatabase.getInstance(applicationContext)
            val jornada = db.scheduleDao().obtenerPorId(scheduleId) ?: return Result.failure()
            val fechaHoy = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            if (!jornada.dias.contains(abreviacionDiaActual())) {
                Log.d(TAG, "Hoy no corresponde a la jornada ${jornada.id} (días: ${jornada.dias}), se omite el informe")
                return Result.success()
            }
            if (FeriadosChile.esFeriado(fechaHoy)) {
                Log.d(TAG, "Hoy ($fechaHoy) es feriado, se omite el informe de la jornada ${jornada.id}")
                return Result.success()
            }
            val excepciones = db.excepcionJornadaDao().obtenerVigentes(jornada.id, fechaHoy)
            if (excepciones.isNotEmpty()) {
                Log.d(TAG, "Hoy ($fechaHoy) cae en una excepción manual, se omite el informe de la jornada ${jornada.id}")
                return Result.success()
            }

            val usos = UsageStatsRepository.obtenerUsoEnRango(
                applicationContext,
                jornada.horaInicio,
                jornada.horaTermino
            )
            val porCategoria = usos.groupBy { it.categoria }
                .mapValues { (_, apps) -> apps.sumOf { it.totalTimeMinutes } }

            db.reportDao().insertar(
                ReportEntity(
                    scheduleId = jornada.id,
                    nombreJornada = jornada.nombre,
                    fecha = fechaHoy,
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

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar el informe automático de la jornada $scheduleId", e)
            Result.failure()
        }
    }

    /** Convierte el día de la semana de hoy a la misma abreviación de 2 letras que usa ScheduleEntity.dias (LU/MA/MI/JU/VI/SA/DO). */
    private fun abreviacionDiaActual(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "LU"
            Calendar.TUESDAY -> "MA"
            Calendar.WEDNESDAY -> "MI"
            Calendar.THURSDAY -> "JU"
            Calendar.FRIDAY -> "VI"
            Calendar.SATURDAY -> "SA"
            else -> "DO"
        }
    }

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
    }
}
