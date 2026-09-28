package cl.duoc.nexo.domain

/**
 * Serializa/deserializa el detalle por app de un informe a un texto simple
 * (una app por línea: "nombre|categoria|minutos"), usado para guardar
 * [AppUsageInfo] dentro de ReportEntity.appsUsadas sin necesitar una tabla
 * hija en Room — nunca se consulta en SQL, solo se pinta en el PDF.
 */
private const val SEPARADOR_CAMPO = "|"
private const val SEPARADOR_FILA = "\n"

fun serializarAppsUsadas(apps: List<AppUsageInfo>): String {
    return apps.joinToString(SEPARADOR_FILA) { app ->
        "${app.appName}$SEPARADOR_CAMPO${app.categoria}$SEPARADOR_CAMPO${app.totalTimeMinutes}"
    }
}

fun parsearAppsUsadas(texto: String): List<AppUsageInfo> {
    if (texto.isBlank()) return emptyList()

    return texto.split(SEPARADOR_FILA).mapNotNull { fila ->
        val partes = fila.split(SEPARADOR_CAMPO)
        if (partes.size != 3) return@mapNotNull null
        val minutos = partes[2].toLongOrNull() ?: return@mapNotNull null

        AppUsageInfo(
            packageName = "",
            appName = partes[0],
            totalTimeMs = minutos * 60_000L,
            categoria = partes[1]
        )
    }
}
