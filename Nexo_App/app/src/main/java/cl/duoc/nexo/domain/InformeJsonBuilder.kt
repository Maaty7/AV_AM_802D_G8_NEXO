package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity

/**
 * Construye el JSON que espera POST /api/informes/ del backend, a partir de
 * un [ReportEntity] ya guardado — mismos nombres de campo que
 * `informes/serializers.py` (`InformeEntradaSerializer`) en el backend.
 *
 * No usa org.json a propósito: esta función se testea con JUnit puro (JVM),
 * y org.json en un test local de Android usa el stub de la plataforma
 * (lanza "Stub!" en vez de construir el JSON de verdad) a menos que se
 * agregue una dependencia de test solo para eso — se prefirió no agregar
 * ninguna librería nueva y armar el JSON a mano en su lugar. El parseo de
 * las RESPUESTAS del backend (en NexoApiClient, que solo corre en el
 * dispositivo real, nunca en un test JVM) sí usa org.json sin problema.
 */
fun construirJsonInforme(informe: ReportEntity, dispositivoUuid: String, dispositivoNombre: String): String {
    val apps = parsearAppsUsadas(informe.appsUsadas)
    val appsJson = apps.joinToString(",") { app ->
        """{"nombre":${jsonString(app.appName)},"categoria":${jsonString(app.categoria)},"minutos":${app.totalTimeMinutes}}"""
    }

    return buildString {
        append('{')
        append(""""dispositivo_uuid":${jsonString(dispositivoUuid)},""")
        append(""""dispositivo_nombre":${jsonString(dispositivoNombre)},""")
        append(""""nombre_jornada":${jsonString(informe.nombreJornada)},""")
        append(""""fecha":${jsonString(informe.fecha)},""")
        append(""""hora_inicio":${jsonString(informe.horaInicio)},""")
        append(""""hora_termino":${jsonString(informe.horaTermino)},""")
        append(""""dias":${jsonString(informe.dias)},""")
        append(""""minutos_totales":${informe.minutosTotales},""")
        append(""""minutos_educacion":${informe.minutosEducacion},""")
        append(""""minutos_comunicacion":${informe.minutosComunicacion},""")
        append(""""minutos_redes_sociales":${informe.minutosRedesSociales},""")
        append(""""minutos_entretenimiento":${informe.minutosEntretenimiento},""")
        append(""""minutos_juegos":${informe.minutosJuegos},""")
        append(""""minutos_otros":${informe.minutosOtros},""")
        append(""""generado_en":${informe.generadoEn},""")
        append(""""apps":[$appsJson]""")
        append('}')
    }
}

/** Escapa [valor] como string JSON — comillas, barras invertidas y los caracteres de control más comunes. */
private fun jsonString(valor: String): String {
    val escapado = buildString {
        valor.forEach { caracter ->
            when (caracter) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(caracter)
            }
        }
    }
    return "\"$escapado\""
}
