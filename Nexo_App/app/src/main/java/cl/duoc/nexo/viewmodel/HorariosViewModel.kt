package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.work.JornadaReportScheduler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "HorariosViewModel"

class HorariosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).scheduleDao()

    var jornadas by mutableStateOf<List<ScheduleEntity>>(emptyList())
        private set

    var errorGuardado by mutableStateOf<String?>(null)
        private set

    init {
        dao.observarTodas()
            .onEach { lista -> jornadas = lista }
            .launchIn(viewModelScope)
    }

    fun agregarJornada(nombre: String, inicio: String, termino: String, dias: String) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.insertar(
                    ScheduleEntity(
                        nombre = nombre.ifBlank { "Nueva jornada" },
                        horaInicio = inicio.ifBlank { "00:00" },
                        horaTermino = termino.ifBlank { "00:00" },
                        dias = dias.ifBlank { "—" }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al crear la jornada", e)
                errorGuardado = "No se pudo crear la jornada. Intenta de nuevo."
            }
        }
    }

    fun actualizarJornada(jornada: ScheduleEntity, nombre: String, inicio: String, termino: String, dias: String) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.actualizar(
                    jornada.copy(
                        nombre = nombre.ifBlank { "Nueva jornada" },
                        horaInicio = inicio.ifBlank { "00:00" },
                        horaTermino = termino.ifBlank { "00:00" },
                        dias = dias.ifBlank { "—" }
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar la jornada", e)
                errorGuardado = "No se pudo guardar el cambio. Intenta de nuevo."
            }
        }
    }

    fun eliminarJornada(jornada: ScheduleEntity) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.eliminar(jornada)
                // Si no se cancela, el informe automático de WorkManager quedaría
                // programado para siempre sobre una jornada que ya no existe.
                JornadaReportScheduler.cancelar(getApplication(), jornada.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar la jornada", e)
                errorGuardado = "No se pudo eliminar la jornada. Intenta de nuevo."
            }
        }
    }
}
