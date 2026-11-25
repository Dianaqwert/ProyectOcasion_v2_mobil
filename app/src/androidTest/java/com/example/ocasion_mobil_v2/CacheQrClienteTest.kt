package com.example.ocasion_mobil_v2

import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocasion_mobil_v2.data.repository.CacheQrCliente
import com.example.ocasion_mobil_v2.data.repository.PaseQr
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class CacheQrClienteTest {
    @Test fun conservaElPaseCifradoEntreAperturasYSoloParaSuCuenta() = runBlocking {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val carpeta = Files.createTempDirectory(contexto.cacheDir.toPath(), "prueba-pases-").toFile()
        val aislado = object : ContextWrapper(contexto) {
            override fun getNoBackupFilesDir(): File = carpeta
        }
        try {
            val pase = PaseQr(17, "Salón de prueba", "2026-11-30", "OCASION-RES-17-TEST1234")
            CacheQrCliente(aislado, 10).actualizar(mapOf(pase.id to pase))
            assertEquals(listOf(pase), CacheQrCliente(aislado, 10).leer())
            assertTrue(CacheQrCliente(aislado, 11).leer().isEmpty())
            assertFalse(File(carpeta, "pases_reserva_10").readBytes().decodeToString().contains(pase.token))
            CacheQrCliente(aislado, 10).actualizar(mapOf(pase.id to null))
            assertTrue(CacheQrCliente(aislado, 10).leer().isEmpty())
        } finally {
            carpeta.deleteRecursively()
        }
    }
}
