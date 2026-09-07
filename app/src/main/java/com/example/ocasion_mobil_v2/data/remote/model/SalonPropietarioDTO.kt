package com.example.ocasion_mobil_v2.data.remote.model

data class SalonPropietarioDTO(
    val id_salon: Int,
    val nombreSalon: String?,
    val capacidadPersonas: Int?,
    val descripcion: String?,
    val precio_hora: Double?,
    val ubicacion: UbicacionDTO?
)

data class UbicacionDTO(
    val ciudad: String?,
    val cp: String?,
    val direccion: String?, // Calle y número
    val latitud: Double?,
    val longitud: Double?
)