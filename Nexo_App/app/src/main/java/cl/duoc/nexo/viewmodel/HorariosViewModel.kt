package cl.duoc.nexo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import cl.duoc.nexo.domain.Jornada

class HorariosViewModel : ViewModel() {

    // TODO: reemplazar por datos reales de Room cuando exista persistencia
    var jornadas by mutableStateOf<List<Jornada>>(emptyList())
        private set

    fun agregarJornada(nombre: String, inicio: String, termino: String, dias: String) {
        jornadas = jornadas + Jornada(
            nombre = nombre.ifBlank { "Nueva jornada" },
            horaInicio = inicio.ifBlank { "00:00" },
            horaTermino = termino.ifBlank { "00:00" },
            dias = dias.ifBlank { "—" }
        )
    }
}