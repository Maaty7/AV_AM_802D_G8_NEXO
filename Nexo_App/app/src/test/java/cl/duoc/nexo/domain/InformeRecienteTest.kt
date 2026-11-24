package cl.duoc.nexo.domain

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InformeRecienteTest {

    private val hoy = LocalDate.of(2026, 11, 24)

    @Test
    fun `informe de hoy es reciente`() {
        assertTrue(esInformeReciente("2026-11-24", hoy))
    }

    @Test
    fun `informe de ayer es reciente`() {
        assertTrue(esInformeReciente("2026-11-23", hoy))
    }

    @Test
    fun `informe de hace 2 dias no es reciente`() {
        assertFalse(esInformeReciente("2026-11-22", hoy))
    }

    @Test
    fun `informe de hace 5 dias no es reciente`() {
        assertFalse(esInformeReciente("2026-11-19", hoy))
    }

    @Test
    fun `informe de manana no es reciente`() {
        assertFalse(esInformeReciente("2026-11-25", hoy))
    }

    @Test
    fun `fecha con formato invalido no es reciente`() {
        assertFalse(esInformeReciente("no-es-una-fecha", hoy))
    }
}
