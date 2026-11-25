package com.example.ocasion_mobil_v2.data.repository

import com.example.ocasion_mobil_v2.data.remote.ApiReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import kotlinx.coroutines.CancellationException
import java.time.LocalDate
import java.time.LocalDateTime

internal sealed interface ResultadoCheckIn {
    data class Concedido(val reserva: Reservacion, val llegada: LocalDateTime) : ResultadoCheckIn
    data class Rechazado(val motivo: String) : ResultadoCheckIn
    data class SinConfirmar(val motivo: String) : ResultadoCheckIn
}

internal class CheckInPropietario(
    private val api: ApiReservaciones,
    private val sesionVigente: () -> Boolean,
    private val hoy: () -> LocalDate = LocalDate::now
) {
    suspend fun validar(token: String): ResultadoCheckIn {
        if (token.isBlank() || token.length > 512 || token.any { it.isISOControl() }) return paseDesconocido()
        if (!sesionVigente()) return sesionInvalida()
        val respuesta = try {
            api.bandeja(estado = null)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return ResultadoCheckIn.SinConfirmar("No pudimos consultar la reserva. Revisa tu conexión y escanea de nuevo.")
        }
        respuesta.errorBody()?.close()
        if (!sesionVigente() || respuesta.code() in listOf(401, 403)) return sesionInvalida()
        val reservas = respuesta.body()
            ?: return ResultadoCheckIn.SinConfirmar("D3 no pudo consultar las reservas de tus salones. Intenta de nuevo.")
        val reserva = reservas.distinctBy { it.id }.singleOrNull { it.tokenQr == token }
            ?: return paseDesconocido()
        val motivo = motivoRechazo(reserva)
        if (motivo != null) return ResultadoCheckIn.Rechazado(motivo)
        if (!sesionVigente()) return sesionInvalida()
        return registrar(token, reserva)
    }

    private fun motivoRechazo(reserva: Reservacion): String? {
        val saldo = ReglasPagoCliente.saldo(reserva) ?: return "D3 no informó el saldo de la reserva."
        if (saldo.signum() > 0) return "La reserva tiene un saldo pendiente."
        if (reserva.estado != "CONFIRMADA") return "La reserva todavía no está confirmada."
        val inicio = reserva.fechaInicio?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val fin = (reserva.fechaFin ?: reserva.fechaInicio)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        return when {
            inicio == null || fin == null || fin < inicio -> "D3 no informó fechas válidas para el evento."
            hoy() !in inicio..fin -> "El pase corresponde a otra fecha del evento."
            reserva.checkInRealizado == true -> "La llegada de esta reserva ya fue registrada."
            else -> null
        }
    }

    private suspend fun registrar(token: String, reserva: Reservacion): ResultadoCheckIn {
        val respuesta = try {
            api.registrarCheckIn(token)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            return sinConfirmarRegistro()
        }
        respuesta.errorBody()?.close()
        if (!sesionVigente() || respuesta.code() in listOf(401, 403)) return sesionInvalida()
        return when (respuesta.code()) {
            200 -> {
                val datos = respuesta.body()
                val llegada = datos?.fechaCheckIn?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
                if (datos?.idReservacion == reserva.id && llegada != null) {
                    ResultadoCheckIn.Concedido(reserva, llegada)
                } else sinConfirmarRegistro()
            }
            400, 422 -> ResultadoCheckIn.Rechazado("D3 rechazó el acceso. Verifica la fecha, el estado y el saldo.")
            404 -> paseDesconocido()
            409 -> ResultadoCheckIn.Rechazado("La llegada de esta reserva ya fue registrada.")
            else -> sinConfirmarRegistro()
        }
    }

    private fun paseDesconocido() =
        ResultadoCheckIn.Rechazado("El QR no coincide con un pase emitido para tus salones.")
    private fun sesionInvalida() =
        ResultadoCheckIn.SinConfirmar("D3 no autorizó el check-in. Vuelve a iniciar sesión como propietario.")
    private fun sinConfirmarRegistro() =
        ResultadoCheckIn.SinConfirmar("D3 no confirmó el registro. Comprueba la llegada antes de conceder acceso.")
}
