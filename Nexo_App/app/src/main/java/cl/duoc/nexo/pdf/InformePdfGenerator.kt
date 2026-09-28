package cl.duoc.nexo.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import cl.duoc.nexo.data.local.entities.ReportEntity
import cl.duoc.nexo.domain.AppUsageInfo
import cl.duoc.nexo.domain.parsearAppsUsadas
import cl.duoc.nexo.ui.theme.NexoCoral
import cl.duoc.nexo.ui.theme.NexoDeep
import cl.duoc.nexo.ui.theme.NexoIce
import cl.duoc.nexo.ui.theme.NexoInk
import cl.duoc.nexo.ui.theme.NexoMute
import cl.duoc.nexo.ui.theme.coloresPorCategoria
import java.io.File
import java.io.FileOutputStream

/**
 * Genera un PDF con estilo (tamaño 595x842pt por página) con el detalle de
 * un informe, dibujado a mano sobre `android.graphics.pdf.PdfDocument` — sin
 * librerías externas. Usa los mismos colores de marca que el dashboard de
 * Estadísticas ([coloresPorCategoria]) para que se vean coherentes.
 *
 * La primera página trae encabezado, datos de la jornada, tiempo total y el
 * gráfico por categoría; la sección "Aplicaciones utilizadas" sigue después
 * y, si no alcanza, continúa en páginas nuevas (sin recortar la lista).
 *
 * El archivo vive en el cache interno de la app (`context.cacheDir/informes/`),
 * nunca en un lugar accesible directamente por el usuario: verlo (ACTION_VIEW)
 * no lo "descarga" a ningún lado, solo "Descargar" (Storage Access Framework)
 * lo copia a donde el usuario elija.
 */
object InformePdfGenerator {

    private const val ANCHO_PAGINA = 595f
    private const val ALTO_PAGINA = 842f
    private const val MARGEN_X = 40f
    private const val MARGEN_INFERIOR = ALTO_PAGINA - 60f // deja espacio para el pie de página

    /** Devuelve el PDF ya generado en cache si existe para este informe, o lo genera. */
    fun generarOReutilizar(context: Context, informe: ReportEntity): File {
        val carpeta = File(context.cacheDir, "informes").apply { mkdirs() }
        val archivo = File(carpeta, "informe_${informe.id}.pdf")
        if (archivo.exists()) return archivo

        val apps = parsearAppsUsadas(informe.appsUsadas).sortedByDescending { it.totalTimeMinutes }

        val documento = PdfDocument()
        var numeroPagina = 1
        var pagina = documento.startPage(paginaInfo(numeroPagina))
        var canvas = pagina.canvas
        canvas.drawColor(android.graphics.Color.WHITE)

        var y = dibujarEncabezado(canvas)
        y = dibujarBloqueJornada(canvas, informe, y)
        y = dibujarTiempoTotal(canvas, informe, y)
        y = dibujarUsoPorCategoria(canvas, informe, y) + 10f

        // Título de la sección de apps: si no alcanza ni el título, pasa a una página nueva.
        if (y + 40f > MARGEN_INFERIOR) {
            documento.finishPage(pagina)
            numeroPagina++
            pagina = documento.startPage(paginaInfo(numeroPagina))
            canvas = pagina.canvas
            canvas.drawColor(android.graphics.Color.WHITE)
            y = MARGEN_X + 20f
        }
        y = dibujarTituloAplicaciones(canvas, y)

        if (apps.isEmpty()) {
            dibujarSinApps(canvas, y)
        } else {
            var indice = 0
            while (indice < apps.size) {
                indice = dibujarFilasApps(canvas, apps, indice, y, MARGEN_INFERIOR)
                if (indice < apps.size) {
                    // No cupieron todas en esta página: se continúa en una nueva, sin recortar la lista.
                    documento.finishPage(pagina)
                    numeroPagina++
                    pagina = documento.startPage(paginaInfo(numeroPagina))
                    canvas = pagina.canvas
                    canvas.drawColor(android.graphics.Color.WHITE)
                    y = dibujarTituloAplicaciones(canvas, MARGEN_X + 30f, continuacion = true)
                }
            }
        }

        dibujarPiePagina(canvas)
        documento.finishPage(pagina)

        FileOutputStream(archivo).use { documento.writeTo(it) }
        documento.close()

        return archivo
    }

    private fun paginaInfo(numero: Int) =
        PdfDocument.PageInfo.Builder(ANCHO_PAGINA.toInt(), ALTO_PAGINA.toInt(), numero).create()

    /** Banda de color NexoDeep de borde a borde, con "NEXO" y el subtítulo en blanco. Devuelve el y donde sigue el contenido. */
    private fun dibujarEncabezado(canvas: Canvas): Float {
        val altoEncabezado = 100f

        val paintFondo = Paint().apply { color = NexoDeep.toArgb() }
        canvas.drawRect(0f, 0f, ANCHO_PAGINA, altoEncabezado, paintFondo)

        val paintTitulo = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 32f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("NEXO", MARGEN_X, 55f, paintTitulo)

        val paintSubtitulo = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 14f
            alpha = 220
            isAntiAlias = true
        }
        canvas.drawText("Informe de Jornada", MARGEN_X, 80f, paintSubtitulo)

        return altoEncabezado + 36f
    }

    /** Card NexoIce con nombre, fecha, horario y días de la jornada. Devuelve el y donde sigue el contenido. */
    private fun dibujarBloqueJornada(canvas: Canvas, informe: ReportEntity, yInicial: Float): Float {
        val alturaBloque = 116f
        val rect = RectF(MARGEN_X, yInicial, ANCHO_PAGINA - MARGEN_X, yInicial + alturaBloque)

        val paintFondo = Paint().apply { color = NexoIce.toArgb(); isAntiAlias = true }
        canvas.drawRoundRect(rect, 14f, 14f, paintFondo)

        val paintNombre = Paint().apply {
            color = NexoInk.toArgb()
            textSize = 17f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintDato = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 12.5f
            isAntiAlias = true
        }

        val xTexto = MARGEN_X + 22f
        var yTexto = yInicial + 32f
        canvas.drawText(informe.nombreJornada, xTexto, yTexto, paintNombre)
        yTexto += 24f
        canvas.drawText("Fecha: ${informe.fecha}", xTexto, yTexto, paintDato)
        yTexto += 20f
        canvas.drawText("Horario: ${informe.horaInicio} – ${informe.horaTermino}", xTexto, yTexto, paintDato)
        yTexto += 20f
        val diasTexto = informe.dias.ifBlank { "—" }
        canvas.drawText("Días: $diasTexto", xTexto, yTexto, paintDato)

        return yInicial + alturaBloque + 34f
    }

    /** Número grande con el tiempo total + etiqueta debajo. Devuelve el y donde sigue el contenido. */
    private fun dibujarTiempoTotal(canvas: Canvas, informe: ReportEntity, yInicial: Float): Float {
        val paintNumero = Paint().apply {
            color = NexoDeep.toArgb()
            textSize = 46f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val yNumero = yInicial + 40f
        canvas.drawText(formatoDuracion(informe.minutosTotales), MARGEN_X, yNumero, paintNumero)

        val paintEtiqueta = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 12.5f
            isAntiAlias = true
        }
        val yEtiqueta = yNumero + 20f
        canvas.drawText("Tiempo total de uso", MARGEN_X, yEtiqueta, paintEtiqueta)

        return yEtiqueta + 40f
    }

    /** Gráfico de barras horizontales por categoría, ordenadas de mayor a menor, solo las que tienen minutos > 0. Devuelve el y donde sigue el contenido. */
    private fun dibujarUsoPorCategoria(canvas: Canvas, informe: ReportEntity, yInicial: Float): Float {
        val categorias = listOf(
            "Educación" to informe.minutosEducacion,
            "Comunicación" to informe.minutosComunicacion,
            "Redes Sociales" to informe.minutosRedesSociales,
            "Entretenimiento" to informe.minutosEntretenimiento,
            "Juegos" to informe.minutosJuegos,
            "Otros" to informe.minutosOtros
        ).filter { it.second > 0 }.sortedByDescending { it.second }

        val paintTituloSeccion = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 12.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("USO POR CATEGORÍA", MARGEN_X, yInicial, paintTituloSeccion)

        if (categorias.isEmpty()) {
            val paintVacio = Paint().apply {
                color = NexoMute.toArgb()
                textSize = 12.5f
                isAntiAlias = true
            }
            canvas.drawText("Sin actividad registrada en esta jornada.", MARGEN_X, yInicial + 26f, paintVacio)
            return yInicial + 26f
        }

        val maxMinutos = categorias.maxOf { it.second }.toFloat().coerceAtLeast(1f)
        val labelAncho = 118f
        val numeroReserva = 58f
        val barraXInicio = MARGEN_X + labelAncho
        val barraAnchoMax = ANCHO_PAGINA - MARGEN_X - barraXInicio - numeroReserva
        val alturaBarra = 15f
        val alturaFila = 30f

        val paintNombreCategoria = Paint().apply {
            color = NexoInk.toArgb()
            textSize = 11.5f
            isAntiAlias = true
        }
        val paintMinutos = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 11f
            isAntiAlias = true
        }
        val paintPistaBarra = Paint().apply { color = NexoIce.toArgb(); isAntiAlias = true }

        var y = yInicial + 24f
        categorias.forEach { (nombre, minutos) ->
            val colorCategoria = (coloresPorCategoria[nombre] ?: NexoCoral).toArgb()
            val proporcion = minutos / maxMinutos
            val anchoBarra = (barraAnchoMax * proporcion).coerceAtLeast(6f)

            val barraTop = y
            val barraBottom = y + alturaBarra

            // Pista completa (fondo) + relleno proporcional al valor.
            canvas.drawRoundRect(
                RectF(barraXInicio, barraTop, barraXInicio + barraAnchoMax, barraBottom),
                7f, 7f, paintPistaBarra
            )
            canvas.drawRoundRect(
                RectF(barraXInicio, barraTop, barraXInicio + anchoBarra, barraBottom),
                7f, 7f, Paint().apply { color = colorCategoria; isAntiAlias = true }
            )

            val baseline = barraBottom - 3.5f
            canvas.drawText(nombre, MARGEN_X, baseline, paintNombreCategoria)
            canvas.drawText("$minutos min", barraXInicio + anchoBarra + 8f, baseline, paintMinutos)

            y += alturaFila
        }

        return y
    }

    private fun dibujarTituloAplicaciones(canvas: Canvas, y: Float, continuacion: Boolean = false): Float {
        val paint = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 12.5f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val titulo = if (continuacion) "APLICACIONES UTILIZADAS (CONTINUACIÓN)" else "APLICACIONES UTILIZADAS"
        canvas.drawText(titulo, MARGEN_X, y, paint)
        return y + 24f
    }

    private fun dibujarSinApps(canvas: Canvas, y: Float) {
        val paint = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 12.5f
            isAntiAlias = true
        }
        canvas.drawText("Sin apps registradas en esta jornada.", MARGEN_X, y + 6f, paint)
    }

    /**
     * Dibuja filas "● nombre de app … minutos" empezando en [indiceInicial],
     * hasta que no quepa otra sin pasar [yLimite]. Devuelve el índice desde
     * donde hay que seguir (== apps.size si ya se dibujaron todas).
     */
    private fun dibujarFilasApps(
        canvas: Canvas,
        apps: List<AppUsageInfo>,
        indiceInicial: Int,
        yInicial: Float,
        yLimite: Float
    ): Int {
        val alturaFila = 22f
        val diametroPunto = 7f
        val xNombre = MARGEN_X + diametroPunto + 8f
        val anchoMaxNombre = ANCHO_PAGINA - MARGEN_X - xNombre - 55f

        val paintNombre = Paint().apply {
            color = NexoInk.toArgb()
            textSize = 11.5f
            isAntiAlias = true
        }
        val paintMinutos = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 11f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }

        var y = yInicial
        var i = indiceInicial
        while (i < apps.size && y + alturaFila <= yLimite) {
            val app = apps[i]
            val baseline = y + alturaFila - 7f
            val colorPunto = (coloresPorCategoria[app.categoria] ?: NexoCoral).toArgb()

            canvas.drawCircle(
                MARGEN_X + diametroPunto / 2f,
                baseline - diametroPunto / 2.5f,
                diametroPunto / 2f,
                Paint().apply { color = colorPunto; isAntiAlias = true }
            )

            val nombre = truncarTexto(paintNombre, app.appName, anchoMaxNombre)
            canvas.drawText(nombre, xNombre, baseline, paintNombre)
            canvas.drawText("${app.totalTimeMinutes} min", ANCHO_PAGINA - MARGEN_X, baseline, paintMinutos)

            y += alturaFila
            i++
        }
        return i
    }

    /** Recorta [texto] con "…" al final si no entra en [anchoMax] con el estilo de [paint]. */
    private fun truncarTexto(paint: Paint, texto: String, anchoMax: Float): String {
        if (paint.measureText(texto) <= anchoMax) return texto
        var recortado = texto
        while (recortado.isNotEmpty() && paint.measureText("$recortado…") > anchoMax) {
            recortado = recortado.dropLast(1)
        }
        return if (recortado.isEmpty()) "…" else "$recortado…"
    }

    private fun dibujarPiePagina(canvas: Canvas) {
        val paint = Paint().apply {
            color = NexoMute.toArgb()
            textSize = 9.5f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Generado por NEXO — Supervisión digital parental",
            ANCHO_PAGINA / 2f,
            ALTO_PAGINA - 30f,
            paint
        )
    }

    private fun formatoDuracion(minutosTotales: Long): String {
        val horas = minutosTotales / 60
        val minutos = minutosTotales % 60
        return if (horas > 0) "${horas}h ${minutos}m" else "${minutos}m"
    }
}
