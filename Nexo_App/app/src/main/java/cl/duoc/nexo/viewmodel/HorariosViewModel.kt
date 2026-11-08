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
import cl.duoc.nexo.domain.validarPausa
import cl.duoc.nexo.work.JornadaReportScheduler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "HorariosViewModel"

class HorariosViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).scheduleDao()
    private val pausaDao = NexoDatabase.getInstance(application).pausaJornadaDao()

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
                val inicioFinal = inicio.ifBlank { "00:00" }
                val terminoFinal = termino.ifBlank { "00:00" }

                // Si la jornada ya tiene pausas, hay que revalidarlas contra el
                // horario nuevo — una pausa que calzaba con el horario viejo
                // podría quedar fuera de rango o solapada con el horario nuevo.
                val pausas = pausaDao.obtenerPorJornada(jornada.id)
                val pausasInvalidas = pausas.mapNotNull { pausa ->
                    val otras = pausas.filter { it.id != pausa.id }.map { it.horaInicio to it.horaTermino }
                    val error = validarPausa(pausa.horaInicio, pausa.horaTermino, inicioFinal, terminoFinal, otras)
                    error?.let { "\"${pausa.nombre}\" (${pausa.horaInicio}–${pausa.horaTermino}): $it" }
                }
                if (pausasInvalidas.isNotEmpty()) {
                    errorGuardado = "No se guardó: corrige estas pausas antes de cambiar el horario — " +
                        pausasInvalidas.joinToString(" / ")
                    return@launch
                }

                dao.actualizar(
                    jornada.copy(
                        nombre = nombre.ifBlank { "Nueva jornada" },
                        horaInicio = inicioFinal,
                        horaTermino = terminoFinal,
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
                pausaDao.eliminarPorJornada(jornada.id)
                // Si no se cancela, el informe automático de WorkManager quedaría
                // programado para siempre sobre una jornada que ya no existe.
                JornadaReportScheduler.cancelar(getApplication(), jornada.id)
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar la jornada", e)
                errorGuardado = "No se pudo eliminar la jornada. Intenta de nuevo."
            }
        }
    }

    /** Cuántas pausas tiene configuradas una jornada — usado para el botón "Recreos y descansos (N)" en Horarios. */
    suspend fun contarPausas(scheduleId: Int): Int = pausaDao.obtenerPorJornada(scheduleId).size
}
