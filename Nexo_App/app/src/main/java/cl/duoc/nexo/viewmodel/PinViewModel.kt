package cl.duoc.nexo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

enum class PinMode { CREAR, CONFIRMAR }

class PinViewModel : ViewModel() {

    var modo by mutableStateOf(PinMode.CREAR)
        private set

    var buffer by mutableStateOf("")
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var primerPin: String = ""

    val titulo: String
        get() = if (modo == PinMode.CREAR) "Crear PIN" else "Confirmar PIN"

    val subtitulo: String
        get() = if (modo == PinMode.CREAR) "Ingresa un PIN de 4 dígitos" else "Ingresa nuevamente el PIN"

    fun onDigitPress(digit: String, onPinConfirmado: () -> Unit) {
        if (buffer.length >= 4) return
        error = null
        buffer += digit
        if (buffer.length == 4) {
            procesarPinCompleto(onPinConfirmado)
        }
    }

    fun onBackspace() {
        if (buffer.isNotEmpty()) {
            buffer = buffer.dropLast(1)
        }
    }

    private fun procesarPinCompleto(onPinConfirmado: () -> Unit) {
        when (modo) {
            PinMode.CREAR -> {
                primerPin = buffer
                buffer = ""
                modo = PinMode.CONFIRMAR
            }
            PinMode.CONFIRMAR -> {
                if (buffer == primerPin) {
                    onPinConfirmado()
                } else {
                    error = "Los PIN no coinciden, intenta de nuevo"
                    buffer = ""
                    modo = PinMode.CREAR
                }
            }
        }
    }
}