package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparacionJornadaTest {

    private fun informe(
        minutosTotales: Long,
        minutosEducacion: Long = 0,
        minutosComunicacion: Long = 0,
        minutosRedesSociales: Long = 0,
        minutosEntretenimiento: Long = 0,
        minutosJuegos: Long = 0,
        minutosOtros: Long = 0,
        fecha: String = "2026-11-24"
    ): ReportEntity = ReportEntity(
        scheduleId = 1,
        nombreJornada = "Horario escolar",
        fecha = fecha,
        horaInicio = "08:00",
        horaTermino = "14:00",
        minutosTotales = minutosTotales,
        minutosEducacion = minutosEducacion,
        minutosComunicacion = minutosComunicacion,
        minutosRedesSociales = minutosRedesSociales,
        minutosEntretenimiento = minutosEntretenimiento,
        minutosJuegos = minutosJuegos,
        minutosOtros = minutosOtros,
        generadoEn = 1760000000000L
    )

    @Test
    fun `sin informe anterior devuelve null`() {
        val actual = informe(minutosTotales = 90)
        assertNull(calcularComparacionJornada(actual, anterior = null))
    }

    @Test
    fun `anterior en 0 minutos no calcula porcentaje`() {
        val actual = informe(minutosTotales = 40, minutosEducacion = 40)
        val anterior = informe(minutosTotales = 0, minutosEducacion = 0, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!

        assertEquals(40L, comparacion.total.diferenciaMinutos)
        assertNull(comparacion.total.porcentaje)
        assertEquals(TendenciaComparacion.SUBIO, comparacion.total.tendencia)
    }

    @Test
    fun `tiempo total que sube calcula diferencia y porcentaje positivos`() {
        val actual = informe(minutosTotales = 120)
        val anterior = informe(minutosTotales = 100, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!

        assertEquals(20L, comparacion.total.diferenciaMinutos)
        assertEquals(20.0, comparacion.total.porcentaje!!, 0.001)
        assertEquals(TendenciaComparacion.SUBIO, comparacion.total.tendencia)
    }

    @Test
    fun `tiempo total que baja calcula diferencia y porcentaje negativos`() {
        val actual = informe(minutosTotales = 80)
        val anterior = informe(minutosTotales = 100, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!

        assertEquals(-20L, comparacion.total.diferenciaMinutos)
        assertEquals(-20.0, comparacion.total.porcentaje!!, 0.001)
        assertEquals(TendenciaComparacion.BAJO, comparacion.total.tendencia)
    }

    @Test
    fun `tiempo total igual no tiene diferencia y queda marcado como igual`() {
        val actual = informe(minutosTotales = 100)
        val anterior = informe(minutosTotales = 100, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!

        assertEquals(0L, comparacion.total.diferenciaMinutos)
        assertEquals(0.0, comparacion.total.porcentaje!!, 0.001)
        assertEquals(TendenciaComparacion.IGUAL, comparacion.total.tendencia)
    }

    @Test
    fun `categoria que no existia antes aparece sin porcentaje`() {
        val actual = informe(minutosTotales = 30, minutosJuegos = 30)
        val anterior = informe(minutosTotales = 0, minutosJuegos = 0, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!
        val juegos = comparacion.categorias.single { it.nombre == "Juegos" }

        assertEquals(30L, juegos.valor.diferenciaMinutos)
        assertNull(juegos.valor.porcentaje)
        assertEquals(TendenciaComparacion.SUBIO, juegos.valor.tendencia)
    }

    @Test
    fun `categorias en 0 en ambos informes se omiten`() {
        val actual = informe(minutosTotales = 30, minutosEducacion = 30)
        val anterior = informe(minutosTotales = 20, minutosEducacion = 20, fecha = "2026-11-23")

        val comparacion = calcularComparacionJornada(actual, anterior)!!

        assertTrue(comparacion.categorias.none { it.nombre == "Juegos" })
        assertTrue(comparacion.categorias.none { it.nombre == "Otros" })
        assertEquals(1, comparacion.categorias.size)
    }
}
