package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import retrofit2.Response

enum class TipoDocumento(val campo: String, val titulo: String, val minimo: Int, val maximo: Int, val permitePdf: Boolean = false) {
    INE("ine", "Identificación oficial (INE o pasaporte)", 1, 1),
    PROPIEDAD("propiedad", "Propiedad o derecho de uso", 1, 1, true),
    COMPROBANTE("comprobante", "Comprobante de domicilio del salón", 1, 1, true),
    VOBO("vobo", "Vo.Bo. de Protección Civil", 1, 1, true),
    ESTABLECIMIENTO("fotosEstablecimiento", "Fotos del establecimiento", 1, 10),
    PROPIETARIO("fotosPropietario", "Fotos del propietario", 3, Int.MAX_VALUE)
}

data class ArchivoDocumento(val uri: Uri, val nombre: String, val bytes: Long, val mime: String) {
    val extension: String get() = when (mime) { "application/pdf" -> "pdf"; "image/png" -> "png"; else -> "jpg" }
}

class DocumentosSalonRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val api = RetrofitClientSalones.getService(context.applicationContext)

    suspend fun salon(id: Int) = api.obtenerMisSalones().also { comprobar(it) }.body()
        ?.firstOrNull { it.id_salon == id } ?: error("El salón ya no está disponible en tu cuenta.")

    suspend fun inspeccionar(uri: Uri, tipo: TipoDocumento): ArchivoDocumento = withContext(Dispatchers.IO) {
        var nombre = "Archivo"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) nombre = cursor.getString(0) ?: nombre
        }
        var total = 0L
        val cabecera = ByteArray(8)
        var leidosCabecera = 0
        resolver.openInputStream(uri)?.use { stream ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = stream.read(buffer)
                if (n < 0) break
                if (leidosCabecera < 8) {
                    val copiar = minOf(n, 8 - leidosCabecera)
                    buffer.copyInto(cabecera, leidosCabecera, 0, copiar)
                    leidosCabecera += copiar
                }
                total += n
                require(total <= MAX_ARCHIVO) { "$nombre supera el máximo de 5 MB." }
            }
        } ?: error("No se pudo abrir $nombre. Selecciónalo de nuevo.")
        require(total > 0) { "$nombre está vacío." }
        val mime = when {
            leidosCabecera >= 3 && cabecera[0] == (-1).toByte() && cabecera[1] == (-40).toByte() && cabecera[2] == (-1).toByte() -> "image/jpeg"
            leidosCabecera >= 8 && cabecera.contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)) -> "image/png"
            leidosCabecera >= 5 && cabecera.copyOfRange(0,5).toString(Charsets.US_ASCII) == "%PDF-" && tipo.permitePdf -> "application/pdf"
            else -> error("$nombre: selecciona ${if (tipo.permitePdf) "PDF, JPG o PNG" else "JPG o PNG"}.")
        }
        ArchivoDocumento(uri, nombre, total, mime)
    }

    suspend fun subir(id: Int, seleccion: Map<TipoDocumento, List<ArchivoDocumento>>) {
        val partes = seleccion.flatMap { (tipo, archivos) -> archivos.mapIndexed { indice, archivo ->
            val body = object : RequestBody() {
                override fun contentType() = archivo.mime.toMediaType()
                override fun contentLength() = archivo.bytes
                override fun writeTo(sink: BufferedSink) {
                    resolver.openInputStream(archivo.uri)?.source()?.use { sink.writeAll(it) }
                        ?: throw java.io.IOException("No se puede leer ${archivo.nombre}. Vuelve a seleccionarlo.")
                }
            }
            MultipartBody.Part.createFormData(tipo.campo, "${tipo.campo}-$indice.${archivo.extension}", body)
        } }
        comprobar(api.subirDocumentosSalon(id, partes))
    }

    suspend fun enviar(id: Int) { val response = api.solicitarValidacionSalon(id); comprobar(response); response.body()?.close() }

    suspend fun publicar(id: Int) { val response = api.publicarSalon(id); comprobar(response); response.body()?.close() }

    private fun comprobar(response: Response<*>) {
        if (response.isSuccessful) return
        val mensaje = response.errorBody()?.use { body ->
            runCatching { JSONObject(body.string()).optString("mensaje").ifBlank { null } }.getOrNull()
        }
        error(mensaje ?: when (response.code()) {
            401 -> "Tu sesión venció. Inicia sesión de nuevo."
            403 -> "Tu sesión no tiene permiso para este salón. Vuelve a iniciar sesión como propietario."
            409 -> "El estado del salón o del expediente cambió. Actualiza el estado antes de continuar."
            413 -> "El servidor rechazó el tamaño de los archivos (5 MB por archivo, 100 MB en total)."
            415 -> "Formato no permitido. Usa JPG, PNG o PDF según el documento."
            422 -> "Completa ubicación, servicios, inventario y todos los documentos obligatorios."
            else -> "No se pudo completar la solicitud (${response.code()}). Puedes reintentar."
        })
    }

    companion object {
        const val MAX_ARCHIVO = 5L * 1024 * 1024
        // Margen para las cabeceras multipart dentro del límite de 100 MiB del servidor.
        const val MAX_TOTAL = 99L * 1024 * 1024
    }
}
