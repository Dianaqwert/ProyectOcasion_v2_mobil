package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.annotations.SerializedName

data class SalonPageResponse(
    val content: List<SalonCardClienteDTO> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val number: Int = 0,
    val last: Boolean = totalPages == 0 || number >= totalPages - 1
)

data class SalonCardClienteDTO(
    @SerializedName("id_salon") val idSalon: Int,
    @SerializedName("nombreSalon") val nombre: String? = null,
    @SerializedName("capacidadPersonas") val capacidad: Int? = null,
    @SerializedName("precio_hora") val precioHora: Double? = null,
    @SerializedName("imagen_portada") val imagenPortada: String? = null,
    val ciudad: String? = null,
    val direccion: String? = null,
    @SerializedName("serviciosDestacados") val servicios: List<String>? = emptyList(),
    val verificado: Boolean = false
)

data class SalonDetalleClienteDTO(
    @SerializedName("id_salon") val idSalon: Int,
    @SerializedName("nombreSalon") val nombreSalon: String? = null,
    val descripcion: String? = null,
    @SerializedName("capacidadPersonas") val capacidadPersonas: Int? = null,
    @SerializedName("precio_hora") val precioHora: Double? = null,
    @SerializedName("galeriaFotos") val galeriaFotos: List<String>? = null,
    val latitud: Double? = null,
    val longitud: Double? = null,
    val direccion: String? = null,
    val ciudad: String? = null,
    @SerializedName("serviciosCompletos") val serviciosCompletos: List<String>? = null,
    val verificado: Boolean = false
)

data class DisponibilidadMesDTO(
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String? = null,
    @SerializedName("hora_fin") val horaFin: String? = null,
    @SerializedName("tipo_bloqueo") val tipoBloqueo: String? = null
)
