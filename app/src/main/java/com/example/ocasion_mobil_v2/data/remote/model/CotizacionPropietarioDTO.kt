package com.example.ocasion_mobil_v2.data.remote.model

import java.math.BigDecimal

data class CalendarioReservaDTO(
    val capacidadMaximaPersonas: Int?,
    val fechasBloqueadas: List<String>?
)

data class RecursoCatalogoDTO(
    val idRecurso: Long?,
    val nombreRecurso: String?,
    val cantidadTotal: Int?
)

data class RecursoOcupadoDTO(
    val idRecurso: Long?,
    val cantidadOcupada: Int?
)

data class RecursoCotizadoDTO(
    val idRecurso: Long,
    val cantidadAsignada: Int
)

data class ServicioCotizadoDTO(
    val idServicio: Long,
    val precioContratado: BigDecimal,
    val cantidad: Int
)

data class CotizacionPropietarioDTO(
    val precioRentaSalon: BigDecimal,
    val servicios: List<ServicioCotizadoDTO>,
    val inventario: List<RecursoCotizadoDTO>,
    val observaciones: String?
)