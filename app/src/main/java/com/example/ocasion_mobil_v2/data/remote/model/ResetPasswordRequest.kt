package com.example.ocasion_mobil_v2.data.remote.model

data class ResetPasswordRequest(
    val token: String,
    val nuevaPassword: String
)