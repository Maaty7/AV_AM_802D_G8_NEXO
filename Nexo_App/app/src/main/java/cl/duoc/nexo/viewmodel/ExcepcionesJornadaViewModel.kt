package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ExcepcionJornadaEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "ExcepcionesJornadaViewModel"

class ExcepcionesJornadaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).excepcionJornadaDao()

    var excepciones by mutableStateOf<List<ExcepcionJornadaEntity>>(emptyList())
        private set

    var errorGuardado by mutableStateOf<String?>(null)
        private set

    init {
        dao.observarTodas()
            .onEach { lista -> excepciones = lista }
            .launchIn(viewModelScope)
    }

    fun agregar(fechaInicio: String, fechaTermino: String, motivo: String) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.insertar(
                    ExcepcionJornadaEntity(
                        fechaInicio = fechaInicio,
                        fechaTermino = fechaTermino,
                        motivo = motivo.ifBlank { "Día libre" }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar la excepción", e)
                errorGuardado = "No se pudo guardar. Intenta de nuevo."
            }
        }
    }

    fun eliminar(excepcion: ExcepcionJornadaEntity) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.eliminar(excepcion)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar la excepción", e)
                errorGuardado = "No se pudo eliminar. Intenta de nuevo."
            }
        }
    }
}
