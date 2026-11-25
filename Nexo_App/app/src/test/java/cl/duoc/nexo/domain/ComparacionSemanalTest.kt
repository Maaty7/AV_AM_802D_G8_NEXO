package cl.duoc.nexo.domain

import cl.duoc.nexo.data.local.entities.ReportEntity
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparacionSemanalTest {

    private fun informe(
        fecha: String,
        minutosTotales: Long,
        minutosEducacion: Long = 0,
        minutosJuegos: Long = 0
    ): ReportEntity = ReportEntity(
        scheduleId = 1,
        nombreJornada = "Horario escolar",
        fecha = fecha,
        horaInicio = "08:00",
        horaTermino = "14:00",
        minutosTotales = minutosTotales,
        minutosEducacion = minutosEducacion,
        minutosComunicacion = 0,
        minutosRedesSociales = 0,
        minutosEntretenimiento = 0,
        minutosJuegos = minutosJuegos,
        minutosOtros = 0,
        generadoEn = 1760000000000L
    )

    // --- calcularRangosComparacionSemanal -----------------------------------

    @Test
    fun `rangos para un miercoles van de lunes de esta semana al miercoles, y lunes a miercoles de la semana anterior`() {
        val miercoles = LocalDate.of(2026, 11, 25) // miércoles
        val rangos = calcularRangosComparacionSemanal(miercoles)

        assertEquals(LocalDate.of(2026, 11, 23), rangos.estaSemana.desde) // lunes
        assertEquals(LocalDate.of(2026, 11, 25), rangos.estaSemana.hasta)
        assertEquals(LocalDate.of(2026, 11, 16), rangos.semanaAnterior.desde) // lunes anterior
        assertEquals(LocalDate.of(2026, 11, 18), rangos.semanaAnterior.hasta) // miércoles anterior
    }

    @Test
    fun `rangos para un lunes son de un solo dia en cada semana`() {
        val lunes = LocalDate.of(2026, 11, 23)
        val rangos = calcularRangosComparacionSemanal(lunes)

        assertEquals(rangos.estaSemana.desde, rangos.estaSemana.hasta)
        assertEquals(rangos.semanaAnterior.desde, rangos.semanaAnterior.hasta)
        assertEquals(LocalDate.of(2026, 11, 16), rangos.semanaAnterior.desde)
    }

    // --- calcularComparacionSemanal ------------------------------------------

    @Test
    fun `semana anterior vacia devuelve null`() {
        val estaSemana = listOf(informe("2026-11-23", 60))
        assertNull(calcularComparacionSemanal(estaSemana, emptyList()))
    }

    @Test
    fun `semana completa suma los 7 dias de cada lado y compara el total`() {
        val estaSemana = (23..29).map { dia -> informe("2026-11-$dia", minutosTotales = 60, minutosEducacion = 60) }
        val semanaAnterior = (16..22).map { dia -> informe("2026-11-$dia", minutosTotales = 50, minutosEducacion = 50) }

        val comparacion = calcularComparacionSemanal(estaSemana, semanaAnterior)!!

        assertEquals(420L, comparacion.total.actual) // 7 * 60
        assertEquals(350L, comparacion.total.anterior) // 7 * 50
        assertEquals(70L, comparacion.total.diferenciaMinutos)
        assertEquals(TendenciaComparacion.SUBIO, comparacion.total.tendencia)
        val educacion = comparacion.categorias.single { it.nombre == "Educación" }
        assertEquals(420L, educacion.valor.actual)
        assertEquals(350L, educacion.valor.anterior)
    }

    @Test
    fun `semana parcial solo suma los dias que ya tienen informe`() {
        // Informe de un miércoles: a esta semana solo le corresponden lunes a miércoles.
        val estaSemana = listOf(
            informe("2026-11-23", 60), // lunes
            informe("2026-11-24", 50), // martes
            informe("2026-11-25", 40)  // miércoles (el informe actual)
        )
        val semanaAnterior = listOf(
            informe("2026-11-16", 30),
            informe("2026-11-17", 30),
            informe("2026-11-18", 30)
        )

        val comparacion = calcularComparacionSemanal(estaSemana, semanaAnterior)!!

        assertEquals(150L, comparacion.total.actual)
        assertEquals(90L, comparacion.total.anterior)
    }

    @Test
    fun `dias sin clases o feriados sin informe simplemente no suman`() {
        // Martes fue feriado: no hay informe para ese día, la lista solo trae lunes y miércoles.
        val estaSemana = listOf(
            informe("2026-11-23", 60), // lunes
            informe("2026-11-25", 40)  // miércoles
        )
        val semanaAnterior = listOf(
            informe("2026-11-16", 50),
            informe("2026-11-17", 50),
            informe("2026-11-18", 50)
        )

        val comparacion = calcularComparacionSemanal(estaSemana, semanaAnterior)!!

        assertEquals(100L, comparacion.total.actual) // solo lunes + miércoles, el feriado no aporta nada
        assertEquals(150L, comparacion.total.anterior)
        assertEquals(TendenciaComparacion.BAJO, comparacion.total.tendencia)
    }

    @Test
    fun `categorias en 0 en ambas semanas se omiten`() {
        val estaSemana = listOf(informe("2026-11-23", 30, minutosEducacion = 30))
        val semanaAnterior = listOf(informe("2026-11-16", 20, minutosEducacion = 20))

        val comparacion = calcularComparacionSemanal(estaSemana, semanaAnterior)!!

        assertTrue(comparacion.categorias.none { it.nombre == "Juegos" })
        assertEquals(1, comparacion.categorias.size)
    }

    @Test
    fun `categoria sin minutos en la semana anterior no calcula porcentaje`() {
        val estaSemana = listOf(informe("2026-11-23", 30, minutosJuegos = 30))
        val semanaAnterior = listOf(informe("2026-11-16", 10)) // sin juegos la semana anterior

        val comparacion = calcularComparacionSemanal(estaSemana, semanaAnterior)!!
        val juegos = comparacion.categorias.single { it.nombre == "Juegos" }

        assertEquals(30L, juegos.valor.diferenciaMinutos)
        assertNull(juegos.valor.porcentaje)
    }
}
