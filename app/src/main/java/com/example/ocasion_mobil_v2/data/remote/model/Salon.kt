package com.example.ocasion_mobil_v2.data.remote.model


data class Salon(
    val id: String,
    val nombreSalon: String,
    val capacidadPersonas: Int,
    val descripcion: String,
    val precioHora: Double,

    val ciudad: String,
    val codigoPostal: String,
    val calle: String,
    val numero: String,
    val fraccionamiento: String,

    val latitud: Double,
    val longitud: Double,

    val imagenes: List<String> = emptyList()
)