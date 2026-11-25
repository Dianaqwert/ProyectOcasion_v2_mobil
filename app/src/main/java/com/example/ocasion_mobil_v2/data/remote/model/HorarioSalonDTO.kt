package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.annotations.SerializedName

data class HorarioSalonDTO(
    val fechaDesde: String,
    val horaInicio: String,
    val horaFin: String
) {
    fun validar(): HorarioSalonDTO {
        java.time.LocalDate.parse(fechaDesde)
        require(java.time.LocalTime.parse(horaFin).isAfter(java.time.LocalTime.parse(horaInicio))) {
            "El horario del salón es inválido."
        }
        return this
    }

    fun comoDisponibilidadInicial() = DisponibilidadInicialRequest(
        horaInicio = "${fechaDesde}T$horaInicio",
        horaFin = "${fechaDesde}T$horaFin",
        fecha = fechaDesde
    )
}

data class BloqueoSalonDTO(
    @SerializedName("id_disponibilidad") val id: Int,
    val fecha: String,
    @SerializedName("hora_inicio") val horaInicio: String,
    @SerializedName("hora_fin") val horaFin: String,
    val observaciones: String?
)
