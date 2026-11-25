package com.example.ocasion_mobil_v2.data.repository

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.*
import retrofit2.Response
import java.io.File
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI

enum class TransportePaypal {
    HTTPS, SANDBOX_HTTP, BLOQUEADO;

    companion object {
        fun paraServidor(baseUrl: String, debug: Boolean): TransportePaypal = when {
            baseUrl.startsWith("https://") -> HTTPS
            debug && baseUrl == "http://172.30.155.214:8082/" -> SANDBOX_HTTP
            else -> BLOQUEADO
        }
    }
}

object ReglasPagoCliente {
    const val PORCENTAJE_ANTICIPO = 50

    fun permitePdf(estado: String): Boolean =
        estado in setOf("ESPERANDO_PAGO", "ACEPTADA_CLIENTE", "ANTICIPO_PAGADO", "CONFIRMADA")

    fun monto(texto: String): BigDecimal? = texto.trim().replace(',', '.')
        .takeIf { it.matches(Regex("[0-9]{1,10}(\\.[0-9]{1,2})?")) }
        ?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 }

    fun saldo(reserva: Reservacion): BigDecimal? =
        reserva.saldoPendiente?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }

    fun puedePagar(reserva: Reservacion): Boolean =
        reserva.estado in setOf("ACEPTADA_CLIENTE", "ANTICIPO_PAGADO", "CONFIRMADA") &&
            saldo(reserva)?.signum() == 1 &&
            reserva.costoTotal?.toBigDecimalOrNull()?.signum() == 1

    fun minimo(reserva: Reservacion): BigDecimal {
        val total = reserva.costoTotal?.toBigDecimalOrNull()?.takeIf { it.signum() > 0 } ?: return BigDecimal.ZERO
        val saldo = saldo(reserva)?.takeIf { it.signum() > 0 } ?: return BigDecimal.ZERO
        val pagado = reserva.totalPagado?.toBigDecimalOrNull()?.takeIf { it.signum() >= 0 }
        val primerPago = pagado?.let { it.signum() == 0 } ?: (saldo.compareTo(total) == 0)
        val porcentaje = BigDecimal.valueOf(PORCENTAJE_ANTICIPO.toLong(), 2)
        return if (primerPago) total.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP)
            else BigDecimal.ZERO
    }

    fun tipoPago(reserva: Reservacion, importe: BigDecimal): String {
        val saldo = saldo(reserva) ?: error("D3 no informó el saldo pendiente.")
        require(importe.signum() > 0 && importe.stripTrailingZeros().scale() <= 2 && importe <= saldo) {
            "Ingresa un importe mayor a cero, con hasta dos decimales y dentro del saldo."
        }
        val minimo = minimo(reserva)
        require(importe >= minimo) { "El primer pago debe ser de al menos ${minimo.toPlainString()} MXN." }
        return when {
            importe.compareTo(saldo) == 0 -> "LIQUIDACION"
            minimo.signum() > 0 -> "ANTICIPO"
            else -> "ABONO"
        }
    }

    fun validarOrden(reserva: Reservacion, orden: OrdenPendientePaypal) {
        check(puedePagar(reserva)) { "La reserva ya no tiene un saldo que pueda pagarse." }
        check(tipoPago(reserva, orden.monto) == orden.tipoPago) {
            "El saldo de la reserva cambió. Revisa la orden antes de continuar."
        }
    }

    fun qr(reserva: Reservacion): String? = reserva.tokenQr?.takeIf {
        it.isNotBlank() && it.length <= 512 && reserva.estado == "CONFIRMADA" && saldo(reserva)?.signum() == 0
    }

    fun urlAprobacion(texto: String?, orderId: String, soloSandbox: Boolean = false): String? {
        if (texto == null) return null
        val url = runCatching { URI(texto) }.getOrNull() ?: return null
        val tokens = url.rawQuery.orEmpty().split('&').filter { it.startsWith("token=") }
        val dominio = url.host?.lowercase()
        return texto.takeIf {
            url.scheme == "https" && url.rawUserInfo == null && url.port in listOf(-1, 443) &&
                (dominio in setOf("www.sandbox.paypal.com", "sandbox.paypal.com") ||
                    (!soloSandbox && dominio == "www.paypal.com")) &&
                url.path == "/checkoutnow" && tokens == listOf("token=$orderId")
        }
    }
}

class PagosClienteRepository(
    private val api: ApiReservaciones,
    private val transporte: TransportePaypal,
    private val sesionVigente: () -> Boolean
) {
    suspend fun historial(): List<Reservacion> = consultar { api.misReservaciones() }.distinctBy { it.id }

    suspend fun detalle(id: Long): Reservacion = historial().firstOrNull { it.id == id }
        ?: error("La reservación ya no aparece en tu historial.")

    suspend fun descargarPdf(id: Long, cache: File): File {
        check(ReglasPagoCliente.permitePdf(detalle(id).estado)) {
            "El PDF está disponible cuando la cotización está lista y la reserva sigue activa."
        }
        val archivo = guardarPdfReserva(cache, consultar { api.pdf(id) })
        if (!sesionVigente()) {
            archivo.delete()
            error("La sesión cambió. Abre de nuevo Mis reservaciones.")
        }
        return archivo
    }

    suspend fun responderCotizacion(vista: Reservacion, aceptar: Boolean): Reservacion {
        val actual = detalle(vista.id)
        check(actual.estado == "ESPERANDO_PAGO" && actual.costoTotal == vista.costoTotal &&
            actual.servicios == vista.servicios && actual.inventario == vista.inventario) {
            "La cotización cambió. Actualiza y revisa el detalle antes de responder."
        }
        val destino = if (aceptar) "ACEPTADA_CLIENTE" else "CANCELADA_CLIENTE"
        return consultar { api.cambiarEstado(actual.id, CambioEstadoReservacion(destino)) }.also {
            check(it.id == actual.id && it.estado == destino) { "D3 no confirmó tu respuesta a la cotización." }
        }
    }

    suspend fun iniciar(id: Long, monto: BigDecimal): OrdenPendientePaypal {
        verificarTransporte()
        val reserva = detalle(id)
        check(ReglasPagoCliente.puedePagar(reserva)) { "Acepta la cotización y verifica que haya saldo pendiente." }
        val tipo = ReglasPagoCliente.tipoPago(reserva, monto)
        val orden = consultar { api.iniciarPaypal(id, CrearOrdenPaypal(monto)) }
        val orderId = orden.id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) }
            ?: error("D3 no devolvió el identificador de la orden PayPal.")
        val enlace = orden.links.orEmpty().firstNotNullOfOrNull {
            if (it.rel in setOf("approve", "payer-action")) enlaceDeAprobacion(it.href, orderId) else null
        }
        check(orden.status in setOf("CREATED", "PAYER_ACTION_REQUIRED") && enlace != null) {
            if (transporte == TransportePaypal.SANDBOX_HTTP) "D3 debe devolver un enlace válido de PayPal Sandbox."
            else "D3 no devolvió un enlace de aprobación válido."
        }
        return OrdenPendientePaypal(orderId, enlace, monto, tipo)
    }

    suspend fun confirmar(id: Long, orden: OrdenPendientePaypal, antesDeCapturar: suspend () -> Unit): PagoRegistrado {
        verificarTransporte()
        checkNotNull(enlaceDeAprobacion(orden.enlace, orden.id)) {
            "El enlace guardado no corresponde al entorno de pago actual."
        }
        val reserva = detalle(id)
        ReglasPagoCliente.validarOrden(reserva, orden)
        antesDeCapturar()
        return consultar { api.capturarPaypal(id, CapturarOrdenPaypal(orden.id, orden.tipoPago)) }.also { pago ->
            val saldo = pago.saldoPendiente
            val estadoEsperado = if (saldo?.signum() == 0) "CONFIRMADA" else "ANTICIPO_PAGADO"
            check((pago.idPago ?: 0) > 0 && pago.idReservacion == id && pago.estadoPago == "COMPLETADO" &&
                pago.metodoPago == "PAYPAL" && !pago.idTransaccionPasarela.isNullOrBlank() &&
                pago.monto?.compareTo(orden.monto) == 0 && saldo != null && saldo.signum() >= 0 &&
                pago.estadoReservacion == estadoEsperado) {
                "D3 devolvió un resultado de pago inesperado. Verifica el movimiento antes de pagar de nuevo."
            }
        }
    }

    fun enlaceDeAprobacion(enlace: String?, idOrden: String): String? =
        ReglasPagoCliente.urlAprobacion(enlace, idOrden, soloSandbox = transporte == TransportePaypal.SANDBOX_HTTP)

    private fun verificarTransporte() {
        check(transporte != TransportePaypal.BLOQUEADO) {
            "El checkout requiere configurar una dirección HTTPS para D3."
        }
    }

    private suspend fun <T> consultar(peticion: suspend () -> Response<T>): T {
        check(sesionVigente()) { "Vuelve a iniciar sesión." }
        val respuesta = peticion()
        respuesta.errorBody()?.close()
        if (!sesionVigente()) {
            (respuesta.body() as? okhttp3.ResponseBody)?.close()
            error("La sesión cambió. Abre de nuevo Mis reservaciones.")
        }
        if (!respuesta.isSuccessful) throw ErrorReservaciones(respuesta.code(), when (respuesta.code()) {
            401, 403 -> "D3 no autorizó esta operación. Revisa tu sesión."
            402, 422 -> "PayPal no confirmó el cobro. Revisa si aprobaste, cancelaste o si tu método fue rechazado."
            409 -> "D3 informó un conflicto. Actualiza tu historial antes de volver a pagar."
            else -> "D3 no pudo completar la operación (HTTP ${respuesta.code()})."
        })
        return respuesta.body() ?: error("D3 devolvió una respuesta vacía.")
    }
}
