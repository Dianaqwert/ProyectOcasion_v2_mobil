package com.example.ocasion_mobil_v2.cliente

import com.example.ocasion_mobil_v2.data.remote.model.DisponibilidadMesDTO
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.validarDisponibilidadReserva
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DisponibilidadReservaTest {
    private val fecha = LocalDate.of(2026, 11, 30)
    private val horario = HorarioSalonDTO(fecha.toString(), "09:30:00", "18:00:00")
    private val bloqueo = DisponibilidadMesDTO(fecha.toString(), "13:00:00", "14:00:00")

    @Test fun validaFechaInicialYAperturaYCierre() {
        assertNotNull(validar("10:00", "12:00", dia = fecha.minusDays(1)))
        assertNotNull(validar("09:29", "10:00"))
        assertNotNull(validar("17:00", "18:01"))
        assertNull(validar("09:30", "18:00"))
        assertNull(validar("09:30", "18:00", dia = fecha.plusDays(1)))
    }

    @Test fun respetaBloqueosParcialesSinBloquearLosIntervalosContiguos() {
        val bloqueos = listOf(bloqueo)
        assertNotNull(validar("12:00", "13:01", bloqueos))
        assertNotNull(validar("12:00", "15:00", bloqueos))
        assertNull(validar("12:00", "13:00", bloqueos))
        assertNull(validar("14:00", "15:00", bloqueos))
        assertNull(validar("13:00", "14:00", bloqueos, fecha.plusDays(1)))
    }

    @Test fun sinHorarioHabitualConservaLosBloqueos() {
        assertNotNull(validarDisponibilidadReserva(
            fecha, LocalTime.NOON, LocalTime.of(14, 0), null, listOf(bloqueo)
        ))
        assertNotNull(validarDisponibilidadReserva(
            fecha, LocalTime.NOON, LocalTime.of(14, 0), null, listOf(DisponibilidadMesDTO(fecha.toString()))
        ))
        assertNull(validarDisponibilidadReserva(
            fecha, LocalTime.NOON, LocalTime.of(14, 0), null, emptyList()
        ))
    }

    private fun validar(
        inicio: String,
        fin: String,
        bloqueos: List<DisponibilidadMesDTO> = emptyList(),
        dia: LocalDate = fecha
    ) = validarDisponibilidadReserva(dia, LocalTime.parse(inicio), LocalTime.parse(fin), horario, bloqueos)
}
