package cl.duoc.nexo.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.entities.ParentSettingsEntity
import cl.duoc.nexo.utils.HashUtils
import kotlinx.coroutines.launch

private const val TAG = "PinViewModel"

enum class PinMode { VERIFICAR, CREAR, CONFIRMAR }

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
    private var esCambioPin = false
    private var pinHashActual: String? = null

    val titulo: String
        get() = when (modo) {
            PinMode.VERIFICAR -> "Verifica tu PIN actual"
            PinMode.CREAR -> if (esCambioPin) "Nuevo PIN" else "Crear PIN"
            PinMode.CONFIRMAR -> "Confirmar PIN"
        }

    val subtitulo: String
        get() = when (modo) {
            PinMode.VERIFICAR -> "Ingresa tu PIN actual para continuar"
            PinMode.CREAR -> "Ingresa un PIN de 4 dígitos"
            PinMode.CONFIRMAR -> "Ingresa nuevamente el PIN"
        }

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
            PinMode.VERIFICAR -> Unit // No aplica en el flujo de onboarding.
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
            try {
                val pinHash = HashUtils.sha256(pin)
                val dao = NexoDatabase.getInstance(getApplication()).parentSettingsDao()
                dao.guardar(
                    ParentSettingsEntity(
                        nombreApoderado = nombre,
                        correoApoderado = correo,
                        pinHash = pinHash
                    )
                )
                onGuardado()
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar la configuración inicial", e)
                error = "No se pudo guardar. Intenta de nuevo."
            } finally {
                guardando = false
            }
        }
    }

    /** Prepara el flujo de "Cambiar PIN": primero hay que verificar el PIN actual. */
    fun iniciarCambioPin() {
        esCambioPin = true
        modo = PinMode.VERIFICAR
        buffer = ""
        error = null
        viewModelScope.launch {
            pinHashActual = NexoDatabase.getInstance(getApplication()).parentSettingsDao().obtener()?.pinHash
        }
    }

    fun onDigitPressCambioPin(digit: String, onGuardado: () -> Unit) {
        if (buffer.length >= 4 || guardando) return
        error = null
        buffer += digit
        if (buffer.length == 4) {
            when (modo) {
                PinMode.VERIFICAR -> {
                    if (HashUtils.sha256(buffer) == pinHashActual) {
                        buffer = ""
                        modo = PinMode.CREAR
                    } else {
                        error = "PIN incorrecto, intenta de nuevo"
                        buffer = ""
                    }
                }
                PinMode.CREAR -> {
                    primerPin = buffer
                    buffer = ""
                    modo = PinMode.CONFIRMAR
                }
                PinMode.CONFIRMAR -> {
                    if (buffer == primerPin) {
                        actualizarPinEnBaseDeDatos(buffer, onGuardado)
                    } else {
                        error = "Los PIN no coinciden, intenta de nuevo"
                        buffer = ""
                        modo = PinMode.CREAR
                    }
                }
            }
        }
    }

    private fun actualizarPinEnBaseDeDatos(pin: String, onGuardado: () -> Unit) {
        guardando = true
        viewModelScope.launch {
            try {
                val dao = NexoDatabase.getInstance(getApplication()).parentSettingsDao()
                dao.obtener()?.let { actual ->
                    dao.guardar(actual.copy(pinHash = HashUtils.sha256(pin)))
                }
                onGuardado()
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar el PIN", e)
                error = "No se pudo guardar. Intenta de nuevo."
            } finally {
                guardando = false
            }
        }
    }

    /** Prepara la verificación de PIN al abrir la app (distinto del flujo de "Cambiar PIN": aquí solo se confirma, no se pide uno nuevo). */
    fun iniciarVerificacionAcceso() {
        modo = PinMode.VERIFICAR
        buffer = ""
        error = null
        viewModelScope.launch {
            pinHashActual = NexoDatabase.getInstance(getApplication()).parentSettingsDao().obtener()?.pinHash
        }
    }

    fun onDigitPressVerificarAcceso(digit: String, onCorrecto: () -> Unit) {
        if (buffer.length >= 4 || guardando) return
        error = null
        buffer += digit
        if (buffer.length == 4) {
            if (HashUtils.sha256(buffer) == pinHashActual) {
                buffer = ""
                onCorrecto()
            } else {
                error = "PIN incorrecto, intenta de nuevo"
                buffer = ""
            }
        }
    }
}