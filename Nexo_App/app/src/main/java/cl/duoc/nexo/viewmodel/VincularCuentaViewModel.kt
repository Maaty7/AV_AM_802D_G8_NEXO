package cl.duoc.nexo.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cl.duoc.nexo.BuildConfig
import cl.duoc.nexo.data.local.database.NexoDatabase
import cl.duoc.nexo.data.local.prefs.SesionBackendPrefs
import cl.duoc.nexo.data.remote.NexoApiClient
import cl.duoc.nexo.data.remote.ResultadoDesafio
import cl.duoc.nexo.data.remote.ResultadoReenvio
import cl.duoc.nexo.data.remote.ResultadoSubida
import cl.duoc.nexo.data.remote.ResultadoVerificacion
import cl.duoc.nexo.domain.construirJsonInforme
import kotlinx.coroutines.launch

private const val MENSAJE_ERROR_RED = "No se pudo conectar al servidor. Revisa tu conexión y que el servidor esté corriendo."

class VincularCuentaViewModel(application: Application) : AndroidViewModel(application) {

    var vinculada by mutableStateOf(SesionBackendPrefs.estaVinculada(application))
        private set

    var emailVinculado by mutableStateOf(SesionBackendPrefs.obtenerEmail(application))
        private set

    /** Si no es null, hay un registro/login pendiente de verificar con el código de 6 dígitos. */
    var desafioPendiente by mutableStateOf<String?>(null)
        private set

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensajeSincronizacion by mutableStateOf<String?>(null)
        private set

    /** Mensaje corto para la pantalla del código (ej. "Te enviamos un código nuevo"). */
    var mensajeCodigo by mutableStateOf<String?>(null)
        private set

    fun registrar(email: String, password: String, nombre: String) {
        iniciarDesafio { NexoApiClient.registrar(BuildConfig.BASE_URL, email, password, nombre) }
    }

    fun iniciarSesion(email: String, password: String) {
        iniciarDesafio { NexoApiClient.iniciarSesion(BuildConfig.BASE_URL, email, password) }
    }

    private fun iniciarDesafio(llamada: suspend () -> ResultadoDesafio) {
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch {
            when (val resultado = llamada()) {
                is ResultadoDesafio.Exito -> {
                    desafioPendiente = resultado.desafio
                    mensajeCodigo = "Te enviamos un código de verificación por correo."
                }
                is ResultadoDesafio.ErrorCredenciales -> error = resultado.mensaje
                is ResultadoDesafio.ErrorDeRed -> error = MENSAJE_ERROR_RED
            }
            cargando = false
        }
    }

    /** Solo si el código es correcto, vigente y no usado se guarda el token — recién ahí queda "vinculada". */
    fun verificarCodigo(codigo: String) {
        val desafio = desafioPendiente ?: return
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch {
            when (val resultado = NexoApiClient.verificarCodigo(BuildConfig.BASE_URL, desafio, codigo)) {
                is ResultadoVerificacion.Exito -> {
                    SesionBackendPrefs.guardarSesion(getApplication(), resultado.token, resultado.email)
                    vinculada = true
                    emailVinculado = resultado.email
                    desafioPendiente = null
                    mensajeCodigo = null
                }
                is ResultadoVerificacion.ErrorCodigo -> error = resultado.mensaje
                is ResultadoVerificacion.ErrorDeRed -> error = MENSAJE_ERROR_RED
            }
            cargando = false
        }
    }

    /** El backend aplica el límite de 60s entre reenvíos — acá solo se muestra el error si lo rechaza. */
    fun reenviarCodigo() {
        val desafio = desafioPendiente ?: return
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch {
            when (val resultado = NexoApiClient.reenviarCodigo(BuildConfig.BASE_URL, desafio)) {
                is ResultadoReenvio.Exito -> mensajeCodigo = "Te enviamos un código nuevo."
                is ResultadoReenvio.Error -> error = resultado.mensaje
                is ResultadoReenvio.ErrorDeRed -> error = MENSAJE_ERROR_RED
            }
            cargando = false
        }
    }

    /** Vuelve al formulario de email/contraseña sin completar la verificación. */
    fun cancelarVerificacion() {
        desafioPendiente = null
        mensajeCodigo = null
        error = null
    }

    fun desvincular() {
        SesionBackendPrefs.desvincular(getApplication())
        vinculada = false
        emailVinculado = null
        mensajeSincronizacion = null
    }

    /** Sube TODOS los informes guardados — el backend es idempotente, así que repetir no duplica nada. */
    fun sincronizarAhora() {
        if (cargando) return
        cargando = true
        error = null
        mensajeSincronizacion = null

        viewModelScope.launch {
            val context = getApplication<Application>()
            val token = SesionBackendPrefs.obtenerToken(context)
            if (token == null) {
                error = "No hay una cuenta vinculada."
                cargando = false
                return@launch
            }

            val informes = NexoDatabase.getInstance(context).reportDao().obtenerTodos()
            if (informes.isEmpty()) {
                mensajeSincronizacion = "No hay informes guardados todavía."
                cargando = false
                return@launch
            }

            val dispositivoUuid = SesionBackendPrefs.obtenerODefinirUuid(context)
            val dispositivoNombre = android.os.Build.MODEL ?: "Dispositivo Android"

            var exitosos = 0
            var sinRed = false
            var tokenInvalido = false

            informes.forEach { informe ->
                val json = construirJsonInforme(informe, dispositivoUuid, dispositivoNombre)
                when (NexoApiClient.subirInforme(BuildConfig.BASE_URL, token, json)) {
                    is ResultadoSubida.Exito -> exitosos++
                    is ResultadoSubida.ErrorDeRed -> sinRed = true
                    is ResultadoSubida.NoAutorizado -> tokenInvalido = true
                    is ResultadoSubida.ErrorServidor -> Unit
                }
            }

            when {
                tokenInvalido -> {
                    desvincular()
                    error = "La sesión ya no es válida. Vincula la cuenta de nuevo."
                }
                sinRed && exitosos == 0 -> {
                    error = MENSAJE_ERROR_RED
                }
                else -> {
                    mensajeSincronizacion = "$exitosos de ${informes.size} informes sincronizados."
                }
            }
            cargando = false
        }
    }
}
