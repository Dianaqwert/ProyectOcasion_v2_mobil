package com.example.ocasion_mobil_v2.data.remote.model

data class SalonCreateFase1Request(
    val nombreSalon: String,
    val capacidadPersonas: Int,
    val precioHora: Double,
    val descripcion: String,
    val ubicacion: UbicacionRequest
)

data class UbicacionRequest(
    val latitud: Double,
    val longitud: Double,
    val calle: String,
    val numero: String,
    val fraccionamiento: String,
    val ciudad: String,
    val codigoPostal: String
)

data class SalonResponse(
    val id_salon: Int,
    val mensaje: String? = null
)