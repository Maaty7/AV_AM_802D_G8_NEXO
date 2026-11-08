package cl.duoc.nexo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PausaJornadaLogicTest {

    // Zona fija para que los tests de milisegundos sean deterministas.
    private val zonaTest = ZoneId.of("America/Santiago")

    private fun millisDe(fecha: LocalDate, hora: String, zona: ZoneId = zonaTest): Long {
        val (h, m) = parsearHora(hora)
        return fecha.atTime(h, m).atZone(zona).toInstant().toEpochMilli()
    }

    // ---------- validarPausa: jornada normal ----------

    @Test
    fun `validarPausa jornada normal con pausa en medio es valida`() {
        // Jornada 08:00-14:00, recreo 10:00-10:15.
        val resultado = validarPausa("10:00", "10:15", "08:00", "14:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa pausa pegada al inicio de la jornada es valida`() {
        val resultado = validarPausa("08:00", "08:30", "08:00", "14:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa pausa pegada al final de la jornada es valida`() {
        val resultado = validarPausa("13:30", "14:00", "08:00", "14:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa duracion cero es invalida`() {
        val resultado = validarPausa("10:00", "10:00", "08:00", "14:00", otrasPausas = emptyList())
        assertNotNull(resultado)
    }

    @Test
    fun `validarPausa que empieza antes de la jornada es invalida`() {
        val resultado = validarPausa("07:45", "08:15", "08:00", "14:00", otrasPausas = emptyList())
        assertNotNull(resultado)
    }

    @Test
    fun `validarPausa que termina despues de la jornada es invalida`() {
        val resultado = validarPausa("13:45", "14:15", "08:00", "14:00", otrasPausas = emptyList())
        assertNotNull(resultado)
    }

    @Test
    fun `validarPausa que se solapa con otra pausa es invalida`() {
        val otras = listOf("10:00" to "10:15")
        val resultado = validarPausa("10:10", "10:30", "08:00", "14:00", otrasPausas = otras)
        assertNotNull(resultado)
    }

    @Test
    fun `validarPausa pegada a otra pausa sin solaparse es valida`() {
        // 10:00-10:15 y 10:15-10:30 se tocan en el borde, no se solapan.
        val otras = listOf("10:00" to "10:15")
        val resultado = validarPausa("10:15", "10:30", "08:00", "14:00", otrasPausas = otras)
        assertNull(resultado)
    }

    // ---------- validarPausa: jornada nocturna ----------

    @Test
    fun `validarPausa jornada nocturna con pausa que cruza medianoche es valida`() {
        // Jornada nocturna 22:00-06:00, pausa 23:30-00:30 (cruza medianoche ella misma).
        val resultado = validarPausa("23:30", "00:30", "22:00", "06:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa jornada nocturna pegada al borde final es valida`() {
        val resultado = validarPausa("05:30", "06:00", "22:00", "06:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa jornada nocturna pegada al borde inicial es valida`() {
        val resultado = validarPausa("22:00", "22:30", "22:00", "06:00", otrasPausas = emptyList())
        assertNull(resultado)
    }

    @Test
    fun `validarPausa jornada nocturna fuera de rango es invalida`() {
        // 12:00 del día (mitad de la tarde) no cae dentro de una jornada 22:00-06:00.
        val resultado = validarPausa("12:00", "12:30", "22:00", "06:00", otrasPausas = emptyList())
        assertNotNull(resultado)
    }

    // ---------- recorte de rangos: resolverRangoPausa + dividirPorPausas ----------

    @Test
    fun `dividirPorPausas sin pausas reproduce exactamente la suma plana que se usaba antes de este cambio`() {
        // UsageStatsRepository.calcularTiemposViaEventos sumaba antes "fin - inicio"
        // de cada sesión directamente. Con el recorte por pausas, ahora suma
        // dividirPorPausas(...).fueraDePausasMs — esto prueba que, sin pausas, da
        // exactamente lo mismo, para varias sesiones representativas a la vez.
        val sesiones = listOf(
            50_000L to 200_000L,
            300_000L to 950_000L,
            1_000_000L to 1_000_001L,
            7_200_000L to 7_200_000L // sesión de duración cero (caso límite real: evento sin duración)
        )

        val sumaPlanaAnterior = sesiones.sumOf { (inicio, fin) -> fin - inicio }
        val sumaConRecorteSinPausas = sesiones.sumOf { (inicio, fin) ->
            dividirPorPausas(inicio, fin, pausas = emptyList()).fueraDePausasMs
        }

        assertEquals(sumaPlanaAnterior, sumaConRecorteSinPausas)
    }

    @Test
    fun `dividirPorPausas sin pausas deja todo fuera`() {
        val fecha = LocalDate.of(2026, 10, 14)
        val inicio = millisDe(fecha, "10:00")
        val fin = millisDe(fecha, "10:30")

        val resultado = dividirPorPausas(inicio, fin, pausas = emptyList())

        assertEquals(30 * 60_000L, resultado.fueraDePausasMs)
        assertEquals(0L, resultado.dentroDePausasMs)
    }

    @Test
    fun `dividirPorPausas con pausa al inicio de la jornada cuenta toda la sesion como dentro`() {
        // Jornada 08:00-14:00, recreo 08:00-08:15; una sesión de app ocupa exactamente ese recreo.
        val fecha = LocalDate.of(2026, 10, 14)
        val jornadaInicioMillis = millisDe(fecha, "08:00")
        val pausa = resolverRangoPausa("08:00", "08:15", "08:00", jornadaInicioMillis)

        val sesionInicio = millisDe(fecha, "08:00")
        val sesionFin = millisDe(fecha, "08:15")

        val resultado = dividirPorPausas(sesionInicio, sesionFin, pausas = listOf(pausa))

        assertEquals(0L, resultado.fueraDePausasMs)
        assertEquals(15 * 60_000L, resultado.dentroDePausasMs)
    }

    @Test
    fun `dividirPorPausas con pausa en medio y sesion fuera de ella cuenta todo fuera`() {
        // Jornada 08:00-14:00, recreo 10:00-10:15; la sesión de app es de 11:00 a 11:30, no toca el recreo.
        val fecha = LocalDate.of(2026, 10, 14)
        val jornadaInicioMillis = millisDe(fecha, "08:00")
        val pausa = resolverRangoPausa("10:00", "10:15", "08:00", jornadaInicioMillis)

        val sesionInicio = millisDe(fecha, "11:00")
        val sesionFin = millisDe(fecha, "11:30")

        val resultado = dividirPorPausas(sesionInicio, sesionFin, pausas = listOf(pausa))

        assertEquals(30 * 60_000L, resultado.fueraDePausasMs)
        assertEquals(0L, resultado.dentroDePausasMs)
    }

    @Test
    fun `dividirPorPausas con pausa al final de la jornada cuenta toda la sesion como dentro`() {
        // Jornada 08:00-14:00, recreo 13:45-14:00; la sesión de app ocupa ese mismo tramo final.
        val fecha = LocalDate.of(2026, 10, 14)
        val jornadaInicioMillis = millisDe(fecha, "08:00")
        val pausa = resolverRangoPausa("13:45", "14:00", "08:00", jornadaInicioMillis)

        val sesionInicio = millisDe(fecha, "13:45")
        val sesionFin = millisDe(fecha, "14:00")

        val resultado = dividirPorPausas(sesionInicio, sesionFin, pausas = listOf(pausa))

        assertEquals(0L, resultado.fueraDePausasMs)
        assertEquals(15 * 60_000L, resultado.dentroDePausasMs)
    }

    @Test
    fun `dividirPorPausas con una sesion que cruza el limite de una pausa se divide en dos partes`() {
        // Recreo 10:00-10:15; la sesión de app va de 09:50 a 10:05: 10 min fuera, 5 min dentro.
        val fecha = LocalDate.of(2026, 10, 14)
        val jornadaInicioMillis = millisDe(fecha, "08:00")
        val pausa = resolverRangoPausa("10:00", "10:15", "08:00", jornadaInicioMillis)

        val sesionInicio = millisDe(fecha, "09:50")
        val sesionFin = millisDe(fecha, "10:05")

        val resultado = dividirPorPausas(sesionInicio, sesionFin, pausas = listOf(pausa))

        assertEquals(10 * 60_000L, resultado.fueraDePausasMs)
        assertEquals(5 * 60_000L, resultado.dentroDePausasMs)
    }

    @Test
    fun `dividirPorPausas con varias pausas separa correctamente dentro y fuera`() {
        // Jornada 08:00-14:00, recreos 09:00-09:15 y 11:00-11:15; la sesión va de 08:50 a 11:20
        // (150 min totales, cruza ambos recreos): 15+15 = 30 min dentro, 150-30 = 120 min fuera.
        val fecha = LocalDate.of(2026, 10, 14)
        val jornadaInicioMillis = millisDe(fecha, "08:00")
        val pausa1 = resolverRangoPausa("09:00", "09:15", "08:00", jornadaInicioMillis)
        val pausa2 = resolverRangoPausa("11:00", "11:15", "08:00", jornadaInicioMillis)

        val sesionInicio = millisDe(fecha, "08:50")
        val sesionFin = millisDe(fecha, "11:20")

        val resultado = dividirPorPausas(sesionInicio, sesionFin, pausas = listOf(pausa1, pausa2))

        assertEquals(120 * 60_000L, resultado.fueraDePausasMs)
        assertEquals(30 * 60_000L, resultado.dentroDePausasMs)
    }

    // ---------- recortarPorPausas (usado por obtenerUsoPorFranjas) ----------

    @Test
    fun `recortarPorPausas sin pausas devuelve la sesion completa sin modificar`() {
        val resultado = recortarPorPausas(1_000L, 5_000L, pausas = emptyList())
        assertEquals(listOf(1_000L to 5_000L), resultado)
    }

    @Test
    fun `recortarPorPausas con una pausa que no se solapa devuelve la sesion completa sin modificar`() {
        val resultado = recortarPorPausas(1_000L, 5_000L, pausas = listOf(10_000L to 20_000L))
        assertEquals(listOf(1_000L to 5_000L), resultado)
    }

    @Test
    fun `recortarPorPausas con una pausa en medio parte la sesion en dos sub-rangos`() {
        // Sesión 0-10000, pausa 4000-6000: queda [0-4000] y [6000-10000].
        val resultado = recortarPorPausas(0L, 10_000L, pausas = listOf(4_000L to 6_000L))
        assertEquals(listOf(0L to 4_000L, 6_000L to 10_000L), resultado)
    }

    @Test
    fun `recortarPorPausas con la sesion completamente dentro de una pausa devuelve vacio`() {
        val resultado = recortarPorPausas(4_000L, 5_000L, pausas = listOf(0L to 10_000L))
        assertEquals(emptyList<Pair<Long, Long>>(), resultado)
    }

    @Test
    fun `resolverRangoPausa en jornada nocturna ubica la pausa en el dia calendario correcto`() {
        // Jornada nocturna 22:00-06:00 que TERMINA el 2026-10-15 (empieza la noche del 14).
        // Pausa 23:00-23:30: debe caer en la noche del 14, no del 15.
        val fechaTermino = LocalDate.of(2026, 10, 15)
        val jornadaInicioMillis = millisDe(fechaTermino.minusDays(1), "22:00")

        val (inicioPausa, finPausa) = resolverRangoPausa("23:00", "23:30", "22:00", jornadaInicioMillis)

        assertEquals(millisDe(fechaTermino.minusDays(1), "23:00"), inicioPausa)
        assertEquals(millisDe(fechaTermino.minusDays(1), "23:30"), finPausa)
    }

    @Test
    fun `resolverRangoPausa en jornada nocturna ubica una pausa pasada medianoche en el dia siguiente`() {
        // Misma jornada nocturna 22:00-06:00 que termina el 2026-10-15; una pausa 01:00-01:30
        // debe caer la madrugada del 15 (después de medianoche), no del 14.
        val fechaTermino = LocalDate.of(2026, 10, 15)
        val jornadaInicioMillis = millisDe(fechaTermino.minusDays(1), "22:00")

        val (inicioPausa, finPausa) = resolverRangoPausa("01:00", "01:30", "22:00", jornadaInicioMillis)

        assertEquals(millisDe(fechaTermino, "01:00"), inicioPausa)
        assertEquals(millisDe(fechaTermino, "01:30"), finPausa)
    }
}
