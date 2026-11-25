package com.example.ocasion_mobil_v2.ui.propietario

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.BloqueoSalonDTO
import com.example.ocasion_mobil_v2.data.remote.model.DisponibilidadInicialRequest
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.repository.CrearSalonRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Response
import java.time.LocalDateTime

class DisponibilidadSalonViewModel(application: Application) : AndroidViewModel(application) {
    data class Estado(
        val idCreado: Int? = null,
        val horario: HorarioSalonDTO? = null,
        val bloqueos: List<BloqueoSalonDTO> = emptyList(),
        val pendientes: List<DisponibilidadInicialRequest> = emptyList(),
        val cargado: Boolean = false,
        val ocupado: Boolean = false,
        val error: String? = null,
        val aviso: String? = null,
        val abrirCatalogo: Boolean = false
    )

    private val api = RetrofitClientSalones.getService(application)
    private val creador = CrearSalonRepository(application)
    private val mutable = MutableStateFlow(Estado())
    val estado = mutable.asStateFlow()
    private var iniciado = false

    fun iniciar(id: Int?, recuperado: Int?, pendientes: List<DisponibilidadInicialRequest>) {
        if (iniciado) return
        iniciado = true
        mutable.value = Estado(idCreado = recuperado, pendientes = pendientes, cargado = id == null)
        if (id != null) cargar(id)
    }

    fun cargar(id: Int) = ejecutar("No se pudo cargar la disponibilidad.") {
        val horario = api.obtenerHorarioSalon(id).validar().body()?.validar()
        val bloqueos = requireNotNull(api.obtenerBloqueosSalon(id).validar().body())
        bloqueos.forEach {
            LocalDateTime.parse(it.horaInicio)
            LocalDateTime.parse(it.horaFin)
        }
        mutable.value = mutable.value.copy(horario = horario, bloqueos = bloqueos, cargado = true)
    }

    fun guardarHorario(id: Int, horario: HorarioSalonDTO) = ejecutar("No se pudo guardar el horario.") {
        val guardado = requireNotNull(api.actualizarHorarioSalon(id, horario).validar().body()).validar()
        mutable.value = mutable.value.copy(horario = guardado, aviso = "Horario actualizado.")
    }

    fun agregarBloqueo(id: Int?, bloqueo: DisponibilidadInicialRequest) {
        val actual = mutable.value
        if (actual.ocupado) return
        if (id == null) {
            if (bloqueo in actual.pendientes) return
            mutable.value = actual.copy(pendientes = actual.pendientes + bloqueo, error = null, aviso = "Bloqueo agregado.")
            return
        }
        ejecutar("No se pudo agregar el bloqueo.") {
            val existentes = requireNotNull(api.obtenerBloqueosSalon(id).validar().body())
            val bloqueos = if (existentes.any { coincide(it, bloqueo) }) existentes
            else existentes + requireNotNull(api.registrarBloqueoSalon(id, bloqueo).validar().body())
            mutable.value = mutable.value.copy(bloqueos = bloqueos.sortedBy { it.horaInicio }, aviso = "Bloqueo guardado.")
        }
    }

    fun quitarPendiente(bloqueo: DisponibilidadInicialRequest) {
        val actual = mutable.value
        if (!actual.ocupado) mutable.value = actual.copy(pendientes = actual.pendientes - bloqueo)
    }

    fun eliminarBloqueo(id: Int, bloqueo: Int) = ejecutar("No se pudo eliminar el bloqueo.") {
        val respuesta = api.eliminarBloqueoSalon(id, bloqueo)
        if (respuesta.code() == 404) respuesta.errorBody()?.close() else respuesta.validar()
        mutable.value = mutable.value.copy(
            bloqueos = mutable.value.bloqueos.filterNot { it.id == bloqueo },
            aviso = "Bloqueo eliminado."
        )
    }

    fun crearYContinuar(request: SalonCreateFase1Request) = ejecutar("No se pudo guardar la disponibilidad.") {
        val previo = mutable.value.idCreado
        val id = previo ?: requireNotNull(creador.crear(request).validar().body()?.idSalon)
        check(id > 0)
        mutable.value = mutable.value.copy(idCreado = id)
        val existentes = if (previo != null && mutable.value.pendientes.isNotEmpty()) {
            requireNotNull(api.obtenerBloqueosSalon(id).validar().body())
        } else emptyList()
        for (bloqueo in mutable.value.pendientes) {
            if (existentes.none { coincide(it, bloqueo) }) api.registrarBloqueoSalon(id, bloqueo).validar()
            mutable.value = mutable.value.copy(pendientes = mutable.value.pendientes - bloqueo)
        }
        mutable.value = mutable.value.copy(abrirCatalogo = true)
    }

    fun consumirAviso() {
        mutable.value = mutable.value.copy(aviso = null)
    }

    private fun coincide(guardado: BloqueoSalonDTO, bloqueo: DisponibilidadInicialRequest): Boolean =
        guardado.fecha == bloqueo.fecha &&
            LocalDateTime.parse(guardado.horaInicio) == LocalDateTime.parse(bloqueo.horaInicio) &&
            LocalDateTime.parse(guardado.horaFin) == LocalDateTime.parse(bloqueo.horaFin) &&
            guardado.observaciones.orEmpty() == bloqueo.observaciones.orEmpty()

    private fun ejecutar(mensaje: String, operacion: suspend () -> Unit) {
        if (mutable.value.ocupado) return
        mutable.value = mutable.value.copy(ocupado = true, error = null, aviso = null)
        viewModelScope.launch {
            try {
                operacion()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val borrador = if (mutable.value.idCreado != null) {
                    " El borrador ya se guardó. Revisa los bloqueos pendientes y vuelve a continuar."
                } else ""
                val detalle = when (e) {
                    is ErrorDisponibilidad -> " ${e.message}"
                    is IllegalArgumentException -> e.message?.let { " $it" }.orEmpty()
                    else -> ""
                }
                mutable.value = mutable.value.copy(error = "$mensaje$detalle$borrador")
            } finally {
                mutable.value = mutable.value.copy(ocupado = false)
            }
        }
    }

    private fun <T> Response<T>.validar(): Response<T> {
        if (isSuccessful) return this
        val json = errorBody()?.use { runCatching { JSONObject(it.string()) }.getOrNull() }
        val detalle = listOf("detalle", "mensaje", "message", "detail").firstNotNullOfOrNull { campo ->
            json?.optString(campo)?.takeIf { it.isNotBlank() && it != "null" }
        }
        throw ErrorDisponibilidad(detalle ?: "Código HTTP ${code()}.")
    }

    private class ErrorDisponibilidad(mensaje: String) : Exception(mensaje)
}

