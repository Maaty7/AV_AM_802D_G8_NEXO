package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import kotlinx.coroutines.launch

private const val TAG = "EditarCorreoViewModel"

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

    /** Valida el formato del correo antes de pedir el PIN — evita pedirlo para un valor que se va a rechazar igual. */
    fun validar(): Boolean {
        if (!emailRegex.matches(correo)) {
            error = "Correo inválido"
            return false
        }
        error = null
        return true
    }

    fun guardar(onGuardado: () -> Unit) {
        guardando = true
        viewModelScope.launch {
            try {
                dao.obtener()?.let { actual ->
                    dao.guardar(actual.copy(correoApoderado = correo))
                }
                onGuardado()
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar el correo del apoderado", e)
                error = "No se pudo guardar. Intenta de nuevo."
            } finally {
                guardando = false
            }
        }
    }
}
