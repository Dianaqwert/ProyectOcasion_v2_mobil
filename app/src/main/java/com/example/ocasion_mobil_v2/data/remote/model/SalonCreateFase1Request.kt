package com.example.ocasion_mobil_v2.data.remote.model

/**
 * IMPORTANTE: los nombres de estos campos deben coincidir EXACTAMENTE
 * (letra por letra) con los del backend, porque Gson arma el JSON usando
 * el nombre del campo tal cual. Aquí ya calzan con SalonCreateFase1DTO.java:
 *  - precio_hora (con guion bajo, no "precioHora")
 *  - ubicacion.direccion + ubicacion.cp (no calle/numero/fraccionamiento/codigoPostal)
 *  - disponibilidadInicial (antes no se mandaba y es obligatorio en el backend)
 */
data class SalonCreateFase1Request(
    val nombreSalon: String,
    val capacidadPersonas: Int,
    val precio_hora: Double,
    val descripcion: String,
    val ubicacion: UbicacionRequest,
    val disponibilidadInicial: DisponibilidadRequest
)

data class UbicacionRequest(
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val ciudad: String,
    val cp: String
)

/**
 * hora_inicio y hora_fin van en formato ISO-8601 ("yyyy-MM-ddTHH:mm:ss"),
 * fecha en formato "yyyy-MM-dd" — así los puede parsear Jackson directo a
 * LocalDateTime/LocalDate sin configuración extra.
 */
data class DisponibilidadRequest(
    val hora_inicio: String,
    val hora_fin: String,
    val fecha: String,
    val observaciones: String? = null
)

data class SalonResponse(
    val id_salon: Int,
    val estado: String? = null,
    val mensaje: String? = null
)
