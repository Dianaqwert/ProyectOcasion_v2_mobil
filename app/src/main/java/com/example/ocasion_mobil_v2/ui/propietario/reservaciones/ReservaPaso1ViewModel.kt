package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import com.example.ocasion_mobil_v2.data.remote.model.SolicitarReserva
import com.example.ocasion_mobil_v2.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class ReservaPaso1State(
    val cargando: Boolean = false,
    val capacidadMaxima: Int? = null,
    val fechasBloqueadas: Set<LocalDate> = emptySet(),
    val fecha: LocalDate? = null,
    val horaInicio: LocalTime = LocalTime.of(15, 0),
    val horaFin: LocalTime = LocalTime.of(22, 0),
    val invitadosTexto: String = "",
    val errorCarga: String? = null,
    val errorEnvio: String? = null,
    val resumen: Boolean = false,
    val enviando: Boolean = false,
    val envioIncierto: Boolean = false,
    val folio: Long? = null,
    val horarioSalon: HorarioSalonDTO? = null
) {
    val numeroInvitados get() = invitadosTexto.toIntOrNull()
    val errorInvitados: String?
        get() {
            if (invitadosTexto.isBlank()) return null
            val cantidad = numeroInvitados
            return when {
                cantidad == null || cantidad <= 0 -> "Ingresa un número mayor a cero."
                capacidadMaxima != null && cantidad > capacidadMaxima -> "El límite es de $capacidadMaxima personas."
                else -> null
            }
        }
    val errorFecha: String? get() = fecha?.let {
        when {
            it.isBefore(LocalDate.now()) -> "Selecciona hoy o una fecha futura."
            it in fechasBloqueadas -> "La fecha está ocupada. Selecciona otra."
            else -> null
        }
    }
    val errorHorario get() = if (!horaFin.isAfter(horaInicio)) "La hora de fin debe ser posterior a la de inicio." else null
    val puedeContinuar get() = capacidadMaxima != null && !cargando && errorCarga == null &&
        fecha != null && errorFecha == null && errorHorario == null &&
        numeroInvitados != null && errorInvitados == null && !enviando && !envioIncierto && folio == null

    fun solicitud(idSalon: Int): SolicitarReserva? {
        if (!puedeContinuar || idSalon <= 0) return null
        val dia = checkNotNull(fecha).toString()
        return SolicitarReserva(idSalon, dia, dia, horaInicio.toString(), horaFin.toString(), checkNotNull(numeroInvitados))
    }
}

class ReservaPaso1ViewModel(application: Application, private val saved: SavedStateHandle) : AndroidViewModel(application) {
    private val api = RetrofitClientReservaciones.getService(application)
    private val apiSalones = RetrofitClientSalones.getService(application)
    private val sesion = SessionManager(application)
    private val _state = MutableStateFlow(ReservaPaso1State(
        fecha = saved.get<String>("fecha")?.let(LocalDate::parse),
        horaInicio = saved.get<String>("desde")?.let(LocalTime::parse) ?: LocalTime.of(15, 0),
        horaFin = saved.get<String>("hasta")?.let(LocalTime::parse) ?: LocalTime.of(22, 0),
        invitadosTexto = saved["invitados"] ?: "",
        resumen = saved["resumen"] ?: false,
        envioIncierto = saved["solicitudEnCurso"] ?: false,
        folio = saved["folio"]
    ))
    val state = _state.asStateFlow()
    private var idSalonActual: Int? = null
    private var cargaHorario: Job? = null

    fun iniciar(idSalon: Int) {
        if (idSalonActual == idSalon) return
        idSalonActual = idSalon
        if (state.value.folio == null) cargarCalendario()
    }

    fun cargarCalendario() {
        val idSalon = idSalonActual ?: return
        if (state.value.cargando || state.value.enviando) return
        cargarHorario(idSalon)
        _state.update { it.copy(cargando = true, errorCarga = null) }
        viewModelScope.launch {
            try {
                val respuesta = api.obtenerCalendario(idSalon)
                val capacidad = checkNotNull(respuesta.capacidadMaximaPersonas)
                check(capacidad > 0)
                val bloqueos = checkNotNull(respuesta.fechasBloqueadas).map(LocalDate::parse).toSet()
                _state.update { it.copy(capacidadMaxima = capacidad, fechasBloqueadas = bloqueos) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                _state.update { it.copy(errorCarga = "No pudimos cargar la disponibilidad.") }
            } finally { _state.update { it.copy(cargando = false) } }
        }
    }

    private fun cargarHorario(idSalon: Int) {
        if (cargaHorario?.isActive == true) return
        cargaHorario = viewModelScope.launch {
            val horario = try {
                val detalle = apiSalones.obtenerDetalleCliente(idSalon)
                detalle.errorBody()?.close()
                detalle.body()?.horarioAtencion?.validar()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                return@launch
            }
            if (idSalonActual == idSalon) _state.update { it.copy(horarioSalon = horario) }
        }
    }

    fun cambiarInvitados(texto: String) {
        saved["invitados"] = texto
        _state.update { it.copy(invitadosTexto = texto) }
    }

    fun seleccionarFecha(milisegundos: Long) {
        val fecha = FechasReserva.desdeCalendario(milisegundos)
        saved["fecha"] = fecha.toString()
        _state.update { it.copy(fecha = fecha) }
    }

    fun seleccionarHora(esInicio: Boolean, hora: LocalTime) {
        saved[if (esInicio) "desde" else "hasta"] = hora.toString()
        _state.update { if (esInicio) it.copy(horaInicio = hora) else it.copy(horaFin = hora) }
    }

    fun mostrarResumen(mostrar: Boolean) {
        if (state.value.enviando || (mostrar && !state.value.puedeContinuar)) return
        saved["resumen"] = mostrar
        _state.update { it.copy(resumen = mostrar, errorEnvio = null) }
    }

    fun enviar() {
        val actual = state.value
        if (!actual.resumen) return
        val solicitud = actual.solicitud(idSalonActual ?: return) ?: return
        val token = sesion.obtenerToken()
        if (token == null) {
            _state.update { it.copy(errorEnvio = "Inicia sesión como cliente para enviar la solicitud.") }
            return
        }
        _state.update { it.copy(enviando = true, errorEnvio = null) }
        viewModelScope.launch {
            try {
                val errorDisponibilidad = consultarDisponibilidad(solicitud)
                if (errorDisponibilidad != null) {
                    _state.update { it.copy(errorEnvio = errorDisponibilidad) }
                    return@launch
                }
                if (token != sesion.obtenerToken()) {
                    _state.update { it.copy(errorEnvio = "La sesión cambió. Vuelve a iniciar sesión para reservar.") }
                    return@launch
                }
                saved["solicitudEnCurso"] = true
                val respuesta = api.solicitar(solicitud)
                respuesta.errorBody()?.close()
                if (token != sesion.obtenerToken()) {
                    marcarEnvioIncierto()
                    return@launch
                }
                when (respuesta.code()) {
                    201 -> {
                        val enviada = respuesta.body()
                        val folio = enviada?.idReservacion?.takeIf { it > 0 }
                        if (folio != null && enviada?.estado == "SOLICITUD_NUEVA") {
                            saved["folio"] = folio
                            saved["solicitudEnCurso"] = false
                            _state.update { it.copy(folio = folio) }
                        } else marcarEnvioIncierto()
                    }
                    409 -> {
                        saved["solicitudEnCurso"] = false
                        _state.update {
                            it.copy(errorEnvio = "El horario ya no está disponible. Elige otra fecha u horario.",
                                fecha = null)
                        }
                        saved.set<String>("fecha", null)
                    }
                    400, 401, 403, 404, 422 -> {
                        saved["solicitudEnCurso"] = false
                        val aviso = when (respuesta.code()) {
                            401, 403 -> "D3 rechazó la sesión. Vuelve a iniciar sesión como cliente."
                            404 -> "El salón ya no está disponible."
                            else -> "D3 rechazó los datos. Revisa la fecha, el horario y la capacidad del salón."
                        }
                        _state.update { it.copy(errorEnvio = aviso) }
                    }
                    else -> marcarEnvioIncierto()
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (saved.get<Boolean>("solicitudEnCurso") == true) marcarEnvioIncierto()
                else _state.update { it.copy(errorEnvio = "No pudimos verificar la disponibilidad del salón. Intenta de nuevo.") }
            }
            finally { _state.update { it.copy(enviando = false) } }
        }
    }

    private suspend fun consultarDisponibilidad(solicitud: SolicitarReserva): String? {
        val fecha = LocalDate.parse(solicitud.fechaInicio)
        val detalle = apiSalones.obtenerDetalleCliente(solicitud.idSalon)
        detalle.errorBody()?.close()
        if (detalle.code() == 404) return "El salón ya no está disponible."
        val horario = requireNotNull(detalle.body()).horarioAtencion?.validar()

        val calendario = apiSalones.obtenerCalendarioCliente(solicitud.idSalon, fecha.monthValue, fecha.year)
        calendario.errorBody()?.close()
        if (calendario.code() == 404) return "El salón ya no está disponible."
        return validarDisponibilidadReserva(
            fecha,
            LocalTime.parse(solicitud.horaInicio),
            LocalTime.parse(solicitud.horaFin),
            horario,
            requireNotNull(calendario.body())
        )
    }

    private fun marcarEnvioIncierto() {
        _state.update { it.copy(envioIncierto = true) }
    }
}
