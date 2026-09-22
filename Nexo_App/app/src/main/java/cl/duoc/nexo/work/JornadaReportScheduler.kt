package cl.duoc.nexo.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Mantiene programado, para cada jornada activa, un trabajo diario de
 * WorkManager que se dispara justo después de la hora de término y genera
 * el informe en Room ([JornadaReportWorker]).
 */
object JornadaReportScheduler {

    private fun nombreTrabajo(scheduleId: Int) = "informe_jornada_$scheduleId"

    fun reprogramarTodas(context: Context, jornadas: List<ScheduleEntity>) {
        jornadas.forEach { jornada ->
            if (jornada.activo) programar(context, jornada) else cancelar(context, jornada.id)
        }
    }

    private fun programar(context: Context, jornada: ScheduleEntity) {
        val solicitud = PeriodicWorkRequestBuilder<JornadaReportWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(calcularRetrasoHastaTermino(jornada.horaTermino), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(JornadaReportWorker.KEY_SCHEDULE_ID to jornada.id))
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            nombreTrabajo(jornada.id),
            ExistingPeriodicWorkPolicy.UPDATE,
            solicitud
        )
    }

    private fun cancelar(context: Context, scheduleId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(nombreTrabajo(scheduleId))
    }

    /** Milisegundos hasta la próxima vez que ocurra horaTermino (hoy o mañana). */
    private fun calcularRetrasoHastaTermino(horaTermino: String): Long {
        val partes = horaTermino.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0

        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (objetivo.timeInMillis <= System.currentTimeMillis()) {
            objetivo.add(Calendar.DAY_OF_YEAR, 1)
        }
        return objetivo.timeInMillis - System.currentTimeMillis()
    }
}
