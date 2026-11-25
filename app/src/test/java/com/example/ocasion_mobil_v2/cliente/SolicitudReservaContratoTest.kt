package com.example.ocasion_mobil_v2.cliente

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReservaPaso1State
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalTime

class SolicitudReservaContratoTest {
    private val fecha = LocalDate.now().plusDays(3)
    private val formulario = ReservaPaso1State(capacidadMaxima = 150, fecha = fecha, invitadosTexto = "100")

    @Test fun enviaFechaYHorarioSinSeleccionarServicios() = runBlocking {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
            val json = JsonParser.parseString(body).asJsonObject
            assertEquals("POST", request.method)
            assertEquals("/api/v1/reservaciones/solicitar", request.url.encodedPath)
            assertEquals(2, json.get("idSalon").asInt)
            assertEquals(fecha.toString(), json.get("fechaInicio").asString)
            assertEquals(fecha.toString(), json.get("fechaFin").asString)
            assertEquals("15:00", json.get("horaInicio").asString)
            assertEquals("22:00", json.get("horaFin").asString)
            assertEquals(100, json.get("numeroInvitados").asInt)
            assertFalse(json.has("servicios"))
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(201).message("Created")
                .body("""{"idReservacion":91,"estado":"SOLICITUD_NUEVA","servicios":[]}"""
                    .toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = Retrofit.Builder().baseUrl("https://d3.test/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(ApiReservaciones::class.java)
        val resultado = api.solicitar(checkNotNull(formulario.solicitud(2)))
        assertEquals(201, resultado.code())
        assertEquals(91L, resultado.body()?.idReservacion)
        assertEquals("SOLICITUD_NUEVA", resultado.body()?.estado)
    }

    @Test fun impideSolicitudOcupadaConHorarioInvalidoOExcesoDeInvitados() {
        assertNull(formulario.copy(fechasBloqueadas = setOf(fecha)).solicitud(2))
        assertNull(formulario.copy(horaFin = LocalTime.of(14, 59)).solicitud(2))
        assertNull(formulario.copy(invitadosTexto = "151").solicitud(2))
        assertNull(formulario.copy(enviando = true).solicitud(2))
        assertNull(formulario.copy(envioIncierto = true).solicitud(2))
    }
}
