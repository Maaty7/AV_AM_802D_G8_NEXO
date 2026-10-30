package cl.duoc.nexo.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

/**
 * Encola la subida de UN informe al backend — solo si el apoderado tiene
 * una cuenta vinculada (eso ya lo decide quien llama a [encolar], ver
 * [JornadaReportWorker]). Cada subida es independiente: no necesita nombre
 * único como [JornadaReportScheduler], da lo mismo si se encolan varias a
 * la vez (ej. una automática y otra de "Sincronizar ahora"), el backend es
 * idempotente.
 */
object SubirInformeScheduler {

    fun encolar(context: Context, reportId: Int) {
        val restricciones = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val solicitud = OneTimeWorkRequestBuilder<SubirInformeWorker>()
            .setConstraints(restricciones)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, TimeUnit.MINUTES.toMillis(1), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(SubirInformeWorker.KEY_REPORT_ID to reportId))
            .build()

        WorkManager.getInstance(context).enqueue(solicitud)
    }
}
