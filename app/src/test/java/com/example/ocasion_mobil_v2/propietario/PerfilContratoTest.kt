package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.data.remote.ApiService
import com.example.ocasion_mobil_v2.data.remote.model.ActualizarPerfilRequest
import com.example.ocasion_mobil_v2.data.remote.model.PerfilUsuario
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PerfilContratoTest {
    @Test
    fun actualizaCamposEditablesYLeeElPerfilDevuelto() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            assertEquals("PUT", request.method)
            assertEquals("/api/v1/usuarios/27", request.url.encodedPath)
            assertEquals("Bearer token-de-prueba", request.header("Authorization"))

            val body = Buffer()
            requireNotNull(request.body).writeTo(body)
            assertEquals(
                JsonParser.parseString(
                    """{"nombre":"Ana","apellidoPaterno":"López","apellidoMaterno":"","telefono":"4490000000"}"""
                ),
                JsonParser.parseString(body.readUtf8())
            )

            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(
                    """{
                        "id_User":27, "nombres":"Ana", "apellidoPaterno":"López", "apellidoMaterno":null,
                        "nombreUser":"ana", "gmail":"ana@example.com", "telefono":"4490000000",
                        "curp":"CURP-DE-PRUEBA", "roles":[3]
                    }""".toResponseBody()
                )
                .build()
        }.build()
        val api = Retrofit.Builder()
            .baseUrl("https://example.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        val respuesta = api.actualizarPerfil(
            "Bearer token-de-prueba", 27, ActualizarPerfilRequest("Ana", "López", "", "4490000000")
        )

        assertEquals(
            PerfilUsuario(
                id = 27, nombres = "Ana", apellidoPaterno = "López", apellidoMaterno = null,
                nombreUser = "ana", gmail = "ana@example.com", telefono = "4490000000", curp = "CURP-DE-PRUEBA"
            ),
            respuesta.body()
        )
    }
}
