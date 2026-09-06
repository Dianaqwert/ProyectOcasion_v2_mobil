package com.example.ocasion_mobil_v2.data.remote.model

data class ValidateRegisterRequest(
    val gmail: String,
    val codigo: String
)

/**
 * Respuesta al validar el código. Se dejan los campos opcionales porque
 * no está confirmado si el backend regresa un token (para loguear
 * directo) o solo un mensaje de confirmación.
 */
data class ValidateRegisterResponse(
    val message: String? = null,
    val token: String? = null
)
