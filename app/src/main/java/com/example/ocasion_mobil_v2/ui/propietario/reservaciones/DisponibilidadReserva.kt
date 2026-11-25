package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import com.example.ocasion_mobil_v2.data.remote.model.DisponibilidadMesDTO
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

internal fun validarDisponibilidadReserva(
    fecha: LocalDate,
    inicio: LocalTime,
    fin: LocalTime,
    horario: HorarioSalonDTO?,
    bloqueos: List<DisponibilidadMesDTO>
): String? {
    if (horario != null) {
        val desde = LocalDate.parse(horario.fechaDesde)
        val apertura = LocalTime.parse(horario.horaInicio)
        val cierre = LocalTime.parse(horario.horaFin)
        if (fecha.isBefore(desde)) {
            return "El salón está disponible a partir del ${desde.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}."
        }
        if (inicio.isBefore(apertura) || fin.isAfter(cierre)) {
            val formato = DateTimeFormatter.ofPattern("HH:mm")
            return "Elige un horario entre las ${apertura.format(formato)} y las ${cierre.format(formato)}."
        }
    }
    val coincide = bloqueos.filter { LocalDate.parse(it.fecha) == fecha }.any { bloqueo ->
        val desde = bloqueo.horaInicio?.let(LocalTime::parse)
        val hasta = bloqueo.horaFin?.let(LocalTime::parse)
        desde == null || hasta == null || !hasta.isAfter(desde) ||
            (inicio.isBefore(hasta) && fin.isAfter(desde))
    }
    return if (coincide) "El horario coincide con un bloqueo del salón. Elige otro intervalo." else null
}
