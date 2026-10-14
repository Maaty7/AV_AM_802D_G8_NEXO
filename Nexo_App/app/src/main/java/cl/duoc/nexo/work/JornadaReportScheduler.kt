package cl.duoc.nexo.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.domain.abreviacionDia
import cl.duoc.nexo.domain.parsearHora
import cl.duoc.nexo.utils.FeriadosChile
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Mantiene programado, para cada jornada activa, un trabajo de WorkManager
 * que se dispara justo después de la hora de término del PRÓXIMO día que
 * realmente corresponda (día configurado, no feriado, sin excepción
 * manual vigente) y genera el informe en Room ([JornadaReportWorker]).
 *
 * Usa [androidx.work.OneTimeWorkRequest] en vez de uno periódico: un
 * periódico de 1 día ancla cada ejecución a "la ejecución anterior + 1
 * día" (no a "la próxima vez que sea horaTermino"), así que un atraso por
 * Doze/batería se iba arrastrando y acumulando día a día. Con uno único
 * por disparo, el propio Worker reprograma el siguiente al terminar
 * (ver [JornadaReportWorker]), siempre recalculando desde el momento real.
 */
object JornadaReportScheduler {

    private fun nombreTrabajo(scheduleId: Int) = "informe_jornada_$scheduleId"

    suspend fun reprogramarTodas(context: Context, jornadas: List<ScheduleEntity>) {
        jornadas.forEach { jornada ->
            if (jornada.activo) programar(context, jornada) else cancelar(context, jornada.id)
        }
    }

    suspend fun programar(context: Context, jornada: ScheduleEntity) {
        val delay = calcularRetrasoHastaProximaEjecucionValida(context, jornada)
        val solicitud = OneTimeWorkRequestBuilder<JornadaReportWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(JornadaReportWorker.KEY_SCHEDULE_ID to jornada.id))
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            nombreTrabajo(jornada.id),
            ExistingWorkPolicy.REPLACE,
            solicitud
        )
    }

    fun cancelar(context: Context, scheduleId: Int) {
        WorkManager.getInstance(context).cancelUniqueWork(nombreTrabajo(scheduleId))
    }

    /**
     * Milisegundos hasta la próxima vez que ocurra horaTermino en un día que
     * realmente corresponda (día configurado, no feriado, sin excepción
     * vigente). Busca hacia adelante día por día, con un tope de 14 días
     * para no quedar buscando para siempre si la jornada no tiene ningún
     * día configurado.
     */
    private suspend fun calcularRetrasoHastaProximaEjecucionValida(context: Context, jornada: ScheduleEntity): Long {
        val (h, m) = parsearHora(jornada.horaTermino)
        val zona = ZoneId.systemDefault()
        val ahora = LocalDateTime.now()

        var candidato = ahora.withHour(h).withMinute(m).withSecond(0).withNano(0)
        if (!candidato.isAfter(ahora)) {
            candidato = candidato.plusDays(1)
        }

        val db = NexoDatabase.getInstance(context)

        repeat(14) {
            val fecha = candidato.toLocalDate()
            val fechaTexto = fecha.toString()
            val diaValido = jornada.dias.contains(abreviacionDia(fecha.dayOfWeek))
            val esFeriado = FeriadosChile.esFeriado(fechaTexto)
            val tieneExcepcion = db.excepcionJornadaDao().obtenerVigentes(jornada.id, fechaTexto).isNotEmpty()

            if (diaValido && !esFeriado && !tieneExcepcion) {
                return candidato.atZone(zona).toInstant().toEpochMilli() - System.currentTimeMillis()
            }
            candidato = candidato.plusDays(1)
        }

        // No se encontró un día válido en las próximas 2 semanas (ej. jornada
        // sin ningún día configurado): no la dejamos sin programar, reintenta
        // en 1 día — reprogramarTodas la va a volver a revisar la próxima vez
        // que se abra la app, y cada ejecución fallida también se reprograma.
        return candidato.atZone(zona).toInstant().toEpochMilli() - System.currentTimeMillis()
    }
}
