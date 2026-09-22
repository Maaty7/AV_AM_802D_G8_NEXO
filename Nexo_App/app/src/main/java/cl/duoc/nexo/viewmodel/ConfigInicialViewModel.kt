package cl.duoc.nexo.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

private val DOMINIOS_CONOCIDOS = setOf(
    "gmail.com", "hotmail.com", "outlook.com", "live.com", "icloud.com", "yahoo.com"
)

class ConfigInicialViewModel : ViewModel() {

    var nombreApoderado by mutableStateOf("")
        private set

    var correoApoderado by mutableStateOf("")
        private set

    var nombreError by mutableStateOf<String?>(null)
        private set

    var correoError by mutableStateOf<String?>(null)
        private set

    fun onNombreChange(nuevoValor: String) {
        nombreApoderado = nuevoValor
        nombreError = null
    }

    fun onCorreoChange(nuevoValor: String) {
        correoApoderado = nuevoValor
        correoError = null
    }

    fun validarYContinuar(onValido: (nombre: String, correo: String) -> Unit) {
        var esValido = true

        if (nombreApoderado.isBlank()) {
            nombreError = "Ingresa tu nombre"
            esValido = false
        }

        val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (correoApoderado.isBlank()) {
            correoError = "Ingresa tu correo"
            esValido = false
        } else if (!emailRegex.matches(correoApoderado)) {
            correoError = "Correo inválido"
            esValido = false
        } else if (correoApoderado.substringAfterLast("@").lowercase() !in DOMINIOS_CONOCIDOS) {
            correoError = "Usa un correo de un proveedor conocido (Gmail, Hotmail, Outlook, etc.)"
            esValido = false
        }

        if (esValido) {
            onValido(nombreApoderado, correoApoderado)
        }
    }
}