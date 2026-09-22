package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "TemaViewModel"

/**
 * Expone la preferencia de tema (guardada en ParentSettingsEntity) para que
 * MainActivity la lea al abrir la app, y para que ConfiguracionScreen pueda
 * cambiarla. Como ambos observan el mismo Flow de Room, un cambio hecho
 * desde Configuración se refleja de inmediato en toda la app sin reiniciar.
 */
class TemaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).parentSettingsDao()

    var temaOscuro by mutableStateOf(false)
        private set

    var errorGuardado by mutableStateOf<String?>(null)
        private set

    init {
        dao.observar()
            .onEach { config -> temaOscuro = config?.temaOscuro ?: false }
            .launchIn(viewModelScope)
    }

    fun alternarTema(activado: Boolean) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.obtener()?.let { actual ->
                    dao.guardar(actual.copy(temaOscuro = activado))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar preferencia de tema", e)
                errorGuardado = "No se pudo guardar el tema. Intenta de nuevo."
            }
        }
    }
}
