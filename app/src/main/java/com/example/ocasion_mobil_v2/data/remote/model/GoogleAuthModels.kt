package com.example.ocasion_mobil_v2.data.remote.model

/** Body para POST /api/v1/auth/login-google */
data class GoogleLoginRequest(
    val token: String
)

/** Body para POST /api/v1/auth/registro-google */
data class GoogleRegisterRequest(
    val token: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val nombreUser: String,
    val curp: String,
    val telefono: String
)

// La respuesta de ambos endpoints se reutiliza de LoginResponse (ya existe):
// data class LoginResponse(val token: String)
