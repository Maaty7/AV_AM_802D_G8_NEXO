package cl.duoc.nexo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class JornadaLogicTest {

    // Zona fija para que los tests sean deterministas sin importar dónde corran.
    private val zonaTest = ZoneId.of("America/Santiago")

    private fun millisDe(fecha: LocalDate, hora: String, zona: ZoneId = zonaTest): Long {
        val (h, m) = parsearHora(hora)
        return fecha.atTime(h, m).atZone(zona).toInstant().toEpochMilli()
    }

    @Test
    fun `minutosDelDia convierte HH-mm correctamente`() {
        assertEquals(0, minutosDelDia("00:00"))
        assertEquals(480, minutosDelDia("08:00"))
        assertEquals(1439, minutosDelDia("23:59"))
    }

    @Test
    fun `jornada normal esta activa dentro del rango`() {
        // Jornada escolar 08:00-14:00, ahora son las 10:00
        assertTrue(jornadaEstaActiva(horaActualMin = 600, horaInicioMin = 480, horaTerminoMin = 840))
    }

    @Test
    fun `jornada normal NO esta activa fuera del rango`() {
        // Jornada escolar 08:00-14:00, ahora son las 20:00
        assertFalse(jornadaEstaActiva(horaActualMin = 1200, horaInicioMin = 480, horaTerminoMin = 840))
    }

    @Test
    fun `jornada nocturna que cruza medianoche esta activa despues de medianoche`() {
        // Jornada nocturna 22:00-06:00, ahora son las 02:00
        assertTrue(jornadaEstaActiva(horaActualMin = 120, horaInicioMin = 1320, horaTerminoMin = 360))
    }

    @Test
    fun `jornada nocturna que cruza medianoche NO esta activa a media tarde`() {
        // Jornada nocturna 22:00-06:00, ahora son las 15:00
        assertFalse(jornadaEstaActiva(horaActualMin = 900, horaInicioMin = 1320, horaTerminoMin = 360))
    }

    @Test
    fun `calcularRangoJornada jornada normal con ahora a mitad de jornada recorta el fin a ahora`() {
        // Jornada 08:00-14:00 del 2026-10-14, "ahora" son las 10:00 de ese mismo día.
        val fechaBase = LocalDate.of(2026, 10, 14)
        val ahora = millisDe(fechaBase, "10:00")

        val rango = calcularRangoJornada(fechaBase, "08:00", "14:00", ahoraMillis = ahora, zona = zonaTest)

        assertTrue(rango is RangoJornada.Valido)
        val valido = rango as RangoJornada.Valido
        assertEquals(millisDe(fechaBase, "08:00"), valido.inicioMillis)
        assertEquals(ahora, valido.finMillis)
    }

    @Test
    fun `calcularRangoJornada jornada normal con ahora despues del termino usa el termino completo`() {
        // Jornada 08:00-14:00 del 2026-10-14, "ahora" ya son las 20:00 de ese mismo día.
        val fechaBase = LocalDate.of(2026, 10, 14)
        val ahora = millisDe(fechaBase, "20:00")

        val rango = calcularRangoJornada(fechaBase, "08:00", "14:00", ahoraMillis = ahora, zona = zonaTest)

        assertTrue(rango is RangoJornada.Valido)
        val valido = rango as RangoJornada.Valido
        assertEquals(millisDe(fechaBase, "08:00"), valido.inicioMillis)
        assertEquals(millisDe(fechaBase, "14:00"), valido.finMillis)
    }

    @Test
    fun `calcularRangoJornada jornada nocturna arranca el dia anterior a horaBase`() {
        // Jornada nocturna 22:00-06:00 que TERMINA el 2026-10-14 (empieza la noche del 13).
        val fechaBase = LocalDate.of(2026, 10, 14)
        val ahora = millisDe(fechaBase, "06:00")

        val rango = calcularRangoJornada(fechaBase, "22:00", "06:00", ahoraMillis = ahora, zona = zonaTest)

        assertTrue(rango is RangoJornada.Valido)
        val valido = rango as RangoJornada.Valido
        assertEquals(millisDe(fechaBase.minusDays(1), "22:00"), valido.inicioMillis)
        assertEquals(millisDe(fechaBase, "06:00"), valido.finMillis)
    }

    @Test
    fun `calcularRangoJornada con fechaBase incorrecta pasada medianoche queda invertido e invalido`() {
        // Este es el bug original: si se usa "hoy" (2026-10-15, el día en que
        // de verdad corre el Worker atrasado) en vez de la fecha objetivo
        // (2026-10-14) para una jornada normal 08:00-14:00, el inicio
        // (15 a las 08:00) queda después del fin recortado a "ahora"
        // (15 a las 00:30) y el rango queda invertido.
        val fechaBaseIncorrecta = LocalDate.of(2026, 10, 15)
        val ahora = millisDe(fechaBaseIncorrecta, "00:30")

        val rango = calcularRangoJornada(fechaBaseIncorrecta, "08:00", "14:00", ahoraMillis = ahora, zona = zonaTest)

        assertEquals(RangoJornada.Invalido, rango)
    }

    @Test
    fun `calcularFechaObjetivo con ahora dentro del mismo dia antes del termino da el dia anterior`() {
        // horaTermino 14:00, "ahora" son las 10:00 del mismo día: la última
        // ocurrencia de las 14:00 que ya pasó fue AYER.
        val ahora = LocalDateTime.of(2026, 10, 14, 10, 0)
        assertEquals(LocalDate.of(2026, 10, 13), calcularFechaObjetivo("14:00", ahora))
    }

    @Test
    fun `calcularFechaObjetivo con ahora despues del termino del mismo dia da hoy`() {
        // horaTermino 14:00, "ahora" son las 20:00 del mismo día.
        val ahora = LocalDateTime.of(2026, 10, 14, 20, 0)
        assertEquals(LocalDate.of(2026, 10, 14), calcularFechaObjetivo("14:00", ahora))
    }

    @Test
    fun `calcularFechaObjetivo justo pasada la medianoche todavia pertenece al dia anterior`() {
        // horaTermino 23:50, el Worker corre recién a las 00:05 del día siguiente (Doze).
        val ahora = LocalDateTime.of(2026, 10, 15, 0, 5)
        assertEquals(LocalDate.of(2026, 10, 14), calcularFechaObjetivo("23:50", ahora))
    }

    @Test
    fun `calcularFechaObjetivo muy atrasado al dia siguiente todavia da el dia anterior`() {
        // horaTermino 14:00, el Worker no corrió hasta las 10:00 del día siguiente.
        val ahora = LocalDateTime.of(2026, 10, 15, 10, 0)
        assertEquals(LocalDate.of(2026, 10, 14), calcularFechaObjetivo("14:00", ahora))
    }

    @Test
    fun `abreviacionDia devuelve las 2 letras usadas por ScheduleEntity dias`() {
        assertEquals("LU", abreviacionDia(java.time.DayOfWeek.MONDAY))
        assertEquals("DO", abreviacionDia(java.time.DayOfWeek.SUNDAY))
    }
}
