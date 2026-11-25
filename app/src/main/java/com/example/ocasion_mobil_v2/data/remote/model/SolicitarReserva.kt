package com.example.ocasion_mobil_v2.data.remote.model

data class SolicitarReserva(
    val idSalon: Int,
    val fechaInicio: String,
    val fechaFin: String,
    val horaInicio: String,
    val horaFin: String,
    val numeroInvitados: Int
)

data class SolicitudEnviada(val idReservacion: Long?, val estado: String?)
