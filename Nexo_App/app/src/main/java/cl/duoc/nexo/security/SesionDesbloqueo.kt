package cl.duoc.nexo.security

/**
 * Si ya se verificó el PIN en ESTE proceso. Vive solo en memoria (ni
 * SavedStateHandle, ni rememberSaveable, ni DataStore) — a propósito: debe
 * volver a `false` cuando Android mata el proceso y lo recrea, pero NO
 * cuando la Activity se recrea por un simple cambio de configuración dentro
 * del mismo proceso (ver NexoNavHost, que es quien la usa).
 */
object SesionDesbloqueo {
    var desbloqueada: Boolean = false
}
