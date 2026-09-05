package com.example.ocasion_mobil_v2.ui.propietario

data class SalonFormState(

    // DATOS DEL FORMULARIO

    val nombreSalon: String = "",
    val capacidadPersonas: String = "",
    val precioHora: String = "",
    val descripcion: String = "",

    // ERRORES

    val errorNombre: String? = null,
    val errorCapacidad: String? = null,
    val errorPrecio: String? = null,
    val errorDescripcion: String? = null,
    val errorImagenes: String? = null,

    // IMÁGENES

    val cantidadImagenes: Int = 0,

    // ESTADO GENERAL

    val formularioValido: Boolean = false
)