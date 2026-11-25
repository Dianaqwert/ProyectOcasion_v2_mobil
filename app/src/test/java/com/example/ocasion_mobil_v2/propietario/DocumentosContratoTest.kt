package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.data.remote.ApiSalonesService
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO
import com.example.ocasion_mobil_v2.ui.propietario.DocumentosSalonState
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class DocumentosContratoTest {
    @Test fun multipartConservaCamposRepetidosYBinariosYLeeRespuestaActual() = runBlocking {
        val bytes = byteArrayOf(-1,-40,-1,0,5,42)
        var solicitudes = 0
        val api = servicio { request ->
            solicitudes++
            assertEquals("POST",request.method)
            assertEquals("/api/v1/salones/12/documentos",request.url.encodedPath)
            val multipart = request.body as MultipartBody
            assertEquals("multipart/form-data",multipart.type.toString())
            val nombres = multipart.parts.map { it.headers?.get("Content-Disposition").orEmpty() }
            assertEquals(3,nombres.count { it.contains("name=\"fotosPropietario\"") })
            assertEquals(1,nombres.count { it.contains("name=\"ine\"") })
            assertEquals(8,multipart.parts.size)
            val buffer = Buffer(); multipart.parts.first().body.writeTo(buffer)
            assertArrayEquals(bytes,buffer.readByteArray())
            respuesta(request,201,"""{"id_salon":12,"id_expediente":8,"estadoRevisionAdmin":"SIN_ENVIAR","estadoSalon":"PUBLICADO","mensaje":"Documentos guardados"}""")
        }
        val campos = listOf("ine","propiedad","comprobante","vobo","fotosEstablecimiento",
            "fotosPropietario","fotosPropietario","fotosPropietario")
        val response = api.subirDocumentosSalon(12, campos.mapIndexed { i, campo ->
            MultipartBody.Part.createFormData(campo,"foto-$i.jpg",bytes.toRequestBody("image/jpeg".toMediaType()))
        })
        assertTrue(response.isSuccessful)
        assertEquals("SIN_ENVIAR",response.body()!!.estadoRevisionAdmin)
        assertEquals(8,response.body()!!.id_expediente)
        assertEquals(1,solicitudes)
    }

    @Test fun solicitudRevisionEsPatchSinPayloadJson() = runBlocking {
        val api = servicio { request ->
            assertEquals("PATCH",request.method)
            assertEquals("/api/v1/salones/12/solicitar-validacion",request.url.encodedPath)
            assertEquals(0L,request.body!!.contentLength())
            respuesta(request,200,"El salón ha sido enviado a revisión exitosamente.")
        }
        val result = api.solicitarValidacionSalon(12)
        assertTrue(result.isSuccessful)
        result.body()?.close()
        Unit
    }

    @Test fun publicacionYVerificacionSonEstadosIndependientesEnElBackendActual() {
        fun estado(revision: String) = DocumentosSalonState(salon = Gson().fromJson("""{
            "id_salon":12,"nombreSalon":"Jardín","estado":"PUBLICADO","verificado":false,
            "documentosValidacionSalonEntity":{"idExpediente":8,"estadoRevisionAdmin":"$revision",
            "observacionesAdmin":"Adjuntar comprobante legible"}}
        """,SalonPropietarioDTO::class.java))
        val rechazado = estado("RECHAZADO")
        assertEquals("PUBLICADO",rechazado.salon!!.estado)
        assertEquals("Adjuntar comprobante legible",rechazado.salon.documentosValidacionSalonEntity!!.observacionesAdmin)
        assertFalse(rechazado.bloqueado)
        assertTrue(estado("PENDIENTE").bloqueado)
        assertTrue(estado("APROBADO").bloqueado)
        assertFalse(estado("SIN_ENVIAR").bloqueado)
    }

    @Test fun creacionMultipartEnviaJsonYFotosBinarias() = runBlocking {
        val bytes = byteArrayOf(-1, -40, -1, 42)
        val api = servicio { request ->
            assertEquals("/api/v1/salones/fase1", request.url.encodedPath)
            val body = request.body as MultipartBody
            assertEquals(2, body.parts.size)
            assertTrue(body.parts[0].headers!!["Content-Disposition"]!!.contains("name=\"salon\""))
            assertEquals("application/json", body.parts[0].body.contentType()!!.let { "${it.type}/${it.subtype}" })
            assertTrue(body.parts[1].headers!!["Content-Disposition"]!!.contains("name=\"imagenes\""))
            val buffer = Buffer()
            body.parts[1].body.writeTo(buffer)
            assertArrayEquals(bytes, buffer.readByteArray())
            respuesta(request, 201, """{"id_salon":12,"estado":"BORRADOR"}""")
        }
        val result = api.crearSalonConFotos(
            """{"nombreSalon":"Jardín","imagenes":[]}""".toRequestBody("application/json".toMediaType()),
            listOf(MultipartBody.Part.createFormData("imagenes", "foto.jpg", bytes.toRequestBody("image/jpeg".toMediaType())))
        )
        assertEquals(12, result.body()!!.idSalon)
    }

    private fun servicio(interceptar: (Request) -> Response): ApiSalonesService = Retrofit.Builder()
        .baseUrl("http://localhost/")
        .client(OkHttpClient.Builder().addInterceptor { interceptar(it.request()) }.build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ApiSalonesService::class.java)

    private fun respuesta(request: Request, codigo: Int, body: String) = Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(codigo).message("test")
        .body(body.toResponseBody("application/json".toMediaType())).build()
}
