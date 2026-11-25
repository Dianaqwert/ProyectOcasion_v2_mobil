package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.data.remote.model.CotizacionPropietarioDTO
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.remote.model.ReservacionDeserializer
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO
import com.example.ocasion_mobil_v2.data.repository.StockCotizacion
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.CotizarState
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.junit.Assert.*
import org.junit.Test

class CotizacionD3Test {
    private val gson = GsonBuilder().registerTypeAdapter(Reservacion::class.java, ReservacionDeserializer()).create()
    private val reserva = gson.fromJson(
        """{"id_reservacion":17,"estado_reservacion":"SOLICITUD_NUEVA","costo_total":24920.00,
            "servicios":[{"idServicio":1,"nombreServicio":"Banquete","cantidad":2},
                         {"idServicio":2,"nombreServicio":"Decoración","cantidad":1}]}""",
        Reservacion::class.java
    )

    @Test fun enviaRentaBaseYPreciosUnitariosYCalculaElTotalConCantidades() {
        val estado = CotizarState(
            reserva = reserva,
            renta = "24920.00",
            verificado = true,
            preciosServicios = mapOf(1L to "125,50", 2L to "0")
        )
        assertTrue(estado.puedeEnviar)
        assertFalse(estado.copy(renta = "0").puedeEnviar)
        assertFalse(estado.copy(preciosServicios = mapOf(1L to "1.255")).puedeEnviar)
        assertEquals("25171.00".toBigDecimal(), estado.totalCotizado)
        val cotizacion = CotizacionPropietarioDTO("24920.00".toBigDecimal(), emptyList(), estado.serviciosCotizados())
        val body = JsonParser.parseString(gson.toJson(cotizacion)).asJsonObject
        assertEquals("24920.00".toBigDecimal(), body.get("precioRentaSalon").asBigDecimal)
        assertTrue(body.getAsJsonArray("inventario").isEmpty)
        val servicios = body.getAsJsonArray("servicios").map { it.asJsonObject }
        assertEquals(listOf(1L, 2L), servicios.map { it.get("idServicio").asLong })
        assertEquals(listOf("125.50".toBigDecimal(), "0".toBigDecimal()), servicios.map { it.get("precio").asBigDecimal })
    }

    @Test fun editarSoloMientrasElClienteNoAceptaNiPaga() {
        val enviada = reserva.copy(estado = "ESPERANDO_PAGO", totalPagado = "0.00")
        val formulario = CotizarState(reserva = enviada, verificado = true, renta = "1000")
        assertTrue(formulario.puedeEnviar)
        assertFalse(formulario.copy(reserva = enviada.copy(totalPagado = "500")).puedeEnviar)
        assertFalse(formulario.copy(reserva = enviada.copy(estado = "ACEPTADA_CLIENTE")).puedeEnviar)
    }

    @Test fun leeCatalogoDeSalonesDelPropietarioYLimitaCantidadAlStockLibre() {
        val salon = gson.fromJson(
            """{"id_salon":2,"inventario":[{"idRecurso":4,"nombreRecurso":"Silla","cantidadTotal":150}]}""",
            SalonPropietarioDTO::class.java
        )
        val recurso = salon.inventario!!.single()
        val stock = StockCotizacion(recurso.idRecurso!!, recurso.nombreRecurso!!, recurso.cantidadTotal!!, 50)
        val estado = CotizarState(reserva = reserva, renta = "24920.00", verificado = true, stock = listOf(stock))
        assertTrue(estado.copy(cantidades = mapOf(4L to "100")).puedeEnviar)
        assertFalse(estado.copy(cantidades = mapOf(4L to "101")).puedeEnviar)
    }
}
