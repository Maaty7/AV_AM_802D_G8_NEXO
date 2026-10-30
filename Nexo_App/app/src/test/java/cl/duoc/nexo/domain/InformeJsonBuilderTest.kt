package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InformeJsonBuilderTest {

    private fun informeDePrueba(appsUsadas: String): ReportEntity = ReportEntity(
        scheduleId = 7,
        nombreJornada = "Horario escolar",
        fecha = "2026-10-20",
        horaInicio = "08:00",
        horaTermino = "14:00",
        dias = "LU, MA, MI, JU, VI",
        minutosTotales = 90,
        minutosEducacion = 60,
        minutosComunicacion = 10,
        minutosRedesSociales = 0,
        minutosEntretenimiento = 20,
        minutosJuegos = 0,
        minutosOtros = 0,
        appsUsadas = appsUsadas,
        generadoEn = 1760000000000L
    )

    @Test
    fun `construirJsonInforme incluye los campos del informe y del dispositivo`() {
        val informe = informeDePrueba(appsUsadas = "")
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Teléfono de Ana")

        assertTrue(json.contains("\"dispositivo_uuid\":\"abc-123\""))
        assertTrue(json.contains("\"dispositivo_nombre\":\"Teléfono de Ana\""))
        assertTrue(json.contains("\"nombre_jornada\":\"Horario escolar\""))
        assertTrue(json.contains("\"fecha\":\"2026-10-20\""))
        assertTrue(json.contains("\"hora_inicio\":\"08:00\""))
        assertTrue(json.contains("\"hora_termino\":\"14:00\""))
        assertTrue(json.contains("\"dias\":\"LU, MA, MI, JU, VI\""))
        assertTrue(json.contains("\"minutos_totales\":90"))
        assertTrue(json.contains("\"minutos_educacion\":60"))
        assertTrue(json.contains("\"minutos_comunicacion\":10"))
        assertTrue(json.contains("\"minutos_redes_sociales\":0"))
        assertTrue(json.contains("\"minutos_entretenimiento\":20"))
        assertTrue(json.contains("\"minutos_juegos\":0"))
        assertTrue(json.contains("\"minutos_otros\":0"))
        assertTrue(json.contains("\"generado_en\":1760000000000"))
    }

    @Test
    fun `construirJsonInforme con appsUsadas vacio produce una lista vacia de apps`() {
        val informe = informeDePrueba(appsUsadas = "")
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Teléfono de Ana")

        assertTrue(json.contains("\"apps\":[]"))
    }

    @Test
    fun `construirJsonInforme con varias apps las incluye todas en orden`() {
        val appsUsadas = "Classroom|Educación|60\nWhatsApp|Comunicación|10\nYouTube|Entretenimiento|20"
        val informe = informeDePrueba(appsUsadas = appsUsadas)
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Teléfono de Ana")

        val esperado = "\"apps\":[" +
            "{\"nombre\":\"Classroom\",\"categoria\":\"Educación\",\"minutos\":60}," +
            "{\"nombre\":\"WhatsApp\",\"categoria\":\"Comunicación\",\"minutos\":10}," +
            "{\"nombre\":\"YouTube\",\"categoria\":\"Entretenimiento\",\"minutos\":20}" +
            "]"
        assertTrue(json.contains(esperado))
    }

    @Test
    fun `construirJsonInforme escapa comillas y barras invertidas en los nombres de apps`() {
        val appsUsadas = """Mi "App" rara|Otros|5"""
        val informe = informeDePrueba(appsUsadas = appsUsadas)
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Teléfono de Ana")

        assertTrue(json.contains("\"nombre\":\"Mi \\\"App\\\" rara\""))
        assertFalse(json.contains("\"nombre\":\"Mi \"App\" rara\""))
    }

    // El nombre del dispositivo (no pasa por el códec de appsUsadas, que usa
    // "\n" como separador de fila) es el campo correcto para probar barras
    // invertidas y saltos de línea de verdad sin chocar con ese separador.

    @Test
    fun `construirJsonInforme escapa barras invertidas en el nombre del dispositivo`() {
        val informe = informeDePrueba(appsUsadas = "")
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = """C:\Apps\Raro""")

        assertTrue(json.contains("\"dispositivo_nombre\":\"C:\\\\Apps\\\\Raro\""))
    }

    @Test
    fun `construirJsonInforme escapa un salto de linea real en el nombre del dispositivo`() {
        val informe = informeDePrueba(appsUsadas = "")
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Linea1\nLinea2")

        assertTrue(json.contains("\"dispositivo_nombre\":\"Linea1\\nLinea2\""))
        assertFalse("no debe quedar un salto de línea real dentro del JSON", json.contains("\n"))
    }

    @Test
    fun `construirJsonInforme escapa caracteres de control genericos como u00XX`() {
        val informe = informeDePrueba(appsUsadas = "")
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Raro\u0001Nombre")

        assertTrue(json.contains("\"dispositivo_nombre\":\"Raro\\u0001Nombre\""))
    }

    @Test
    fun `construirJsonInforme produce un JSON balanceado en llaves y corchetes`() {
        val appsUsadas = "Classroom|Educación|60\nWhatsApp|Comunicación|10"
        val informe = informeDePrueba(appsUsadas = appsUsadas)
        val json = construirJsonInforme(informe, dispositivoUuid = "abc-123", dispositivoNombre = "Teléfono de Ana")

        assertEquals(json.count { it == '{' }, json.count { it == '}' })
        assertEquals(json.count { it == '[' }, json.count { it == ']' })
    }
}
