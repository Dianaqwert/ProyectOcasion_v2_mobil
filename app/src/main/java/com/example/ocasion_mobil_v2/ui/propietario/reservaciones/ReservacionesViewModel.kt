package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.repository.ErrorReservaciones
import com.example.ocasion_mobil_v2.data.repository.ReservacionesRepository
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.google.gson.JsonParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

data class BandejaState(
    val reservas: List<Reservacion> = emptyList(),
    val cargando: Boolean = false,
    val procesando: Long? = null,
    val error: String? = null,
    val mensaje: String? = null,
    val sesionVencida: Boolean = false
)

class ReservacionesViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = ReservacionesRepository(RetrofitClientReservaciones.getService(app))
    private val mutable = MutableStateFlow(BandejaState())
    val state = mutable.asStateFlow()
    private val nombresSalones = mutableMapOf<Int, String>()
    private val nombresServicios = mutableMapOf<Long, String>()
    init { cargar() }
    var seleccion: Long?
        get() = saved["reservaSeleccionada"]
        set(value) { saved["reservaSeleccionada"] = value }

    fun cargar() {
        if (state.value.cargando || state.value.procesando != null) return
        mutable.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                val token = SessionManager(getApplication()).obtenerToken()
                val pendientes = repo.pendientes()
                if (token != SessionManager(getApplication()).obtenerToken()) return@launch
                mutable.update { it.copy(reservas = pendientes) }
                // D3 puede incluir sólo idSalon. El nombre se cruza con el catálogo del propietario en D2.
                if (pendientes.any { it.nombreSalon == null && it.idSalon !in nombresSalones }) {
                    try {
                        val salones = withTimeoutOrNull(5_000) { RetrofitClientSalones.getService(getApplication()).obtenerMisSalones() }
                        if (salones?.isSuccessful == true) salones.body().orEmpty().forEach { salon ->
                            salon.nombreSalon?.let { nombresSalones[salon.id_salon] = it }
                        } else salones?.errorBody()?.close()
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { /* Los IDs siguen permitiendo identificar el salón si D2 no responde. */ }
                }
                if (token != SessionManager(getApplication()).obtenerToken()) return@launch
                if (pendientes.any { r -> r.servicios.any { it.nombre == null && it.id !in nombresServicios } }) {
                    try {
                        val response = withTimeoutOrNull(5_000) { RetrofitClientSalones.getService(getApplication()).obtenerCatalogoServicios() }
                        if (response?.isSuccessful == true) response.body().orEmpty().forEach { nombresServicios[it.idServicio.toLong()] = it.nombreServicio }
                        else response?.errorBody()?.close()
                    } catch (e: CancellationException) { throw e }
                    catch (_: Exception) { /* Conserva las referencias enviadas por D3 si el catálogo no está disponible. */ }
                }
                if (token != SessionManager(getApplication()).obtenerToken()) return@launch
                val reservas = pendientes.map { r -> r.copy(nombreSalon = r.nombreSalon ?: nombresSalones[r.idSalon],
                    servicios = r.servicios.map { it.copy(nombre = it.nombre ?: nombresServicios[it.id]) }) }
                mutable.update { it.copy(reservas = reservas) }
                if (token != null) ReservacionesNotifications.publicarPendientes(getApplication(), reservas, token)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fallo(e) }
            finally { mutable.update { it.copy(cargando = false) } }
        }
    }

    fun dictaminar(id: Long, aprobar: Boolean, motivo: String? = null) {
        if (state.value.cargando || state.value.procesando != null || state.value.reservas.none { it.id == id }) return
        mutable.update { it.copy(procesando = id, error = null) }
        viewModelScope.launch {
            try {
                repo.dictaminar(id, aprobar, motivo)
                ReservacionesNotifications.quitar(getApplication(), id)
                seleccion = null
                mutable.update { s -> s.copy(reservas = s.reservas.filterNot { it.id == id },
                    mensaje = if (aprobar) "Solicitud aprobada. Ahora está esperando pago." else "Solicitud rechazada correctamente.") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { fallo(e) }
            finally { mutable.update { it.copy(procesando = null) } }
        }
    }

    fun consumirMensaje() { mutable.update { it.copy(mensaje = null) } }
    fun obtenerMotivo(): String = saved["motivo_${seleccion}"] ?: ""
    fun guardarMotivo(motivo: String) { saved["motivo_${seleccion}"] = motivo }
    private fun fallo(e: Exception) {
        mutable.update { it.copy(error = when (e) {
            is IOException -> "No se pudo conectar con reservaciones. Revisa tu conexión y vuelve a actualizar antes de repetir la acción."
            is JsonParseException -> "El servicio devolvió un formato de reservaciones incompatible."
            else -> e.message ?: "No se pudo completar la operación."
        }, sesionVencida = e is ErrorReservaciones && e.codigo == 401) }
    }
}
