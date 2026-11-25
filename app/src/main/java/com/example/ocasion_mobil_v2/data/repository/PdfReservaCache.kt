package com.example.ocasion_mobil_v2.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.DataInputStream
import java.io.File

internal suspend fun guardarPdfReserva(cache: File, respuesta: ResponseBody): File = respuesta.use { body ->
    var temporal: File? = null
    try {
        withContext(Dispatchers.IO) {
            val carpeta = File(cache, "contratos").apply { mkdirs() }
            val limiteAntiguedad = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            carpeta.listFiles()?.filter { it.isFile && it.lastModified() < limiteAntiguedad }?.forEach { it.delete() }

            val archivo = File.createTempFile("Cotizacion_", ".pdf", carpeta).also { temporal = it }
            val limiteBytes = 20L * 1024 * 1024
            check(body.contentLength() <= limiteBytes) { "El documento supera el tamaño permitido." }
            body.byteStream().use { entrada ->
                val firma = ByteArray(5)
                val lectura = DataInputStream(entrada)
                check(runCatching { lectura.readFully(firma) }.isSuccess && firma.toString(Charsets.US_ASCII) == "%PDF-") {
                    "El servidor no devolvió un PDF válido."
                }
                archivo.outputStream().use { salida ->
                    salida.write(firma)
                    val buffer = ByteArray(8192)
                    var total = firma.size.toLong()
                    while (true) {
                        ensureActive()
                        val cantidad = entrada.read(buffer)
                        if (cantidad < 0) break
                        total += cantidad
                        check(total <= limiteBytes) { "El documento supera el tamaño permitido." }
                        salida.write(buffer, 0, cantidad)
                    }
                }
            }
            archivo
        }
    } catch (e: Exception) {
        temporal?.delete()
        throw e
    }
}
