package cl.duoc.nexo.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Prueba solo [AppCategoryClassifier.clasificarConocido] — la parte pura que
 * no necesita un Context real de Android. El fallback por categoría del
 * sistema (clasificar() con un paquete que no está en el mapa) sí depende
 * de PackageManager y queda fuera de una prueba unitaria de JVM normal.
 */
class AppCategoryClassifierTest {

    @Test
    fun `Instagram se clasifica como Redes Sociales`() {
        assertEquals("Redes Sociales", AppCategoryClassifier.clasificarConocido("com.instagram.android"))
    }

    @Test
    fun `WhatsApp se clasifica como Comunicacion`() {
        assertEquals("Comunicación", AppCategoryClassifier.clasificarConocido("com.whatsapp"))
    }

    @Test
    fun `YouTube se clasifica como Entretenimiento`() {
        assertEquals("Entretenimiento", AppCategoryClassifier.clasificarConocido("com.google.android.youtube"))
    }

    @Test
    fun `Roblox se clasifica como Juegos`() {
        assertEquals("Juegos", AppCategoryClassifier.clasificarConocido("com.roblox.client"))
    }

    @Test
    fun `Chrome se clasifica como Educacion`() {
        assertEquals("Educación", AppCategoryClassifier.clasificarConocido("com.android.chrome"))
    }

    @Test
    fun `paquete desconocido no esta en el mapa (cae al fallback del sistema)`() {
        assertNull(AppCategoryClassifier.clasificarConocido("com.paquete.desconocido"))
    }
}
