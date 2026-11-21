package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ReservaPaso1State(
    val cargando: Boolean = false,
    val calendarioCargado: Boolean = false,
    val capacidadMaxima: Int = 0,
    val fechasBloqueadas: Set<LocalDate> = emptySet(),
    val inicio: LocalDate? = null,
    val fin: LocalDate? = null,
    val invitadosTexto: String = "",
    val errorCarga: String? = null,
    val errorFechas: String? = null
) {
    val numeroInvitados: Int?
        get() = invitadosTexto.toIntOrNull()

    val errorInvitados: String?
        get() {
            if (invitadosTexto.isBlank()) return null

            val invitados = numeroInvitados

            return when {
                invitados == null || invitados <= 0 ->
                    "Ingresa un número mayor a cero."

                calendarioCargado && invitados > capacidadMaxima ->
                    "El límite del salón es de $capacidadMaxima personas"

                else -> null
            }
        }

    val rangoValido: Boolean
        get() {
            val desde = inicio ?: return false
            val hasta = fin ?: return false
            val hoy = LocalDate.now()

            return !desde.isBefore(hoy) &&
                    hasta.isAfter(hoy) &&
                    !hasta.isBefore(desde) &&
                    fechasBloqueadas.none { fecha ->
                        !fecha.isBefore(desde) && !fecha.isAfter(hasta)
                    }
        }

    val puedeContinuar: Boolean
        get() {
            val invitados = numeroInvitados ?: return false

            return calendarioCargado &&
                    !cargando &&
                    errorCarga == null &&
                    rangoValido &&
                    invitados > 0 &&
                    invitados <= capacidadMaxima
        }
}

class ReservaPaso1ViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ReservaPaso1State())
    val state = _state.asStateFlow()

    private var idSalonActual: Int? = null
    private var consulta: Job? = null

    fun iniciar(idSalon: Int) {
        if (idSalonActual == idSalon) return

        idSalonActual = idSalon
        cargarCalendario()
    }

    fun cargarCalendario() {
        val idSalon = idSalonActual ?: return

        consulta?.cancel()

        _state.update {
            it.copy(
                cargando = true,
                calendarioCargado = false,
                inicio = null,
                fin = null,
                errorCarga = null,
                errorFechas = null
            )
        }

        consulta = viewModelScope.launch {
            try {
                require(idSalon > 0)

                val respuesta = RetrofitClientReservaciones
                    .getService(getApplication())
                    .obtenerCalendario(idSalon)

                val capacidad = requireNotNull(
                    respuesta.capacidadMaximaPersonas
                )

                require(capacidad > 0)

                // Si faltan los bloqueos en la respuesta, no asumimos
                // que todas las fechas están disponibles.
                val bloqueos = requireNotNull(respuesta.fechasBloqueadas)
                    .map { LocalDate.parse(it) }
                    .toSet()

                _state.update {
                    it.copy(
                        cargando = false,
                        calendarioCargado = true,
                        capacidadMaxima = capacidad,
                        fechasBloqueadas = bloqueos
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        cargando = false,
                        calendarioCargado = false,
                        errorCarga = "No pudimos cargar la disponibilidad"
                    )
                }
            }
        }
    }

    fun cambiarInvitados(texto: String) {
        _state.update {
            it.copy(invitadosTexto = texto)
        }
    }

    fun seleccionarFechas(inicioMillis: Long, finMillis: Long) {
        val inicio = FechasReserva.desdeCalendario(inicioMillis)
        val fin = FechasReserva.desdeCalendario(finMillis)
        val hoy = LocalDate.now()

        val error = when {
            inicio.isBefore(hoy) ->
                "La fecha de inicio no puede estar en el pasado."

            !fin.isAfter(hoy) ->
                "La fecha final debe ser posterior a hoy."

            fin.isBefore(inicio) ->
                "La fecha final no puede ser anterior a la inicial."

            _state.value.fechasBloqueadas.any {
                !it.isBefore(inicio) && !it.isAfter(fin)
            } ->
                "El rango incluye días ocupados. Selecciona otras fechas."

            else -> null
        }

        _state.update {
            it.copy(
                inicio = if (error == null) inicio else null,
                fin = if (error == null) fin else null,
                errorFechas = error
            )
        }
    }

    fun revalidarFechas() {
        val actual = _state.value

        if (actual.inicio != null && !actual.rangoValido) {
            _state.update {
                it.copy(
                    inicio = null,
                    fin = null,
                    errorFechas = "Selecciona nuevamente las fechas."
                )
            }
        }
    }
}