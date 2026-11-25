package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.example.ocasion_mobil_v2.data.repository.*
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.textoLlegadaPropietario
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime

class ReservacionesContratoTest {
    private val gson = GsonBuilder().registerTypeAdapter(Reservacion::class.java, ReservacionDeserializer()).create()
    private fun json(id: Int = 7, estado: String = EstadosReservacion.PENDIENTE) = """{"idReservacion":$id,"estado":"$estado","idSalon":2}"""

    @Test fun bandejaUsaRutaFiltroYDescartaEstadosAjenoAlInbox() = runBlocking {
        val repo = ReservacionesRepository(servicio { request ->
            assertEquals("GET", request.method)
            assertEquals("/api/v1/reservaciones/propietario/bandeja", request.url.encodedPath)
            assertEquals("COTIZACION_PENDIENTE", request.url.queryParameter("estado"))
            respuesta(request, 200, "[${json()},${json()},${json(8, "CONFIRMADA")},${json(9, "SOLICITUD_NUEVA")}]")
        })
        assertEquals(listOf(7L), repo.pendientes().map { it.id })
    }

    @Test fun aprobarEnviaPatchEsperandoPagoSinMotivoNiIdentidadDelUsuario() = runBlocking {
        val repo = ReservacionesRepository(servicio { request ->
            assertEquals("PATCH", request.method)
            assertEquals("/api/v1/reservaciones/7/estado", request.url.encodedPath)
            assertEquals("""{"estado":"ESPERANDO_PAGO"}""", cuerpo(request))
            respuesta(request, 200, json(estado = EstadosReservacion.APROBADA))
        })
        assertEquals(EstadosReservacion.APROBADA, repo.dictaminar(7, true, "texto ignorado al aprobar").estado)
    }

    @Test fun rechazarRecortaElMotivoYUsaEstadoDocumentado() = runBlocking {
        val repo = ReservacionesRepository(servicio { request ->
            val body = JsonParser.parseString(cuerpo(request)).asJsonObject
            assertEquals("RECHAZADA_PROPIETARIO", body["estado"].asString)
            assertEquals("No disponible", body["motivoRechazo"].asString)
            respuesta(request, 200, json(estado = EstadosReservacion.RECHAZADA))
        })
        assertEquals(EstadosReservacion.RECHAZADA, repo.dictaminar(7, false, "  No disponible  ").estado)
    }

    @Test fun rechazoVacioNoHacePeticiones() = runBlocking {
        val repo = ReservacionesRepository(servicio { error("No debe enviar nada") })
        try { repo.dictaminar(7, false, " \n "); fail("Debió validar el motivo") }
        catch (_: IllegalArgumentException) { }
    }

    @Test fun respuesta200ConEstadoOIdDistintoNoSeConsideraExito() = runBlocking {
        for (body in listOf(json(estado = "ACEPTADA_PROPIETARIO"), json(8, EstadosReservacion.APROBADA))) {
            val repo = ReservacionesRepository(servicio { respuesta(it, 200, body) })
            try { repo.dictaminar(7, true, null); fail("No debe retirar la tarjeta") }
            catch (e: IllegalStateException) { assertTrue(e.message!!.contains("no confirmó")) }
        }
    }

    @Test fun conservaCodigoHttpParaSesionVencidaYConflicto() = runBlocking {
        for (code in listOf(401, 403, 409, 422, 500)) {
            val repo = ReservacionesRepository(servicio { respuesta(it, code, "{}") })
            try { repo.dictaminar(7, true, null); fail("No debe informar éxito") }
            catch (e: ErrorReservaciones) { assertEquals(code, e.codigo) }
        }
    }

    @Test fun desgloseLeeContactoFechasServiciosYRetencionesDeLaReserva() {
        val r = gson.fromJson("""{
            "id_reservacion":7,"estado":"COTIZACION_PENDIENTE",
            "salon":{"id_salon":2,"nombreSalon":"Jardín"},
            "cliente":{"nombres":"Ana","apellidoPaterno":"Pérez","gmail":"ana@example.com","telefono":"5551234567"},
            "fecha_inicio":"2026-12-20","fechaFin":"2026-12-21","numeroInvitados":150,
            "servicios":[{"idServicio":1,"servicio":{"nombre":"Banquete"},"cantidad":150,"precioContratado":3500}],
            "inventario":[{"idRecurso":4,"recurso":{"nombre":"Silla"},"cantidadAsignada":150}],
            "inventarioDisponible":[{"idRecurso":8,"cantidadOcupada":80}]
        }""", Reservacion::class.java)
        assertEquals("Ana Pérez", r.nombreCliente)
        assertEquals("ana@example.com", r.correoCliente)
        assertEquals("5551234567", r.telefonoCliente)
        assertEquals("2026-12-20", r.fechaInicio)
        assertEquals(150, r.numeroInvitados)
        assertEquals("Jardín", r.nombreSalon)
        assertEquals("Banquete", r.servicios.single().nombre)
        assertEquals("Silla", r.inventario.single().nombre)
        assertEquals(150, r.inventario.single().cantidad)
    }

    @Test fun datosAusentesNoInventanClienteServiciosNiRetenciones() {
        val r = gson.fromJson(json(), Reservacion::class.java)
        assertNull(r.nombreCliente)
        assertTrue(r.inventario.isEmpty())
        assertTrue(r.servicios.isEmpty())
    }

    @Test fun panelMuestraLaLlegadaQueDevuelveD3() {
        val registrada = gson.fromJson(
            """{"id_reservacion":7,"estado_reservacion":"CONFIRMADA","check_in_realizado":true,
                "fecha_checkin":"2026-12-20T15:07:00"}""",
            Reservacion::class.java
        )
        assertEquals("Llegada registrada · 20/12/2026 · 15:07", registrada.textoLlegadaPropietario())
        assertEquals("Llegada registrada", registrada.copy(fechaCheckIn = null).textoLlegadaPropietario())
        assertEquals("Llegada registrada", registrada.copy(fechaCheckIn = "inválida").textoLlegadaPropietario())
        assertNull(registrada.copy(checkInRealizado = false).textoLlegadaPropietario())
        assertNull(registrada.copy(checkInRealizado = null).textoLlegadaPropietario())
    }

    @Test fun checkInConsultaLaBandejaYRegistraSoloTrasValidar() = runBlocking {
        val token = "OCASION-RES-7-ABCD"
        val llamadas = mutableListOf<String>()
        val repo = CheckInPropietario(servicio { request ->
            llamadas.add(request.method)
            when (request.method) {
                "GET" -> {
                    assertEquals("/api/v1/reservaciones/propietario/bandeja", request.url.encodedPath)
                    assertNull(request.url.queryParameter("estado"))
                    respuesta(request, 200, "[${reservaParaCheckIn()}]")
                }
                "POST" -> {
                    assertEquals("/api/v1/reservaciones/check-in", request.url.encodedPath)
                    assertEquals(token, request.url.queryParameter("tokenQr"))
                    respuesta(request, 200, """{"idReservacion":7,"fechaCheckIn":"2026-12-20T15:07:00"}""")
                }
                else -> error("Método inesperado")
            }
        }, { true }, { LocalDate.of(2026, 12, 20) })
        val resultado = repo.validar(token) as ResultadoCheckIn.Concedido
        assertEquals(7L, resultado.reserva.id)
        assertEquals(LocalDateTime.of(2026, 12, 20, 15, 7), resultado.llegada)
        assertEquals(listOf("GET", "POST"), llamadas)
    }

    @Test fun checkInRechazaPasesAlteradosConSaldoFueraDeFechaOYaUsados() = runBlocking {
        val casos = listOf(
            Triple("OCASION-RES-7-ALTERADO", reservaParaCheckIn(), "no coincide"),
            Triple("OCASION-RES-7-ABCD", reservaParaCheckIn(saldo = "0.01"), "saldo pendiente"),
            Triple("OCASION-RES-7-ABCD", reservaParaCheckIn(saldo = "null"), "no informó el saldo"),
            Triple("OCASION-RES-7-ABCD", reservaParaCheckIn(fecha = "2026-12-21"), "otra fecha"),
            Triple("OCASION-RES-7-ABCD", reservaParaCheckIn(estado = "CANCELADA"), "no está confirmada"),
            Triple("OCASION-RES-7-ABCD", reservaParaCheckIn(registrado = true), "ya fue registrada")
        )
        for ((token, reserva, motivo) in casos) {
            val repo = CheckInPropietario(servicio { request ->
                assertEquals("GET", request.method)
                respuesta(request, 200, "[$reserva]")
            }, { true }, { LocalDate.of(2026, 12, 20) })
            val resultado = repo.validar(token) as ResultadoCheckIn.Rechazado
            assertTrue(resultado.motivo.contains(motivo))
        }
    }

    @Test fun checkInConRespuestaInconsistenteODuplicadaNoConcedeAcceso() = runBlocking {
        val respuestas = listOf(
            200 to """{"idReservacion":8,"fechaCheckIn":"2026-12-20T15:07:00"}""",
            409 to """{"mensaje":"Ya registrado"}"""
        )
        for ((codigo, body) in respuestas) {
            val repo = CheckInPropietario(servicio { request ->
                if (request.method == "GET") respuesta(request, 200, "[${reservaParaCheckIn()}]")
                else respuesta(request, codigo, body)
            }, { true }, { LocalDate.of(2026, 12, 20) })
            assertFalse(repo.validar("OCASION-RES-7-ABCD") is ResultadoCheckIn.Concedido)
        }
    }

    private fun reservaParaCheckIn(
        fecha: String = "2026-12-20",
        saldo: String = "0",
        estado: String = "CONFIRMADA",
        registrado: Boolean = false
    ) = """{"id_reservacion":7,"estado_reservacion":"$estado","fecha_inicio":"$fecha",
        "fecha_fin":"$fecha","token_qr":"OCASION-RES-7-ABCD","saldoPendiente":$saldo,
        "check_in_realizado":$registrado}"""

    @Test fun entidadSinIdentificadorOEstadoYDesgloseInvalidoSeRechazan() {
        listOf("{}", """{"idReservacion":0,"estado":"COTIZACION_PENDIENTE"}""",
            """{"idReservacion":7}""", """{"idReservacion":7,"estado":"COTIZACION_PENDIENTE","inventario":{}}""").forEach {
            try { gson.fromJson(it, Reservacion::class.java); fail("Debe rechazar el contrato inválido") }
            catch (_: JsonParseException) { }
        }
    }

    private fun cuerpo(request: Request): String = Buffer().also { request.body!!.writeTo(it) }.readUtf8()
    private fun servicio(responder: (Request) -> okhttp3.Response): ApiReservaciones = Retrofit.Builder()
        .baseUrl("http://reservaciones.test/")
        .client(OkHttpClient.Builder().addInterceptor { responder(it.request()) }.build())
        .addConverterFactory(GsonConverterFactory.create(gson)).build().create(ApiReservaciones::class.java)
    private fun respuesta(request: Request, codigo: Int, body: String) = okhttp3.Response.Builder()
        .request(request).protocol(Protocol.HTTP_1_1).code(codigo).message("Respuesta de prueba")
        .body(body.toResponseBody("application/json".toMediaType())).build()
}
