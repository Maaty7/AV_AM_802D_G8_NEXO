package cl.duoc.nexo.work

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cl.duoc.nexo.BuildConfig
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.prefs.SesionBackendPrefs
import cl.duoc.nexo.data.remote.NexoApiClient
import cl.duoc.nexo.data.remote.ResultadoSubida
import cl.duoc.nexo.domain.construirJsonInforme

private const val TAG = "SubirInformeWorker"

/**
 * Sube UN informe ya guardado al backend. Lo encola [SubirInformeScheduler]
 * — este Worker nunca decide si corresponde subir algo, solo lo intenta.
 */
class SubirInformeWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val reportId = inputData.getInt(KEY_REPORT_ID, -1)
        if (reportId == -1) return Result.failure()

        val token = SesionBackendPrefs.obtenerToken(applicationContext)
            ?: return Result.success() // se desvinculó la cuenta entre medio: no hay nada que subir ni reintentar.

        val informe = NexoDatabase.getInstance(applicationContext).reportDao().obtenerPorId(reportId)
            ?: return Result.success() // el informe ya no existe, nada que subir.

        val dispositivoUuid = SesionBackendPrefs.obtenerODefinirUuid(applicationContext)
        val dispositivoNombre = Build.MODEL ?: "Dispositivo Android"
        val json = construirJsonInforme(informe, dispositivoUuid, dispositivoNombre)

        return when (val resultado = NexoApiClient.subirInforme(BuildConfig.BASE_URL, token, json)) {
            is ResultadoSubida.Exito -> Result.success()
            is ResultadoSubida.NoAutorizado -> {
                Log.w(TAG, "Token rechazado por el servidor, se desvincula la cuenta")
                SesionBackendPrefs.desvincular(applicationContext)
                Result.failure()
            }
            is ResultadoSubida.ErrorDeRed -> Result.retry()
            is ResultadoSubida.ErrorServidor -> {
                Log.w(TAG, "El servidor devolvió ${resultado.codigo} al subir el informe $reportId")
                Result.retry()
            }
        }
    }

    companion object {
        const val KEY_REPORT_ID = "report_id"
    }
}
