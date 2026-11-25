package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.data.remote.ApiSalonesService
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionDTO
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionRequest
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class EdicionSalonTest {
    @Test fun actualizaElIdSeleccionadoYConservaGaleriaYCodigoPostal() = runBlocking {
        val gson = Gson()
        for (clave in listOf("cp", "CP")) {
            assertEquals("01000", gson.fromJson("""{"$clave":"01000"}""", UbicacionDTO::class.java).cp)
        }
        val cliente = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/salones/27", request.url.encodedPath)
            val contenido = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
            val json = JsonParser.parseString(contenido).asJsonObject
            assertEquals("01000", json.getAsJsonObject("ubicacion").get("cp").asString)
            assertTrue(json.getAsJsonArray("imagenes").isEmpty)
            assertTrue(json.has("disponibilidadInicial"))
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200)
                .message("OK").body("{}".toResponseBody()).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://d2.test/").client(cliente)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiSalonesService::class.java)
        val datos = SalonCreateFase1Request("Salón", 100, 250.0, "Descripción",
            ubicacion = UbicacionRequest("Calle 1", 19.4, -99.1, "Ciudad", "01000"))
        assertTrue(api.actualizarSalon(27, datos).isSuccessful)
    }
}
