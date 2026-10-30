package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.PausaJornadaEntity
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.domain.validarPausa
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "PausasJornadaViewModel"

class PausasJornadaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).pausaJornadaDao()
    private val scheduleDao = NexoDatabase.getInstance(application).scheduleDao()

    private var scheduleIdCargado: Int = -1
    private var jornada: ScheduleEntity? = null

    var pausas by mutableStateOf<List<PausaJornadaEntity>>(emptyList())
        private set

    var errorGuardado by mutableStateOf<String?>(null)
        private set

    /** Suscribe la lista de pausas de [scheduleId] — se llama una vez al entrar a la pantalla. */
    fun cargar(scheduleId: Int) {
        if (scheduleIdCargado == scheduleId) return
        scheduleIdCargado = scheduleId

        viewModelScope.launch {
            jornada = scheduleDao.obtenerPorId(scheduleId)
        }
        dao.observarPorJornada(scheduleId)
            .onEach { lista -> pausas = lista }
            .launchIn(viewModelScope)
    }

    fun agregar(nombre: String, horaInicio: String, horaTermino: String) {
        viewModelScope.launch {
            errorGuardado = null
            val jornadaActual = jornada ?: run {
                errorGuardado = "No se pudo guardar. Intenta de nuevo."
                return@launch
            }
            val otras = pausas.map { it.horaInicio to it.horaTermino }
            val error = validarPausa(horaInicio, horaTermino, jornadaActual.horaInicio, jornadaActual.horaTermino, otras)
            if (error != null) {
                errorGuardado = error
                return@launch
            }
            try {
                dao.insertar(
                    PausaJornadaEntity(
                        scheduleId = jornadaActual.id,
                        nombre = nombre.ifBlank { "Recreo" },
                        horaInicio = horaInicio,
                        horaTermino = horaTermino
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar la pausa", e)
                errorGuardado = "No se pudo guardar. Intenta de nuevo."
            }
        }
    }

    fun eliminar(pausa: PausaJornadaEntity) {
        viewModelScope.launch {
            try {
                errorGuardado = null
                dao.eliminar(pausa)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar la pausa", e)
                errorGuardado = "No se pudo eliminar. Intenta de nuevo."
            }
        }
    }
}
