package cl.duoc.nexo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JornadaLogicTest {

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
}
