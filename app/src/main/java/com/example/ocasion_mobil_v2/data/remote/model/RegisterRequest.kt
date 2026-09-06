package com.example.ocasion_mobil_v2.data.remote.model

data class RegisterRequest(
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombreUser: String,
    val curp: String,
    val gmail: String,
    val telefono: String,
    val password: String
)

/**
 * Respuesta genérica para registro. Se dejan los campos como nulos/opcionales
 * porque no tenemos confirmado el formato exacto que devuelve el backend
 * (podría regresar un mensaje, un token, o ambos).
 */
data class RegisterResponse(
    val message: String? = null,
    val token: String? = null
)
