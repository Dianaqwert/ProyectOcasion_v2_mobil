package com.example.ocasion_mobil_v2.data.remote.model

import java.math.BigDecimal

data class CrearOrdenPaypal(val monto: BigDecimal)
data class CapturarOrdenPaypal(val orderId: String, val tipoPago: String)
data class EnlacePaypal(val rel: String?, val href: String?)
data class OrdenPaypal(val id: String?, val status: String?, val links: List<EnlacePaypal>?)

data class OrdenPendientePaypal(
    val id: String,
    val enlace: String,
    val monto: BigDecimal,
    val tipoPago: String,
    val capturaSolicitada: Boolean = false
)

data class PagoRegistrado(
    val idPago: Long?,
    val idReservacion: Long?,
    val estadoPago: String?,
    val metodoPago: String?,
    val idTransaccionPasarela: String?,
    val monto: BigDecimal?,
    val saldoPendiente: BigDecimal?,
    val estadoReservacion: String?,
    val tokenQr: String?,
    val totalPagado: BigDecimal? = null
)
