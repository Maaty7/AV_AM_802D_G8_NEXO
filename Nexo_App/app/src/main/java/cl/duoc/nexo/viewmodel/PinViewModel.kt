package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import cl.duoc.nexo.utils.HashUtils
import kotlinx.coroutines.launch

enum class PinMode { CREAR, CONFIRMAR }

class PinViewModel(application: Application) : AndroidViewModel(application) {

    var modo by mutableStateOf(PinMode.CREAR)
        private set

    var buffer by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var guardando by mutableStateOf(false)
        private set

    private var primerPin: String = ""

    val titulo: String
        get() = if (modo == PinMode.CREAR) "Crear PIN" else "Confirmar PIN"

    val subtitulo: String
        get() = if (modo == PinMode.CREAR) "Ingresa un PIN de 4 dígitos" else "Ingresa nuevamente el PIN"

    fun onDigitPress(digit: String, nombre: String, correo: String, onGuardado: () -> Unit) {
        if (buffer.length >= 4 || guardando) return
        error = null
        buffer += digit
        if (buffer.length == 4) {
            procesarPinCompleto(nombre, correo, onGuardado)
        }
    }

    fun onBackspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.dropLast(1)
        }
    }

    private fun procesarPinCompleto(nombre: String, correo: String, onGuardado: () -> Unit) {
        when (modo) {
            PinMode.CREAR -> {
                primerPin = buffer
                buffer = ""
                modo = PinMode.CONFIRMAR
            }
            PinMode.CONFIRMAR -> {
                if (buffer == primerPin) {
                    guardarEnBaseDeDatos(nombre, correo, buffer, onGuardado)
                } else {
                    error = "Los PIN no coinciden, intenta de nuevo"
                    buffer = ""
                    modo = PinMode.CREAR
                }
            }
        }
    }

    private fun guardarEnBaseDeDatos(nombre: String, correo: String, pin: String, onGuardado: () -> Unit) {
        guardando = true
        viewModelScope.launch {
            val pinHash = HashUtils.sha256(pin)
            val dao = NexoDatabase.getInstance(getApplication()).parentSettingsDao()
            dao.guardar(
                ParentSettingsEntity(
                    nombreApoderado = nombre,
                    correoApoderado = correo,
                    pinHash = pinHash
                )
            )
            guardando = false
            onGuardado()
        }
    }
}