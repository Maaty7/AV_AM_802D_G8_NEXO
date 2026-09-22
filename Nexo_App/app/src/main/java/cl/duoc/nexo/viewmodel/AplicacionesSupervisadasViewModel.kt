package cl.duoc.nexo.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.SupervisedAppEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class AppSupervisable(
    val packageName: String,
    val nombre: String,
    val supervisada: Boolean
)

class AplicacionesSupervisadasViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).supervisedAppDao()
    private val appsInstaladas = obtenerAppsInstaladas()

    var apps by mutableStateOf<List<AppSupervisable>>(emptyList())
        private set

    init {
        dao.observarTodas()
            .onEach { guardadas ->
                val estadoGuardado = guardadas.associateBy({ it.packageName }, { it.supervisada })
                apps = appsInstaladas
                    .map { (pkg, nombre) ->
                        // Por defecto una app se considera supervisada hasta que el apoderado la desactive.
                        AppSupervisable(pkg, nombre, supervisada = estadoGuardado[pkg] ?: true)
                    }
                    .sortedBy { it.nombre.lowercase() }
            }
            .launchIn(viewModelScope)
    }

    fun alternar(packageName: String, supervisada: Boolean) {
        viewModelScope.launch {
            dao.guardar(SupervisedAppEntity(packageName = packageName, supervisada = supervisada))
        }
    }

    private fun obtenerAppsInstaladas(): List<Pair<String, String>> {
        val context = getApplication<Application>()
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }
            .filterNot { (pkg, _) -> pkg == context.packageName }
            .distinctBy { it.first }
    }
}
