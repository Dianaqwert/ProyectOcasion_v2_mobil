package com.example.ocasion_mobil_v2.data.repository

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.*
import retrofit2.Response

class ErrorReservaciones(val codigo: Int, mensaje: String) : Exception(mensaje)

class ReservacionesRepository(private val api: ApiReservaciones) {
    suspend fun pendientes(): List<Reservacion> = resultado(api.bandeja())
        .filter { it.estado == EstadosReservacion.PENDIENTE }.distinctBy { it.id }

    suspend fun dictaminar(id: Long, aprobar: Boolean, motivo: String?): Reservacion {
        val destino = if (aprobar) EstadosReservacion.APROBADA else EstadosReservacion.RECHAZADA
        val rechazo = motivo?.trim()?.takeIf { it.isNotEmpty() }
        require(aprobar || rechazo != null) { "Escribe el motivo del rechazo." }
        val actualizada = resultado(api.cambiarEstado(id, CambioEstadoReservacion(destino, if (aprobar) null else rechazo)))
        check(actualizada.id == id && actualizada.estado == destino) {
            "El servidor no confirmó el estado solicitado. Actualiza la bandeja antes de volver a decidir."
        }
        return actualizada
    }

    private fun <T> resultado(response: Response<T>): T {
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw ErrorReservaciones(response.code(), when (response.code()) {
                401 -> "Tu sesión venció. Inicia sesión nuevamente."
                403 -> "Tu cuenta no tiene permiso para gestionar estas solicitudes."
                404 -> "La solicitud o la ruta del servicio ya no está disponible."
                409, 422 -> "La solicitud cambió de estado o el servidor no permite esta transición. Actualiza la bandeja."
                else -> "No se pudo completar la operación (${response.code()}). Intenta nuevamente."
            })
        }
        return response.body() ?: error("El servidor devolvió una respuesta vacía. Actualiza la bandeja.")
    }
}
