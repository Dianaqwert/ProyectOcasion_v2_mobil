package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.annotations.SerializedName

data class SalonPropietarioDTO(
    val id_salon: Int,
    val nombreSalon: String?,
    val capacidadPersonas: Int?,
    val descripcion: String?,
    val precio_hora: Double?,
    val ubicacion: UbicacionDTO?,
    val estado: String? = null,
    val verificado: Boolean = false,
    val documentosValidacionSalonEntity: ExpedientePropietarioDTO? = null
)

data class ExpedientePropietarioDTO(
    val idExpediente: Int? = null,
    val estadoRevisionAdmin: String? = null,
    val observacionesAdmin: String? = null
)

data class DocumentosGuardadosDTO(
    val id_salon: Int,
    val id_expediente: Int,
    val estadoRevisionAdmin: String?,
    val estadoSalon: String?,
    val mensaje: String?
)

data class UbicacionDTO(
    val ciudad: String?,
    // El backend serializa este campo como "CP" (mayúsculas) porque su
    // entidad Java tiene el field llamado "CP" — sin este alias, Gson no
    // lo encuentra y siempre llega null.
    @SerializedName("CP")
    val cp: String?,
    val direccion: String?,
    val latitud: Double?,
    val longitud: Double?
)
