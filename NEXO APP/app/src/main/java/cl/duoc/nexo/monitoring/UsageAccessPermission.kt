package cl.duoc.nexo.monitoring

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

/**
 * Verifica y solicita el permiso especial "Usage Access" (PACKAGE_USAGE_STATS).
 *
 * Este permiso NO se puede pedir con un diálogo estándar (ActivityResultContracts):
 * Android obliga a que el usuario lo otorgue manualmente desde Ajustes. Por eso
 * la app solo puede detectar si ya fue otorgado y, si no, llevar al usuario a la
 * pantalla de Ajustes correspondiente.
 */
object UsageAccessPermission {

    /**
     * Retorna true si el usuario ya otorgó el acceso a estadísticas de uso.
     * Se consulta vía AppOpsManager porque checkSelfPermission() no sirve para
     * permisos especiales como este.
     */
    fun isGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            @Suppress("DEPRECATION")
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Abre la pantalla de Ajustes de Android donde el usuario otorga el acceso.
     * Se intenta llevarlo directo a la fila de esta app; si el dispositivo no
     * soporta ese extra, cae de vuelta a la lista general de Usage Access.
     */
    fun openSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            val fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        }
    }
}
