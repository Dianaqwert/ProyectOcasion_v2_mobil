package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.annotations.SerializedName

data class PerfilUsuario(
    @SerializedName(value = "id_User", alternate = ["id_user"])
    val id: Int,
    @SerializedName(value = "nombres", alternate = ["nombre"])
    val nombres: String?,
    val apellidoPaterno: String?,
    val apellidoMaterno: String?,
    val nombreUser: String?,
    val gmail: String?,
    val telefono: String?,
    val curp: String?
)

data class ActualizarPerfilRequest(
    val nombre: String,
    val apellidoPaterno: String,
    val apellidoMaterno: String,
    val telefono: String
)
