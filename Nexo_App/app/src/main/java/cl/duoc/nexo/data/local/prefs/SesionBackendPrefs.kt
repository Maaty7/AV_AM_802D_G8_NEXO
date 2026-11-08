package cl.duoc.nexo.data.local.prefs

import android.content.Context
import java.util.UUID

/**
 * Credenciales de sincronización con el backend (token de la cuenta
 * vinculada + UUID del dispositivo) — SharedPreferences normales, sin
 * cifrar: es el mismo nivel de protección que ya usa el resto de la
 * configuración de la app, y cifrarlo exigiría una librería nueva
 * (androidx.security.crypto) que no se pidió para esta tarea.
 *
 * El PIN y su hash NUNCA pasan por acá — viven solo en Room
 * (ParentSettingsEntity) y nunca se envían al servidor.
 */
object SesionBackendPrefs {
    private const val PREFS = "nexo_backend_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_EMAIL = "email"
    private const val KEY_UUID = "dispositivo_uuid"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun obtenerToken(context: Context): String? = prefs(context).getString(KEY_TOKEN, null)

    fun obtenerEmail(context: Context): String? = prefs(context).getString(KEY_EMAIL, null)

    fun estaVinculada(context: Context): Boolean = obtenerToken(context) != null

    fun guardarSesion(context: Context, token: String, email: String) {
        prefs(context).edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_EMAIL, email)
            .apply()
    }

    /** Borra el token (y el email) — el UUID del dispositivo NO se toca, sigue siendo el mismo dispositivo. */
    fun desvincular(context: Context) {
        prefs(context).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_EMAIL)
            .apply()
    }

    /** UUID del dispositivo: se genera UNA sola vez (la primera vez que se necesita) y queda fijo. */
    fun obtenerODefinirUuid(context: Context): String {
        val preferencias = prefs(context)
        return preferencias.getString(KEY_UUID, null) ?: UUID.randomUUID().toString().also { nuevoUuid ->
            preferencias.edit().putString(KEY_UUID, nuevoUuid).apply()
        }
    }
}
