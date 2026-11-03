package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import android.net.Uri
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class CrearSalonRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val api = RetrofitClientSalones.getService(context)

    suspend fun crear(request: SalonCreateFase1Request) = withContext(Dispatchers.IO) {
        val archivos = prepararFotos(request.imagenes)
        api.crearSalonConFotos(
            Gson().toJson(request.copy(imagenes = emptyList())).toRequestBody("application/json; charset=utf-8".toMediaType()),
            archivos
        )
    }
    suspend fun reemplazarFotos(id: Int, imagenes: List<String>) = withContext(Dispatchers.IO) {
        api.reemplazarFotosSalon(id, prepararFotos(imagenes))
    }

    private fun prepararFotos(imagenes: List<String>): List<MultipartBody.Part> {
        require(imagenes.size in 1..5) { "Selecciona entre 1 y 5 fotos del salón." }
        return imagenes.mapIndexed { index, valor ->
            val uri = Uri.parse(valor)
            require(uri.scheme == "content" || uri.scheme == "file") { "Selecciona nuevamente las fotos desde tu dispositivo." }
            val bytes = resolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 5 * 1024 * 1024) { "Cada foto debe pesar como máximo 5 MB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
                ?: error("No se pudo abrir la foto ${index + 1}. Selecciónala nuevamente.")
            require(bytes.isNotEmpty() && bytes.size <= 5 * 1024 * 1024) { "Cada foto debe tener contenido y pesar como máximo 5 MB." }
            val png = bytes.take(8) == listOf(-119, 80, 78, 71, 13, 10, 26, 10).map { it.toByte() }
            val jpg = bytes.size >= 3 && bytes[0] == (-1).toByte() && bytes[1] == (-40).toByte() && bytes[2] == (-1).toByte()
            require(png || jpg) { "La foto ${index + 1} debe ser JPG o PNG." }
            val mime = if (png) "image/png" else "image/jpeg"
            MultipartBody.Part.createFormData("imagenes", "salon-$index.${if (png) "png" else "jpg"}", bytes.toRequestBody(mime.toMediaType()))
        }
    }

}
