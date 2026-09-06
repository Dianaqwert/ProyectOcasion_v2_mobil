package com.example.ocasion_mobil_v2.data.remote.model

/**
 * Respuesta genérica que solo trae un mensaje de confirmación.
 * Usado para /auth/reset-password (solicitud de envío del código).
 */
data class GenericMessageResponse(
    val message: String? = null
)
