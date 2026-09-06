package com.example.ocasion_mobil_v2.data.remote

import com.example.ocasion_mobil_v2.data.remote.model.GenericMessageResponse
import com.example.ocasion_mobil_v2.data.remote.model.GoogleLoginRequest
import com.example.ocasion_mobil_v2.data.remote.model.GoogleRegisterRequest
import com.example.ocasion_mobil_v2.data.remote.model.LoginRequest
import com.example.ocasion_mobil_v2.data.remote.model.LoginResponse
import com.example.ocasion_mobil_v2.data.remote.model.RegisterRequest
import com.example.ocasion_mobil_v2.data.remote.model.RegisterResponse
import com.example.ocasion_mobil_v2.data.remote.model.ResetPasswordRequest
import com.example.ocasion_mobil_v2.data.remote.model.ValidateRegisterRequest
import com.example.ocasion_mobil_v2.data.remote.model.ValidateRegisterResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/registro")
    suspend fun registro(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/auth/registro-validar")
    suspend fun registroValidar(@Body request: ValidateRegisterRequest): Response<ValidateRegisterResponse>

    @POST("api/v1/auth/login-google")
    suspend fun loginGoogle(@Body request: GoogleLoginRequest): Response<LoginResponse>

    @POST("api/v1/auth/registro-google")
    suspend fun registroGoogle(@Body request: GoogleRegisterRequest): Response<LoginResponse>

    @POST("api/v1/auth/forgot-password")
    suspend fun solicitarResetPassword(@Query("email") email: String): Response<GenericMessageResponse>

    // 2. Enviar la nueva contraseña junto con el token
    @POST("api/v1/auth/reset-password")
    suspend fun resetPassword(@Body request: ResetPasswordRequest): Response<GenericMessageResponse>

}
