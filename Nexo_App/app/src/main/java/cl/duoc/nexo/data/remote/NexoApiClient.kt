package cl.duoc.nexo.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "NexoApiClient"
private const val TIMEOUT_MS = 15_000

/** Resultado de registrarse o iniciar sesión contra /api/cuentas/. */
sealed interface ResultadoAuth {
    data class Exito(val token: String, val email: String, val nombre: String) : ResultadoAuth
    data class ErrorCredenciales(val mensaje: String) : ResultadoAuth
    data object ErrorDeRed : ResultadoAuth
}

/** Resultado de subir un informe a /api/informes/. */
sealed interface ResultadoSubida {
    data object Exito : ResultadoSubida
    /** Token inválido/revocado — hay que desvincular la cuenta, no tiene sentido reintentar. */
    data object NoAutorizado : ResultadoSubida
    data object ErrorDeRed : ResultadoSubida
    data class ErrorServidor(val codigo: Int) : ResultadoSubida
}

/**
 * Cliente HTTP mínimo para hablar con el backend de NEXO — sin librerías
 * nuevas: `HttpURLConnection` + `org.json`, ambas ya incluidas en el SDK de
 * Android. Solo se usa si el apoderado vinculó una cuenta; si no, nada en la
 * app llama a nada de acá.
 */
object NexoApiClient {

    suspend fun registrar(baseUrl: String, email: String, password: String, nombre: String): ResultadoAuth =
        withContext(Dispatchers.IO) {
            autenticar("${baseUrl}api/cuentas/registro/", email, password, nombre)
        }

    suspend fun iniciarSesion(baseUrl: String, email: String, password: String): ResultadoAuth =
        withContext(Dispatchers.IO) {
            autenticar("${baseUrl}api/cuentas/login/", email, password, nombre = null)
        }

    suspend fun subirInforme(baseUrl: String, token: String, jsonInforme: String): ResultadoSubida =
        withContext(Dispatchers.IO) {
            try {
                val conexion = abrirConexionPost("${baseUrl}api/informes/", jsonInforme, token = token)
                val codigo = conexion.responseCode
                when {
                    codigo in 200..299 -> ResultadoSubida.Exito
                    codigo == 401 || codigo == 403 -> ResultadoSubida.NoAutorizado
                    else -> {
                        Log.w(TAG, "subirInforme: respuesta $codigo del servidor")
                        ResultadoSubida.ErrorServidor(codigo)
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "subirInforme: error de red", e)
                ResultadoSubida.ErrorDeRed
            }
        }

    private fun autenticar(url: String, email: String, password: String, nombre: String?): ResultadoAuth {
        return try {
            val cuerpo = JSONObject().apply {
                put("email", email)
                put("password", password)
                if (nombre != null) put("nombre", nombre)
            }.toString()

            val conexion = abrirConexionPost(url, cuerpo)
            val codigo = conexion.responseCode
            if (codigo in 200..299) {
                val respuesta = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
                ResultadoAuth.Exito(
                    token = respuesta.getString("token"),
                    email = respuesta.optString("email", email),
                    nombre = respuesta.optString("nombre", "")
                )
            } else {
                val cuerpoError = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                ResultadoAuth.ErrorCredenciales(extraerMensajeError(cuerpoError))
            }
        } catch (e: IOException) {
            Log.w(TAG, "autenticar: error de red", e)
            ResultadoAuth.ErrorDeRed
        }
    }

    /** DRF devuelve errores como {"campo": ["mensaje"]} o {"non_field_errors": ["mensaje"]} — se junta todo en un solo texto. */
    private fun extraerMensajeError(cuerpoError: String): String {
        if (cuerpoError.isBlank()) return "El servidor rechazó la solicitud."
        return try {
            val json = JSONObject(cuerpoError)
            val mensajes = mutableListOf<String>()
            json.keys().forEach { campo ->
                val valor = json.get(campo)
                if (valor is org.json.JSONArray) {
                    for (i in 0 until valor.length()) mensajes.add(valor.getString(i))
                } else {
                    mensajes.add(valor.toString())
                }
            }
            mensajes.joinToString(" ").ifBlank { "El servidor rechazó la solicitud." }
        } catch (e: Exception) {
            "El servidor rechazó la solicitud."
        }
    }

    private fun abrirConexionPost(url: String, cuerpo: String, token: String? = null): HttpURLConnection {
        val conexion = URL(url).openConnection() as HttpURLConnection
        conexion.requestMethod = "POST"
        conexion.connectTimeout = TIMEOUT_MS
        conexion.readTimeout = TIMEOUT_MS
        conexion.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (token != null) {
            conexion.setRequestProperty("Authorization", "Token $token")
        }
        conexion.doOutput = true
        conexion.outputStream.use { it.write(cuerpo.toByteArray(Charsets.UTF_8)) }
        return conexion
    }
}
