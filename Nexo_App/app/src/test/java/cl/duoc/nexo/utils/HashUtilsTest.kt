package cl.duoc.nexo.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HashUtilsTest {

    @Test
    fun `la misma entrada siempre da el mismo hash`() {
        val hash1 = HashUtils.sha256("1234")
        val hash2 = HashUtils.sha256("1234")
        assertEquals(hash1, hash2)
    }

    @Test
    fun `entradas distintas dan hashes distintos`() {
        assertNotEquals(HashUtils.sha256("1234"), HashUtils.sha256("5678"))
    }

    @Test
    fun `el hash es SHA-256 valido — 64 caracteres hexadecimales`() {
        val hash = HashUtils.sha256("un-pin-cualquiera")
        assertEquals(64, hash.length)
        assertTrue(hash.all { it.isDigit() || it in 'a'..'f' })
    }
}
