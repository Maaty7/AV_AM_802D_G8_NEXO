package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ScheduleEntity
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.FranjaUso
import cl.duoc.nexo.monitoring.UsageStatsRepository
import cl.duoc.nexo.work.JornadaReportScheduler
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class JornadaActivaViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleDao = NexoDatabase.getInstance(application).scheduleDao()

    var jornadaReferencia by mutableStateOf<ScheduleEntity?>(null)
        private set

    var jornadaEstaActiva by mutableStateOf(false)
        private set

    var appsUsadas by mutableStateOf<List<AppUsageInfo>>(emptyList())
        private set

    var minutosTotales by mutableStateOf(0L)
        private set

    var usoPorFranjas by mutableStateOf<List<FranjaUso>>(emptyList())
        private set

    init {
        scheduleDao.observarTodas()
            .onEach { lista ->
                val jornada = elegirJornadaReferencia(lista)
                jornadaReferencia = jornada
                jornadaEstaActiva = jornada?.let { estaActivaAhora(it) } ?: false
                refrescarUso()
                JornadaReportScheduler.reprogramarTodas(getApplication(), lista)
            }
            .launchIn(viewModelScope)
    }

    suspend fun refrescarUso() {
        val jornada = jornadaReferencia ?: run {
            appsUsadas = emptyList()
            minutosTotales = 0L
            usoPorFranjas = emptyList()
            return
        }
        val resultado = UsageStatsRepository.obtenerUsoEnRango(
            getApplication(),
            jornada.horaInicio,
            jornada.horaTermino
        )
        appsUsadas = resultado
        minutosTotales = resultado.sumOf { it.totalTimeMinutes }
        usoPorFranjas = UsageStatsRepository.obtenerUsoPorFranjas(
            getApplication(),
            jornada.horaInicio,
            jornada.horaTermino,
            tamanoFranjaMinutos(jornada)
        )
    }

    fun minutosPorCategoria(): Map<String, Long> {
        return appsUsadas
            .groupBy { it.categoria }
            .mapValues { (_, apps) -> apps.sumOf { it.totalTimeMinutes } }
    }

    /** Utilidad (Educación + Comunicación) vs. Entretenimiento (Entretenimiento + Juegos) vs. Otros, en minutos. */
    fun utilidadVsEntretenimiento(): Triple<Long, Long, Long> {
        val porCategoria = minutosPorCategoria()
        val utilidad = (porCategoria["Educación"] ?: 0L) + (porCategoria["Comunicación"] ?: 0L)
        val entretenimiento = (porCategoria["Entretenimiento"] ?: 0L) + (porCategoria["Juegos"] ?: 0L)
        val otros = porCategoria["Otros"] ?: 0L
        return Triple(utilidad, entretenimiento, otros)
    }

    /** Bloques más chicos para jornadas cortas, más grandes para jornadas largas, para que el gráfico no quede saturado. */
    private fun tamanoFranjaMinutos(jornada: ScheduleEntity): Int {
        val duracion = minutosDelDia(jornada.horaTermino) - minutosDelDia(jornada.horaInicio)
        val duracionPositiva = if (duracion > 0) duracion else duracion + 24 * 60
        return when {
            duracionPositiva <= 120 -> 15
            duracionPositiva <= 360 -> 30
            else -> 60
        }
    }

    private fun elegirJornadaReferencia(lista: List<ScheduleEntity>): ScheduleEntity? {
        if (lista.isEmpty()) return null
        return lista.firstOrNull { estaActivaAhora(it) } ?: lista.first()
    }

    private fun estaActivaAhora(jornada: ScheduleEntity): Boolean {
        val ahoraMin = minutosDelDia(horaActual())
        val inicioMin = minutosDelDia(jornada.horaInicio)
        val terminoMin = minutosDelDia(jornada.horaTermino)
        return if (inicioMin <= terminoMin) {
            ahoraMin in inicioMin..terminoMin
        } else {
            // Jornada que cruza medianoche (ej. horario nocturno)
            ahoraMin >= inicioMin || ahoraMin <= terminoMin
        }
    }

    private fun horaActual(): String {
        val calendar = java.util.Calendar.getInstance()
        val h = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val m = calendar.get(java.util.Calendar.MINUTE)
        return "%02d:%02d".format(h, m)
    }

    private fun minutosDelDia(hora: String): Int {
        val partes = hora.split(":")
        val h = partes.getOrNull(0)?.toIntOrNull() ?: 0
        val m = partes.getOrNull(1)?.toIntOrNull() ?: 0
        return h * 60 + m
    }
}