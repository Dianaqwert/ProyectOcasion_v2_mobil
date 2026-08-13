package com.example.ocasion_mobil_v2.data.remote.model

data class LoginResponse(
    val token: String
)

/**
 * Estructura estandarizada de error que devuelve el backend
 * cuando falla la validación (según el criterio de aceptación:
 * "estructura JSON estandarizada con el código y detalle del error").
 *
 * Ajusta los nombres de los campos si tu backend los llama distinto
 * (ej. "message" en vez de "detalle", "status" en vez de "codigo", etc.)
 */
data class ApiErrorResponse(
    val codigo: String? = null,
    val detalle: String? = null,
    val message: String? = null
)
