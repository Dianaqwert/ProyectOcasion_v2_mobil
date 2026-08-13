package com.example.ocasion_mobil_v2.data.remote

import com.example.ocasion_mobil_v2.data.remote.model.LoginRequest
import com.example.ocasion_mobil_v2.data.remote.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    // Aquí se irán agregando el resto de endpoints que ya tienes en Postman:
    // GET  api/v1/usuarios          -> obtener usuario
    // POST api/v1/usuarios/registro -> registro
    // etc.
}
