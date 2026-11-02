package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.annotations.SerializedName

// Una tarjeta del carrusel de la pantalla principal.
data class SalonCardClienteDTO(
    @SerializedName("id_salon")
    val idSalon: Int,

    @SerializedName("nombreSalon")
    val nombre: String?,

    @SerializedName("capacidadPersonas")
    val capacidad: Int?,

    @SerializedName("precio_hora")
    val precioHora: Double?,

    @SerializedName("imagen_portada")
    val imagenPortada: String?,

    val ciudad: String?,
    val direccion: String?,

    @SerializedName("serviciosDestacados")
    val servicios: List<String>?,

    val verificado: Boolean
)

// GET /api/v1/salones/explorar devuelve una página de Spring.
// Las tarjetas vienen dentro de "content".
data class SalonPageResponse(
    val content: List<SalonCardClienteDTO>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val last: Boolean
)

// GET /api/v1/salones/{id}/detalle
data class SalonDetalleClienteDTO(
    @SerializedName("id_salon")
    val idSalon: Int,

    val nombreSalon: String?,
    val descripcion: String?,
    val capacidadPersonas: Int?,

    @SerializedName("precio_hora")
    val precioHora: Double?,

    val galeriaFotos: List<String>?,
    val serviciosCompletos: List<String>?,
    val direccion: String?,
    val ciudad: String?,
    val latitud: Double?,
    val longitud: Double?,
    val verificado: Boolean
)

// Cada registro representa un bloqueo del calendario.
data class DisponibilidadMesDTO(
    val fecha: String,

    @SerializedName("hora_inicio")
    val horaInicio: String?,

    @SerializedName("hora_fin")
    val horaFin: String?,

    @SerializedName("tipo_bloqueo")
    val tipoBloqueo: String?
)