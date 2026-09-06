package com.example.ocasion_mobil_v2.data.salon

data class SalonData (
    val id: String,
    val nombre: String,
    val imagenUrl: String,
    val capacidad: Int,
    val precioPorHora: Double,
    val ciudad: String,
    val servicios: List<String>
)