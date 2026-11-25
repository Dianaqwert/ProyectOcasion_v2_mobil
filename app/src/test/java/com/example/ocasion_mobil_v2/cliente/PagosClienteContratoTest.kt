package com.example.ocasion_mobil_v2.cliente

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.OrdenPendientePaypal
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.remote.model.ReservacionDeserializer
import com.example.ocasion_mobil_v2.data.repository.PagosClienteRepository
import com.example.ocasion_mobil_v2.data.repository.ReglasPagoCliente
import com.example.ocasion_mobil_v2.data.repository.TransportePaypal
import com.example.ocasion_mobil_v2.data.repository.combinarPasesQr
import com.example.ocasion_mobil_v2.data.repository.paseQr
import com.example.ocasion_mobil_v2.ui.cliente.pagos.EstadoVisualReserva
import com.example.ocasion_mobil_v2.ui.cliente.pagos.HistorialCliente
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class PagosClienteContratoTest {
    @get:Rule val temporal = TemporaryFolder()
    private val gson = GsonBuilder().registerTypeAdapter(Reservacion::class.java, ReservacionDeserializer()).create()
    private val url = "https://www.sandbox.paypal.com/checkoutnow?token=ORD-7"
    private val raiz = "/api/v1/reservaciones"

    @Test fun dashboardOrdenaPorFechaYUsaElEstadoVisualDelSaldo() {
        val datos = listOf(
            """{"id_reservacion":90,"estado_reservacion":"RECHAZADO","fecha_inicio":"2026-11-22"}""",
            """{"id_reservacion":17,"estado_reservacion":"CONFIRMADA","fecha_inicio":"2026-12-04","saldoPendiente":0}""",
            """{"id_reservacion":91,"estado_reservacion":"SOLICITUD_NUEVA"}"""
        ).map { gson.fromJson(it, Reservacion::class.java) }
        assertEquals(listOf(17L, 90L, 91L), HistorialCliente.ordenar(datos).map { it.id })
        assertEquals(EstadoVisualReserva.RECHAZADA, EstadoVisualReserva.de("RECHAZADO", false))
        assertEquals(EstadoVisualReserva.REVISION, EstadoVisualReserva.de("SOLICITUD_NUEVA", false))
        assertEquals(EstadoVisualReserva.COTIZADA, EstadoVisualReserva.de("ESPERANDO_PAGO", true))
        assertEquals(EstadoVisualReserva.CONFIRMADA, EstadoVisualReserva.de("CONFIRMADA", false))
        assertEquals(EstadoVisualReserva.POR_PAGAR, EstadoVisualReserva.de("CONFIRMADA", true))
    }

    @Test fun pdfSeGuardaEnCacheSoloSiLaReservaPerteneceAlClienteYEstaActiva() = runBlocking {
        val documento = "%PDF-1.7\n%%EOF"
        var peticionesPdf = 0
        val repo = repo {
            when (it.url.encodedPath) {
                "$raiz/mis-reservaciones" -> """[${reserva("CONFIRMADA", 0)},
                    {"id_reservacion":18,"estado_reservacion":"RECHAZADO"}]"""
                "$raiz/17/pdf" -> { peticionesPdf++; documento }
                else -> error("Ruta inesperada: ${it.url}")
            }
        }
        assertTrue(runCatching { repo.descargarPdf(18, temporal.root) }.isFailure)
        assertTrue(runCatching { repo.descargarPdf(99, temporal.root) }.isFailure)
        val archivo = repo.descargarPdf(17, temporal.root)
        assertEquals("contratos", archivo.parentFile?.name)
        assertEquals(documento, archivo.readText())
        assertEquals(1, peticionesPdf)
    }

    @Test fun respuestaInvalidaNoQuedaEnCacheComoPdf() = runBlocking {
        val repo = repo {
            if (it.url.encodedPath == "$raiz/mis-reservaciones") """[${reserva("ESPERANDO_PAGO", 1000)}]"""
            else "<html>Error</html>"
        }
        assertTrue(runCatching { repo.descargarPdf(17, temporal.root) }.isFailure)
        assertTrue(temporal.root.resolve("contratos").listFiles().orEmpty().isEmpty())
    }

    @Test fun historialLeeLaFichaCompletaDevueltaPorD3() = runBlocking {
        val repo = repo { request ->
            assertEquals("GET", request.method)
            assertEquals("$raiz/mis-reservaciones", request.url.encodedPath)
            """[{"id_reservacion":17,"estado_reservacion":"ANTICIPO_PAGADO","nombreSalon":"Salón Jardín",
                "ubicacionSalon":"Aguascalientes","precioHoraSalon":500,"numero_invitados":120,
                "fecha_inicio":"2026-11-30","fecha_fin":"2026-11-30","hora_inicio":"15:00","hora_fin":"22:00",
                "costo_total":3500,"totalPagado":1000,"saldoPendiente":2500,
                "servicios":[{"idServicio":2,"nombreServicio":"Comida","cantidad":1,"precioContratado":200,"subtotal":200}],
                "inventario":[{"idRecurso":3,"nombreRecurso":"Silla","cantidadAsignada":120}],
                "pagos":[{"id_pago":4,"monto":1000,"fecha_pago":"2026-11-21","tipo_pago":"ANTICIPO",
                    "estadoPago":"COMPLETADO","metodo_pago":"PAYPAL"}],"check_in_realizado":false}]"""
        }
        val reserva = repo.historial().single()
        assertEquals("15:00", reserva.horaInicio)
        assertEquals("22:00", reserva.horaFin)
        assertEquals("Aguascalientes", reserva.ubicacionSalon)
        assertEquals(120, reserva.numeroInvitados)
        assertEquals("1000", reserva.totalPagado)
        assertEquals("2500", reserva.saldoPendiente)
        assertEquals("200", reserva.servicios.single().subtotal)
        assertEquals(120, reserva.inventario.single().cantidad)
        assertEquals("ANTICIPO", reserva.pagos.single().tipo)
        assertEquals("2026-11-21", reserva.pagos.single().fecha)
        assertEquals(false, reserva.checkInRealizado)
    }

    @Test fun aceptaDesdeElHistorialConPatch() = runBlocking {
        val repo = repo {
            when (it.url.encodedPath) {
                "$raiz/mis-reservaciones" -> """[${reserva("ESPERANDO_PAGO", 1000)}]"""
                "$raiz/17/estado" -> {
                    assertEquals("PATCH", it.method)
                    assertEquals("ACEPTADA_CLIENTE", cuerpo(it).get("estado").asString)
                    reserva("ACEPTADA_CLIENTE", 1000)
                }
                else -> error("Ruta inesperada: ${it.url}")
            }
        }
        val vista = repo.historial().single()
        assertEquals("ACEPTADA_CLIENTE", repo.responderCotizacion(vista, true).estado)
    }

    @Test fun rechazaSoloLaVersionRevisada() = runBlocking {
        var cambios = 0
        val repo = repo {
            when (it.url.encodedPath) {
                "$raiz/mis-reservaciones" -> """[${reserva("ESPERANDO_PAGO", 1000)}]"""
                "$raiz/17/estado" -> {
                    cambios++
                    assertEquals("PATCH", it.method)
                    assertEquals("CANCELADA_CLIENTE", cuerpo(it).get("estado").asString)
                    reserva("CANCELADA_CLIENTE", 1000)
                }
                else -> error("Ruta inesperada")
            }
        }
        val vista = repo.historial().single()
        assertTrue(runCatching { repo.responderCotizacion(vista.copy(costoTotal = "1"), true) }.isFailure)
        assertEquals(0, cambios)
        assertEquals("CANCELADA_CLIENTE", repo.responderCotizacion(vista, false).estado)
        assertEquals(1, cambios)
    }

    @Test fun creaOrdenConMontoYLeeElEnlaceDePayPal() = runBlocking {
        val repo = repo {
            when (it.url.encodedPath) {
                "$raiz/mis-reservaciones" -> """[${reserva("ACEPTADA_CLIENTE", 1000)}]"""
                "$raiz/17/paypal/crear-orden" -> {
                    assertEquals("POST", it.method)
                    assertEquals("500".toBigDecimal(), cuerpo(it).get("monto").asBigDecimal)
                    assertEquals(1, cuerpo(it).size())
                    """{"id":"ORD-7","status":"CREATED","links":[{"rel":"approve","href":"$url"}]}"""
                }
                else -> error("Ruta inesperada: ${it.url}")
            }
        }
        assertEquals(OrdenPendientePaypal("ORD-7", url, "500".toBigDecimal(), "ANTICIPO"),
            repo.iniciar(17, "500".toBigDecimal()))
    }

    @Test fun capturaYRegistraEnUnaSolaPeticionSinLlamarAlEndpointGenerico() = runBlocking {
        for ((tipo, saldo) in listOf("ANTICIPO" to 500, "LIQUIDACION" to 0)) {
            val monto = if (saldo == 0) 1000 else 500
            var enviada = false
            val repo = repo {
                when (it.url.encodedPath) {
                    "$raiz/mis-reservaciones" -> """[${reserva("ACEPTADA_CLIENTE", 1000)}]"""
                    "$raiz/17/paypal/capturar-orden" -> {
                        assertTrue(enviada)
                        assertEquals("POST", it.method)
                        assertEquals("ORD-7", cuerpo(it).get("orderId").asString)
                        assertEquals(tipo, cuerpo(it).get("tipoPago").asString)
                        respuestaPago(monto, saldo)
                    }
                    else -> error("Ruta inesperada: ${it.url}")
                }
            }
            val resultado = repo.confirmar(17, OrdenPendientePaypal("ORD-7", url, monto.toBigDecimal(), tipo)) {
                enviada = true
            }
            assertEquals("CAP-9", resultado.idTransaccionPasarela)
            assertEquals(saldo.toBigDecimal(), resultado.saldoPendiente)
            assertEquals(if (saldo == 0) "CONFIRMADA" else "ANTICIPO_PAGADO", resultado.estadoReservacion)
        }
    }

    @Test fun impideCrearOCapturarUnAnticipoAnteriorAlNuevoMinimo() = runBlocking {
        val repo = repo {
            assertEquals("$raiz/mis-reservaciones", it.url.encodedPath)
            """[${reserva("ACEPTADA_CLIENTE", 1000)}]"""
        }
        val importe = "499.99".toBigDecimal()
        val orden = OrdenPendientePaypal("ORD-7", url, importe, "ANTICIPO")
        val errorCreacion = runCatching { repo.iniciar(17, importe) }.exceptionOrNull()
        val errorCaptura = runCatching {
            repo.confirmar(17, orden) { fail("No se debe iniciar la captura") }
        }.exceptionOrNull()
        assertTrue(errorCreacion is IllegalArgumentException)
        assertTrue(errorCaptura is IllegalArgumentException)
        assertTrue(errorCaptura?.message.orEmpty().contains("500.00"))
    }

    @Test fun noCreaSiLaOfertaSigueSinAceptarOElAnticipoEsInferiorAlMinimo() = runBlocking {
        for (estado in listOf("ESPERANDO_PAGO", "ACEPTADA_CLIENTE")) {
            val repo = repo {
                assertEquals("$raiz/mis-reservaciones", it.url.encodedPath)
                """[${reserva(estado, 1000)}]"""
            }
            val error = runCatching { repo.iniciar(17, "299.99".toBigDecimal()) }.exceptionOrNull()
            assertTrue(error is IllegalStateException || error is IllegalArgumentException)
        }
    }

    @Test fun anticipoPagadoPermiteAbonarOLiquidarYQrExigeSaldoCero() {
        val parcial = gson.fromJson(reserva("ANTICIPO_PAGADO", 250), Reservacion::class.java)
        assertTrue(ReglasPagoCliente.puedePagar(parcial))
        assertEquals("ABONO", ReglasPagoCliente.tipoPago(parcial, "10".toBigDecimal()))
        assertEquals("LIQUIDACION", ReglasPagoCliente.tipoPago(parcial, "250".toBigDecimal()))
        assertNull(ReglasPagoCliente.qr(parcial))
        assertEquals("PASE-17", ReglasPagoCliente.qr(gson.fromJson(reserva("CONFIRMADA", 0), Reservacion::class.java)))
    }

    @Test fun saldoSoloCuentaPagosCompletados() {
        val reserva = gson.fromJson("""{"id_reservacion":17,"estado_reservacion":"CONFIRMADA","token_qr":"PASE-17",
            "costo_total":1000,"pagos":[{"monto":300,"estadoPago":"COMPLETADO"},{"monto":700,"estadoPago":"RECHAZADO"}]}""",
            Reservacion::class.java)
        assertEquals("700".toBigDecimal(), ReglasPagoCliente.saldo(reserva))
        assertNull(ReglasPagoCliente.qr(reserva))
    }

    @Test fun unaRespuestaActualRevocaPasesAnterioresSiPierdenElegibilidad() {
        val confirmada = gson.fromJson(reserva("CONFIRMADA", 0), Reservacion::class.java)
        val guardado = checkNotNull(confirmada.paseQr())
        assertEquals("PASE-17", guardado.token)
        val anteriores = listOf(guardado, guardado.copy(id = 18))
        val invalidas = listOf(
            confirmada.copy(estado = "CANCELADA"),
            confirmada.copy(saldoPendiente = "0.01"),
            confirmada.copy(saldoPendiente = null),
            confirmada.copy(tokenQr = null)
        )
        invalidas.forEach { actual ->
            val cambios = mapOf(actual.id to actual.paseQr())
            assertEquals(listOf(18L), combinarPasesQr(anteriores, cambios, null).map { it.id })
            assertTrue(combinarPasesQr(anteriores, cambios, setOf(actual.id)).isEmpty())
        }
        val actual = confirmada.copy(tokenQr = "PASE-ACTUALIZADO")
        assertEquals("PASE-ACTUALIZADO",
            combinarPasesQr(anteriores, mapOf(actual.id to actual.paseQr()), setOf(actual.id)).single().token)
        assertTrue(combinarPasesQr(anteriores, emptyMap(), emptySet()).isEmpty())
    }

    @Test fun noIniciaPagoSobreHttpNiAceptaEnlacesAjenosALaOrden() = runBlocking {
        val repo = repo(TransportePaypal.BLOQUEADO) { error("El checkout no debe usar un transporte bloqueado") }
        assertTrue(runCatching { repo.iniciar(17, "300".toBigDecimal()) }.isFailure)
        assertEquals(url, ReglasPagoCliente.urlAprobacion(url, "ORD-7"))
        assertNull(ReglasPagoCliente.urlAprobacion(url, "ORD-8"))
        assertNull(ReglasPagoCliente.urlAprobacion("https://www.paypal.com.ejemplo.net/checkoutnow?token=ORD-7", "ORD-7"))
    }

    @Test fun permiteSandboxConElD3ActualEnDebug() = runBlocking {
        val d3 = "http://172.30.155.214:8082/"
        val modo = TransportePaypal.paraServidor(d3, debug = true)
        assertEquals(TransportePaypal.SANDBOX_HTTP, modo)
        assertEquals(TransportePaypal.BLOQUEADO, TransportePaypal.paraServidor(d3, debug = false))
        assertEquals(TransportePaypal.BLOQUEADO, TransportePaypal.paraServidor("http://otro.test/", debug = true))
        val repo = repo(modo) {
            if (it.method == "GET") """[${reserva("ACEPTADA_CLIENTE", 1000)}]"""
            else """{"id":"ORD-7","status":"CREATED","links":[{"rel":"approve","href":"$url"}]}"""
        }
        assertEquals(url, repo.iniciar(17, "500".toBigDecimal()).enlace)
    }

    @Test fun sandboxHttpBloqueaCapturasDeOrdenGuardadaDePaypalReal() = runBlocking {
        val urlReal = "https://www.paypal.com/checkoutnow?token=ORD-7"
        val repo = repo(TransportePaypal.SANDBOX_HTTP) { error("No debe consultar ni capturar una orden real") }
        assertNull(repo.enlaceDeAprobacion(urlReal, "ORD-7"))
        val orden = OrdenPendientePaypal("ORD-7", urlReal, "300".toBigDecimal(), "ANTICIPO")
        assertTrue(runCatching { repo.confirmar(17, orden) {} }.exceptionOrNull() is IllegalStateException)
    }

    private fun reserva(estado: String, saldo: Int) = """{"id_reservacion":17,"estado_reservacion":"$estado",
        "saldoPendiente":$saldo,"costo_total":1000,"token_qr":"PASE-17"}"""

    private fun respuestaPago(monto: Int, saldo: Int) = """{"idPago":5,"idReservacion":17,"monto":$monto,
        "estadoPago":"COMPLETADO","metodoPago":"PAYPAL","idTransaccionPasarela":"CAP-9","saldoPendiente":$saldo,
        "estadoReservacion":"${if (saldo == 0) "CONFIRMADA" else "ANTICIPO_PAGADO"}","tokenQr":"PASE-17"}"""

    private fun cuerpo(request: Request) =
        JsonParser.parseString(Buffer().also { request.body!!.writeTo(it) }.readUtf8()).asJsonObject

    private fun repo(modo: TransportePaypal = TransportePaypal.HTTPS, responder: (Request) -> String): PagosClienteRepository {
        val cliente = OkHttpClient.Builder().addInterceptor {
            Response.Builder().request(it.request()).code(200).protocol(Protocol.HTTP_1_1).message("OK")
                .body(responder(it.request()).toResponseBody("application/json".toMediaType())).build()
        }.build()
        val base = if (modo == TransportePaypal.SANDBOX_HTTP) "http://172.30.155.214:8082/" else "https://d3.test/"
        val api = Retrofit.Builder().baseUrl(base).client(cliente)
            .addConverterFactory(GsonConverterFactory.create(gson)).build().create(ApiReservaciones::class.java)
        return PagosClienteRepository(api, modo) { true }
    }
}
