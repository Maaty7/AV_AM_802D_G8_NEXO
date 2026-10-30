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

/**
 * Resultado de registrarse o iniciar sesión — ninguno de los dos devuelve
 * un token directo: el backend exige verificar un código de 6 dígitos
 * enviado por correo (2FA) antes de entregar uno.
 */
sealed interface ResultadoDesafio {
    data class Exito(val desafio: String) : ResultadoDesafio
    data class ErrorCredenciales(val mensaje: String) : ResultadoDesafio
    data object ErrorDeRed : ResultadoDesafio
}

/** Resultado de verificar el código de 6 dígitos — el único lugar donde se obtiene un token. */
sealed interface ResultadoVerificacion {
    data class Exito(val token: String, val email: String, val nombre: String) : ResultadoVerificacion
    data class ErrorCodigo(val mensaje: String) : ResultadoVerificacion
    data object ErrorDeRed : ResultadoVerificacion
}

/** Resultado de pedir que se reenvíe el código (respeta el límite de 60s que ya aplica el backend). */
sealed interface ResultadoReenvio {
    data object Exito : ResultadoReenvio
    data class Error(val mensaje: String) : ResultadoReenvio
    data object ErrorDeRed : ResultadoReenvio
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

    suspend fun registrar(baseUrl: String, email: String, password: String, nombre: String): ResultadoDesafio =
        withContext(Dispatchers.IO) {
            iniciarDesafio("${baseUrl}api/cuentas/registro/") {
                put("email", email)
                put("password", password)
                put("nombre", nombre)
            }
        }

    suspend fun iniciarSesion(baseUrl: String, email: String, password: String): ResultadoDesafio =
        withContext(Dispatchers.IO) {
            iniciarDesafio("${baseUrl}api/cuentas/login/") {
                put("email", email)
                put("password", password)
            }
        }

    suspend fun verificarCodigo(baseUrl: String, desafio: String, codigo: String): ResultadoVerificacion =
        withContext(Dispatchers.IO) {
            try {
                val cuerpo = JSONObject().apply {
                    put("desafio", desafio)
                    put("codigo", codigo)
                }.toString()
                val conexion = abrirConexionPost("${baseUrl}api/cuentas/verificar/", cuerpo)
                if (conexion.responseCode in 200..299) {
                    val respuesta = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
                    ResultadoVerificacion.Exito(
                        token = respuesta.getString("token"),
                        email = respuesta.optString("email", ""),
                        nombre = respuesta.optString("nombre", "")
                    )
                } else {
                    val cuerpoError = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    ResultadoVerificacion.ErrorCodigo(extraerMensajeError(cuerpoError))
                }
            } catch (e: IOException) {
                Log.w(TAG, "verificarCodigo: error de red", e)
                ResultadoVerificacion.ErrorDeRed
            }
        }

    suspend fun reenviarCodigo(baseUrl: String, desafio: String): ResultadoReenvio =
        withContext(Dispatchers.IO) {
            try {
                val cuerpo = JSONObject().apply { put("desafio", desafio) }.toString()
                val conexion = abrirConexionPost("${baseUrl}api/cuentas/reenviar/", cuerpo)
                if (conexion.responseCode in 200..299) {
                    ResultadoReenvio.Exito
                } else {
                    val cuerpoError = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    ResultadoReenvio.Error(extraerMensajeError(cuerpoError))
                }
            } catch (e: IOException) {
                Log.w(TAG, "reenviarCodigo: error de red", e)
                ResultadoReenvio.ErrorDeRed
            }
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

    private fun iniciarDesafio(url: String, construirCuerpo: JSONObject.() -> Unit): ResultadoDesafio {
        return try {
            val cuerpo = JSONObject().apply(construirCuerpo).toString()
            val conexion = abrirConexionPost(url, cuerpo)
            val codigo = conexion.responseCode
            if (codigo in 200..299) {
                val respuesta = JSONObject(conexion.inputStream.bufferedReader().use { it.readText() })
                ResultadoDesafio.Exito(respuesta.getString("desafio"))
            } else {
                val cuerpoError = conexion.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                ResultadoDesafio.ErrorCredenciales(extraerMensajeError(cuerpoError))
            }
        } catch (e: IOException) {
            Log.w(TAG, "iniciarDesafio: error de red", e)
            ResultadoDesafio.ErrorDeRed
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
