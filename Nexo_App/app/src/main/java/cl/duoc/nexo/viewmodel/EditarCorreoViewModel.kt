package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import kotlinx.coroutines.launch

class EditarCorreoViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = NexoDatabase.getInstance(application).parentSettingsDao()
    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    var correo by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            correo = dao.obtener()?.correoApoderado ?: ""
        }
    }

    fun onCorreoChange(nuevoValor: String) {
        correo = nuevoValor
        error = null
    }

    fun guardar(onGuardado: () -> Unit) {
        if (!emailRegex.matches(correo)) {
            error = "Correo inválido"
            return
        }
        guardando = true
        viewModelScope.launch {
            dao.obtener()?.let { actual ->
                dao.guardar(actual.copy(correoApoderado = correo))
            }
            guardando = false
            onGuardado()
        }
    }
}
