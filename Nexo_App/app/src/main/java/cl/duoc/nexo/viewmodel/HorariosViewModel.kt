package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class HorariosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).scheduleDao()

    var jornadas by mutableStateOf<List<ScheduleEntity>>(emptyList())
        private set

    init {
        dao.observarTodas()
            .onEach { lista -> jornadas = lista }
            .launchIn(viewModelScope)
    }

    fun agregarJornada(nombre: String, inicio: String, termino: String, dias: String) {
        viewModelScope.launch {
            dao.insertar(
                ScheduleEntity(
                    nombre = nombre.ifBlank { "Nueva jornada" },
                    horaInicio = inicio.ifBlank { "00:00" },
                    horaTermino = termino.ifBlank { "00:00" },
                    dias = dias.ifBlank { "—" }
                )
            )
        }
    }

    fun eliminarJornada(jornada: ScheduleEntity) {
        viewModelScope.launch {
            dao.eliminar(jornada)
        }
    }
}