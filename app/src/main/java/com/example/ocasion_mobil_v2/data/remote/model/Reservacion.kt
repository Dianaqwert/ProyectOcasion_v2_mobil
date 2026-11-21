package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.*
import java.lang.reflect.Type

object EstadosReservacion {
    const val PENDIENTE = "COTIZACION_PENDIENTE"
    const val APROBADA = "ESPERANDO_PAGO"
    const val RECHAZADA = "RECHAZADA_PROPIETARIO"
}

data class CambioEstadoReservacion(
    val estado: String,
    val motivoRechazo: String? = null
)

data class ServicioReservado(
    val id: Long?,
    val nombre: String?,
    val cantidad: Int?,
    val precio: String?
)

data class InventarioRetenido(
    val id: Long?,
    val nombre: String?,
    val cantidad: Int?
)

data class Reservacion(
    val id: Long,
    val estado: String,
    val idSalon: Int?,
    val nombreSalon: String?,
    val nombreCliente: String?,
    val correoCliente: String?,
    val telefonoCliente: String?,
    val fechaInicio: String?,
    val fechaFin: String?,
    val numeroInvitados: Int?,
    val servicios: List<ServicioReservado>,
    val inventario: List<InventarioRetenido>,
    val observaciones: String?,
    val idCliente: Long? = null,
    val costoTotal: String? = null
)

class ReservacionDeserializer : JsonDeserializer<Reservacion> {

    override fun deserialize(
        json: JsonElement,
        type: Type,
        context: JsonDeserializationContext
    ): Reservacion {

        val o = json.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw JsonParseException("Reservación inválida")

        val id = o.long("idReservacion", "id_reservacion", "id")
            ?.takeIf { it > 0 }
            ?: throw JsonParseException(
                "La reservación no contiene un identificador válido"
            )

        val estado = o.str("estado", "estado_reservacion")
            ?: throw JsonParseException(
                "La reservación no contiene estado"
            )

        val salon = o.obj("salon", "salonEntity")

        val cliente = o.obj(
            "cliente",
            "clienteEntity",
            "usuario",
            "usuarioEntity"
        )

        return Reservacion(
            id = id,
            estado = estado,

            idSalon = o.long(
                "idSalon",
                "id_salon",
                "salonid_salon",
                "Salonid_salon"
            )?.toInt()
                ?: salon?.long(
                    "idSalon",
                    "id_salon",
                    "id"
                )?.toInt(),

            nombreSalon = o.str(
                "nombreSalon",
                "nombre_salon"
            ) ?: salon?.str(
                "nombreSalon",
                "nombre_salon",
                "nombre"
            ),

            nombreCliente = o.str(
                "nombreCliente",
                "nombre_cliente"
            ) ?: cliente?.str(
                "nombreCompleto",
                "nombreCliente"
            ) ?: cliente?.let { c ->
                listOfNotNull(
                    c.str("nombres", "nombre"),
                    c.str("apellidoPaterno", "apellido_paterno"),
                    c.str("apellidoMaterno", "apellido_materno")
                )
                    .joinToString(" ")
                    .takeIf { it.isNotBlank() }
            },

            correoCliente = o.str(
                "correoCliente",
                "emailCliente"
            ) ?: cliente?.str(
                "gmail",
                "email",
                "correo"
            ),

            telefonoCliente = o.str("telefonoCliente")
                ?: cliente?.str("telefono", "celular"),

            fechaInicio = o.str("fechaInicio", "fecha_inicio"),
            fechaFin = o.str("fechaFin", "fecha_fin"),

            numeroInvitados = o.long(
                "numeroInvitados",
                "numero_invitados"
            )?.toInt(),

            servicios = o.objects(
                "servicios",
                "serviciosReservados",
                "reservacionServicios"
            ).map { s ->

                val detalle = s.obj(
                    "servicio",
                    "servicioEntity"
                )

                ServicioReservado(
                    id = s.long(
                        "idServicio",
                        "id_servicio",
                        "serviciosid_servicio",
                        "Serviciosid_servicio"
                    ) ?: detalle?.long(
                        "idServicio",
                        "id_servicio",
                        "id"
                    ),

                    nombre = s.str(
                        "nombreServicio",
                        "nombre"
                    ) ?: detalle?.str(
                        "nombreServicio",
                        "nombre"
                    ),

                    cantidad = s.long(
                        "cantidad",
                        "serviciosSolicitadosReservacion"
                    )?.toInt(),

                    precio = s.str(
                        "precioContratado",
                        "precio_contratado"
                    )
                )
            },

            inventario = o.objects(
                "inventario",
                "inventarioRetenido",
                "reservacionInventarioSalon"
            ).map { i ->

                val recurso = i.obj(
                    "recurso",
                    "recursoEntity",
                    "inventarioSalon",
                    "inventarioSalonEntity"
                )

                InventarioRetenido(
                    id = i.long(
                        "idRecurso",
                        "id_recurso",
                        "inventario_Salonid_recurso",
                        "Inventario_Salonid_recurso"
                    ) ?: recurso?.long(
                        "idRecurso",
                        "id_recurso",
                        "id"
                    ),

                    nombre = i.str(
                        "nombreRecurso",
                        "nombre"
                    ) ?: recurso?.str(
                        "nombreRecurso",
                        "nombre"
                    ),

                    cantidad = i.long(
                        "cantidadAsignada",
                        "cantidad_asignada",
                        "cantidad"
                    )?.toInt()
                )
            },

            observaciones = o.str("observaciones"),

            idCliente = o.long(
                "usuariosid_user",
                "Usuariosid_user",
                "idCliente"
            ),

            costoTotal = o.str(
                "costo_total",
                "costoTotal"
            )
        )
    }
}

private fun JsonObject.str(vararg keys: String): String? =
    keys.firstNotNullOfOrNull { key ->
        get(key)
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

private fun JsonObject.long(vararg keys: String): Long? =
    str(*keys)?.toLongOrNull()

private fun JsonObject.obj(vararg keys: String): JsonObject? =
    keys.firstNotNullOfOrNull { key ->
        get(key)
            ?.takeIf { it.isJsonObject }
            ?.asJsonObject
    }

private fun JsonObject.objects(
    vararg keys: String
): List<JsonObject> {

    val value = keys.firstNotNullOfOrNull { key ->
        get(key)?.takeUnless { it.isJsonNull }
    } ?: return emptyList()

    if (!value.isJsonArray) {
        throw JsonParseException(
            "Desglose de reservación inválido"
        )
    }

    return value.asJsonArray.map {
        it.takeIf { element -> element.isJsonObject }
            ?.asJsonObject
            ?: throw JsonParseException(
                "Elemento de desglose inválido"
            )
    }
}