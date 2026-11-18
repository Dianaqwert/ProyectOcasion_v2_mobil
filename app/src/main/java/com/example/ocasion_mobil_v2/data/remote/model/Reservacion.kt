package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.*
import java.lang.reflect.Type

object EstadosReservacion {
    const val PENDIENTE = "COTIZACION_PENDIENTE"
    const val APROBADA = "ESPERANDO_PAGO"
    const val RECHAZADA = "RECHAZADA_PROPIETARIO"
}

data class CambioEstadoReservacion(val estado: String, val motivoRechazo: String? = null)
data class ServicioReservado(val id: Long?, val nombre: String?, val cantidad: Int?, val precio: String?)
data class InventarioRetenido(val id: Long?, val nombre: String?, val cantidad: Int?)
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
    val observaciones: String?
)

/** D3 entrega entidades. Acepta camelCase/snake_case y referencias anidadas;
 * nunca trata el inventario ocupado por OTROS eventos como mobiliario de esta reserva. */
class ReservacionDeserializer : JsonDeserializer<Reservacion> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): Reservacion {
        val o = json.takeIf { it.isJsonObject }?.asJsonObject
            ?: throw JsonParseException("Reservación inválida")
        val id = o.long("idReservacion", "id_reservacion", "id")?.takeIf { it > 0 }
            ?: throw JsonParseException("La reservación no contiene un identificador válido")
        val estado = o.str("estado") ?: throw JsonParseException("La reservación no contiene estado")
        val salon = o.obj("salon", "salonEntity")
        val cliente = o.obj("cliente", "clienteEntity", "usuario", "usuarioEntity")
        return Reservacion(
            id, estado, o.long("idSalon", "id_salon")?.toInt() ?: salon?.long("idSalon", "id_salon", "id")?.toInt(),
            o.str("nombreSalon", "nombre_salon") ?: salon?.str("nombreSalon", "nombre_salon", "nombre"),
            o.str("nombreCliente", "nombre_cliente") ?: cliente?.str("nombreCompleto", "nombreCliente")
                ?: cliente?.let { c -> listOfNotNull(c.str("nombres", "nombre"), c.str("apellidoPaterno", "apellido_paterno"), c.str("apellidoMaterno", "apellido_materno")).joinToString(" ").takeIf { it.isNotBlank() } },
            o.str("correoCliente", "emailCliente") ?: cliente?.str("gmail", "email", "correo"),
            o.str("telefonoCliente") ?: cliente?.str("telefono", "celular"),
            o.str("fechaInicio", "fecha_inicio"), o.str("fechaFin", "fecha_fin"),
            o.long("numeroInvitados", "numero_invitados")?.toInt(),
            o.objects("servicios", "serviciosReservados", "reservacionServicios").map { s ->
                val detalle = s.obj("servicio", "servicioEntity")
                ServicioReservado(s.long("idServicio", "id_servicio") ?: detalle?.long("idServicio", "id_servicio", "id"),
                    s.str("nombreServicio", "nombre") ?: detalle?.str("nombreServicio", "nombre"),
                    s.long("cantidad")?.toInt(), s.str("precioContratado", "precio_contratado"))
            },
            o.objects("inventario", "inventarioRetenido", "reservacionInventarioSalon").map { i ->
                val recurso = i.obj("recurso", "recursoEntity", "inventarioSalon", "inventarioSalonEntity")
                InventarioRetenido(i.long("idRecurso", "id_recurso") ?: recurso?.long("idRecurso", "id_recurso", "id"),
                    i.str("nombreRecurso", "nombre") ?: recurso?.str("nombreRecurso", "nombre"),
                    i.long("cantidadAsignada", "cantidad_asignada", "cantidad")?.toInt())
            }, o.str("observaciones")
        )
    }
}

private fun JsonObject.str(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
    get(key)?.takeIf { it.isJsonPrimitive }?.asString?.trim()?.takeIf { it.isNotEmpty() }
}
private fun JsonObject.long(vararg keys: String): Long? = str(*keys)?.toLongOrNull()
private fun JsonObject.obj(vararg keys: String): JsonObject? = keys.firstNotNullOfOrNull { get(it)?.takeIf { e -> e.isJsonObject }?.asJsonObject }
private fun JsonObject.objects(vararg keys: String): List<JsonObject> {
    val value = keys.firstNotNullOfOrNull { get(it)?.takeUnless { e -> e.isJsonNull } } ?: return emptyList()
    if (!value.isJsonArray) throw JsonParseException("Desglose de reservación inválido")
    return value.asJsonArray.map { it.takeIf { e -> e.isJsonObject }?.asJsonObject ?: throw JsonParseException("Elemento de desglose inválido") }
}
