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
import cl.duoc.nexo.data.remote.ResultadoAuth
import cl.duoc.nexo.data.remote.ResultadoSubida
import cl.duoc.nexo.domain.construirJsonInforme
import kotlinx.coroutines.launch

class VincularCuentaViewModel(application: Application) : AndroidViewModel(application) {

    var vinculada by mutableStateOf(SesionBackendPrefs.estaVinculada(application))
        private set

    var emailVinculado by mutableStateOf(SesionBackendPrefs.obtenerEmail(application))
        private set

    var cargando by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var mensajeSincronizacion by mutableStateOf<String?>(null)
        private set

    fun registrar(email: String, password: String, nombre: String) {
        autenticar { NexoApiClient.registrar(BuildConfig.BASE_URL, email, password, nombre) }
    }

    fun iniciarSesion(email: String, password: String) {
        autenticar { NexoApiClient.iniciarSesion(BuildConfig.BASE_URL, email, password) }
    }

    private fun autenticar(llamada: suspend () -> ResultadoAuth) {
        if (cargando) return
        cargando = true
        error = null
        viewModelScope.launch {
            when (val resultado = llamada()) {
                is ResultadoAuth.Exito -> {
                    SesionBackendPrefs.guardarSesion(getApplication(), resultado.token, resultado.email)
                    vinculada = true
                    emailVinculado = resultado.email
                }
                is ResultadoAuth.ErrorCredenciales -> error = resultado.mensaje
                is ResultadoAuth.ErrorDeRed -> error = "No se pudo conectar al servidor. Revisa tu conexión y que el servidor esté corriendo."
            }
            cargando = false
        }
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
                    error = "No se pudo conectar al servidor. Revisa tu conexión y que el servidor esté corriendo."
                }
                else -> {
                    mensajeSincronizacion = "$exitosos de ${informes.size} informes sincronizados."
                }
            }
            cargando = false
        }
    }
}
