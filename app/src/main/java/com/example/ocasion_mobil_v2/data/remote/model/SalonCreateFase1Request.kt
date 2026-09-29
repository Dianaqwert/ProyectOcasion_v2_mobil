package com.example.ocasion_mobil_v2.data.remote.model


/**
 * IMPORTANTE: los nombres de estos campos deben coincidir EXACTAMENTE
 * (letra por letra) con los del backend, porque Gson arma el JSON usando
 * el nombre del campo tal cual. Aquí ya calzan con SalonCreateFase1DTO.java:
 *  - precio_hora (con guion bajo, no "precioHora")
 *  - ubicacion.direccion + ubicacion.cp (no calle/numero/fraccionamiento/codigoPostal)
 *  - disponibilidadInicial (antes no se mandaba y es obligatorio en el backend)
 */

import com.google.gson.annotations.SerializedName

data class SalonResponse(
    @SerializedName("id_salon")
    val idSalon: Int,

    @SerializedName("estado")
    val estado: String? = null,

    @SerializedName("mensaje")
    val mensaje: String? = null
)

data class UbicacionRequest(
    @SerializedName("direccion")
    val direccion: String,

    @SerializedName("latitud")
    val latitud: Double,

    @SerializedName("longitud")
    val longitud: Double,

    @SerializedName("ciudad")
    val ciudad: String,

    @SerializedName("cp")
    val codigoPostal: String
) {

    // Compatibilidad con código anterior
    constructor(
        latitud: Double,
        longitud: Double,
        calle: String,
        numero: String,
        fraccionamiento: String,
        ciudad: String,
        codigoPostal: String
    ) : this(
        direccion = "$calle $numero, $fraccionamiento",
        latitud = latitud,
        longitud = longitud,
        ciudad = ciudad,
        codigoPostal = codigoPostal
    )
}

data class DisponibilidadInicialRequest(

    @SerializedName("hora_inicio")
    val horaInicio: String,

    @SerializedName("hora_fin")
    val horaFin: String,

    @SerializedName("fecha")
    val fecha: String,

    @SerializedName("observaciones")
    val observaciones: String? = null
)

data class SalonCreateFase1Request(

    @SerializedName("nombreSalon")
    val nombreSalon: String,

    @SerializedName("capacidadPersonas")
    val capacidadPersonas: Int,

    @SerializedName("precio_hora")
    val precioHora: Double,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("imagenes")
    val imagenes: List<String> = emptyList(),

    @SerializedName("ubicacion")
    val ubicacion: UbicacionRequest,

    @SerializedName("disponibilidadInicial")
    val disponibilidadInicial: DisponibilidadInicialRequest =
        DisponibilidadInicialRequest(
            horaInicio = "2000-01-01T00:00:00",
            horaFin = "2000-01-01T01:00:00",
            fecha = "2000-01-01",
            observaciones = "Registro heredado."
        )
)

data class ServicioBackend(

    @SerializedName("idServicio")
    val idServicio: Int,

    @SerializedName("nombreServicio")
    val nombreServicio: String,

    @SerializedName("descripcion")
    val descripcion: String? = null
)

data class ServicioAsignadoRequest(

    @SerializedName("id_servicio")
    val idServicio: Int,

    @SerializedName("numeroServiciosSolicitados")
    val numeroServiciosSolicitados: Int = 1
)

data class RecursoDTO(

    @SerializedName("nombre_recurso")
    val nombreRecurso: String,

    @SerializedName("cantidad_total")
    val cantidadTotal: Int
)

data class CategoriaBatchDTO(

    @SerializedName("nombreCategoria")
    val nombreCategoria: String,

    @SerializedName("recursos")
    val recursos: List<RecursoDTO>
)

data class InventarioBatchDTO(

    @SerializedName("categorias")
    val categorias: List<CategoriaBatchDTO>
)
/*package com.example.ocasion_mobil_v2.data.remote.model

>>>>>>> branchInv
data class SalonCreateFase1Request(
    val nombreSalon: String,
    val capacidadPersonas: Int,
    val precio_hora: Double,
    val descripcion: String,
    val ubicacion: UbicacionRequest,
    val disponibilidadInicial: DisponibilidadRequest
)

data class UbicacionRequest(
    val direccion: String,
    val latitud: Double,
    val longitud: Double,
    val ciudad: String,
    val cp: String
)

/**
 * hora_inicio y hora_fin van en formato ISO-8601 ("yyyy-MM-ddTHH:mm:ss"),
 * fecha en formato "yyyy-MM-dd" — así los puede parsear Jackson directo a
 * LocalDateTime/LocalDate sin configuración extra.
 */
data class DisponibilidadRequest(
    val hora_inicio: String,
    val hora_fin: String,
    val fecha: String,
    val observaciones: String? = null
)

data class SalonResponse(
    val id_salon: Int,
    val estado: String? = null,
    val mensaje: String? = null
<<<<<<< HEAD
)
=======
)*/

