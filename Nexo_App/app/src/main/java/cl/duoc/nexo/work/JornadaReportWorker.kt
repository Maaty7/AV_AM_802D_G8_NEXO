package cl.duoc.nexo.work

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.monitoring.UsageStatsRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "JornadaReportWorker"

/**
 * Se ejecuta en segundo plano cuando termina una jornada configurada
 * (programado por [JornadaReportScheduler]) y guarda un informe en Room con
 * el uso real de esa jornada, sin que el apoderado necesite abrir NEXO.
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
                    fecha = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    horaInicio = jornada.horaInicio,
                    horaTermino = jornada.horaTermino,
                    minutosTotales = usos.sumOf { it.totalTimeMinutes },
                    minutosEducacion = porCategoria["Educación"] ?: 0L,
                    minutosComunicacion = porCategoria["Comunicación"] ?: 0L,
                    minutosRedesSociales = porCategoria["Redes Sociales"] ?: 0L,
                    minutosEntretenimiento = porCategoria["Entretenimiento"] ?: 0L,
                    minutosJuegos = porCategoria["Juegos"] ?: 0L,
                    minutosOtros = porCategoria["Otros"] ?: 0L,
                    generadoEn = System.currentTimeMillis()
                )
            )

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error al generar el informe automático de la jornada $scheduleId", e)
            Result.failure()
        }
    }

    companion object {
        const val KEY_SCHEDULE_ID = "schedule_id"
    }
}
