package cl.duoc.nexo.domain

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * Un informe es "reciente" si su [fecha] (formato `yyyy-MM-dd`, igual que
 * [cl.duoc.nexo.data.local.entities.ReportEntity.fecha]) es la de [hoy] o la
 * del día anterior — el mismo criterio que usa el backend
 * (`informes/correo.py::es_informe_reciente`) para decidir si vale la pena
 * mandar el correo de resumen. Solo a los informes recientes les sirve
 * adjuntar el PDF: a uno viejo nunca se le va a mandar el correo, así que
 * generar y subir su PDF sería trabajo (y datos) desperdiciados.
 */
fun esInformeReciente(fecha: String, hoy: LocalDate = LocalDate.now()): Boolean {
    val fechaInforme = try {
        LocalDate.parse(fecha)
    } catch (e: DateTimeParseException) {
        return false
    }
    return fechaInforme == hoy || fechaInforme == hoy.minusDays(1)
}
