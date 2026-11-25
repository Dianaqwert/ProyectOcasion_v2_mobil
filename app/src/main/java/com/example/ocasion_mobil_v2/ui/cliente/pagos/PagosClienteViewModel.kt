package com.example.ocasion_mobil_v2.ui.cliente.pagos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.BuildConfig
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.remote.model.OrdenPendientePaypal
import com.example.ocasion_mobil_v2.data.remote.model.MovimientoReserva
import com.example.ocasion_mobil_v2.data.repository.*
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.util.JwtUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.File
import java.math.BigDecimal

data class PagosClienteState(
    val reservas: List<Reservacion> = emptyList(),
    val pases: List<PaseQr> = emptyList(),
    val pasesGuardados: Set<Long> = emptySet(),
    val detalle: Reservacion? = null,
    val cargando: Boolean = false,
    val aviso: String? = null,
    val orden: OrdenPendientePaypal? = null,
    val descargando: Long? = null,
    val documento: File? = null
)

class PagosClienteViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val sesion = SessionManager(app)
    private val token = sesion.obtenerToken()
    private val cuenta = JwtUtils.obtenerIdCliente(token)
    val transporte = TransportePaypal.paraServidor(BuildConfig.RESERVACIONES_BASE_URL, BuildConfig.DEBUG)
    private val repo = PagosClienteRepository(RetrofitClientReservaciones.getService(app), transporte, ::sesionVigente)
    fun enlacePaypal(orden: OrdenPendientePaypal) = repo.enlaceDeAprobacion(orden.enlace, orden.id)
    private val cache = cuenta?.let { CacheQrCliente(app, it) }
    private val ordenes = cuenta?.let { OrdenesPaypalLocales(app, it, BuildConfig.RESERVACIONES_BASE_URL) }
    private val mutable = MutableStateFlow(PagosClienteState())
    val state = mutable.asStateFlow()
    val seleccion: Long? get() = saved["reservaPago"]
    private var leyoCache = false

    fun sesionVigente() = cuenta != null && token == sesion.obtenerToken()

    init { actualizar() }

    fun actualizar() = ejecutar {
        if (!leyoCache) {
            leyoCache = true
            try {
                val pases = checkNotNull(cache).leer()
                mutable.update { it.copy(pases = pases, pasesGuardados = pases.map { pase -> pase.id }.toSet()) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update { it.copy(aviso = "No pudimos leer los pases guardados.") }
            }
        }
        val id = seleccion
        val reservas = HistorialCliente.ordenar(repo.historial())
        val detalle = reservas.firstOrNull { it.id == id }
        actualizarPases(reservas, reservas.map { it.id }.toSet()) {
            it.copy(
                reservas = reservas,
                detalle = detalle,
                aviso = if (id != null && detalle == null) "La reservación ya no aparece en tu historial." else null
            )
        }
        if (detalle != null) cargarOrden(detalle)
    }

    fun seleccionar(id: Long) {
        if (state.value.cargando || !sesionVigente()) return
        saved["reservaPago"] = id
        mutable.update { estado -> estado.copy(detalle = estado.reservas.firstOrNull { it.id == id }, orden = null) }
        actualizar()
    }

    fun volver(): Boolean {
        if (seleccion == null || state.value.cargando) return false
        saved["reservaPago"] = null
        mutable.update { it.copy(detalle = null, orden = null) }
        actualizar()
        return true
    }

    fun descargarPdf(id: Long) {
        val actual = state.value
        if (!sesionVigente() || actual.descargando != null || actual.documento != null || actual.cargando ||
            actual.reservas.none { it.id == id && ReglasPagoCliente.permitePdf(it.estado) }) return

        mutable.update { it.copy(descargando = id, aviso = null) }
        viewModelScope.launch {
            try {
                val archivo = repo.descargarPdf(id, getApplication<Application>().cacheDir)
                mutable.update { it.copy(documento = archivo) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val aviso = when (e) {
                    is IOException -> "No pudimos descargar el PDF. Revisa tu conexión y vuelve a intentar."
                    is IllegalStateException, is ErrorReservaciones -> e.message
                    else -> null
                }
                mutable.update { it.copy(aviso = aviso ?: "No pudimos descargar el PDF.") }
            } finally {
                mutable.update { it.copy(descargando = null) }
            }
        }
    }

    fun documentoAbierto() { mutable.update { it.copy(documento = null) } }

    fun responderCotizacion(vista: Reservacion, aceptar: Boolean) = ejecutar {
        check(vista.id == seleccion) { "Vuelve a abrir la cotización." }
        val detalle = repo.responderCotizacion(vista, aceptar)
        actualizarDetalle(detalle, if (aceptar) "Cotización aceptada. Ya puedes pagar con PayPal."
            else "Cotización rechazada. La reservación quedó cancelada.")
    }

    fun iniciar(monto: BigDecimal) = ejecutar {
        val id = checkNotNull(seleccion)
        val almacenamiento = checkNotNull(ordenes)
        check(almacenamiento.leer(id) == null) { "Ya tienes una orden para esta reservación." }
        val orden = repo.iniciar(id, monto)
        almacenamiento.guardar(id, orden)
        mutable.update { it.copy(orden = orden, aviso = "Aprueba el pago en PayPal, vuelve y confirma el cobro aquí.") }
    }

    fun descartarOrden() = ejecutar {
        val id = checkNotNull(seleccion)
        val almacenamiento = checkNotNull(ordenes)
        check(almacenamiento.leer(id)?.capturaSolicitada != true) {
            "Ya se solicitó la captura. Comprueba el resultado antes de iniciar otro pago."
        }
        almacenamiento.borrar(id)
        mutable.update { it.copy(orden = null, aviso = "Orden retirada de este dispositivo.") }
    }

    fun verificar() = ejecutar {
        val id = checkNotNull(seleccion)
        val almacenamiento = checkNotNull(ordenes)
        val orden = almacenamiento.leer(id) ?: error("No hay una orden pendiente.")
        val pago = repo.confirmar(id, orden) {
            val enProceso = orden.copy(capturaSolicitada = true)
            almacenamiento.guardar(id, enProceso)
            mutable.update { it.copy(orden = enProceso) }
        }
        val saldo = checkNotNull(pago.saldoPendiente)
        val anterior = checkNotNull(state.value.detalle)
        val detalle = anterior.copy(
            estado = checkNotNull(pago.estadoReservacion),
            saldoPendiente = saldo.toPlainString(),
            tokenQr = pago.tokenQr,
            totalPagado = pago.totalPagado?.toPlainString(),
            pagos = anterior.pagos.filterNot { it.id == pago.idPago } + MovimientoReserva(
                pago.idPago, null, null, pago.estadoPago, pago.metodoPago, pago.monto?.toPlainString()
            )
        )
        val aviso = if (saldo.signum() == 0) "Pago registrado. La reservación está liquidada."
            else "Pago registrado. Saldo pendiente: ${saldo.toPlainString()} MXN."
        actualizarDetalle(detalle, aviso)
        almacenamiento.borrar(id)
        mutable.update { it.copy(orden = null) }
    }

    private suspend fun cargarOrden(detalle: Reservacion) {
        val almacenamiento = checkNotNull(ordenes)
        val orden = almacenamiento.leer(detalle.id)
        val liquidada = detalle.estado == "CONFIRMADA" && ReglasPagoCliente.saldo(detalle)?.signum() == 0
        if (liquidada && orden != null) almacenamiento.borrar(detalle.id)
        mutable.update { it.copy(orden = orden.takeUnless { liquidada }) }
    }

    private suspend fun actualizarDetalle(detalle: Reservacion, aviso: String) =
        actualizarPases(listOf(detalle), null) { estado ->
            estado.copy(
                detalle = detalle,
                reservas = estado.reservas.map { if (it.id == detalle.id) detalle else it },
                aviso = aviso
            )
        }

    private suspend fun actualizarPases(
        reservas: List<Reservacion>,
        ids: Set<Long>?,
        mostrarResultado: (PagosClienteState) -> PagosClienteState
    ) {
        if (!sesionVigente()) return
        val cambios = reservas.associate { it.id to it.paseQr() }
        mutable.update { estado ->
            val pases = combinarPasesQr(estado.pases, cambios, ids)
            val guardados = pases.filter { it.id in estado.pasesGuardados && it in estado.pases }
            mostrarResultado(estado).copy(pases = pases, pasesGuardados = guardados.map { it.id }.toSet())
        }
        try {
            val pases = checkNotNull(cache).actualizar(cambios, ids)
            if (sesionVigente()) mutable.update { it.copy(pasesGuardados = pases.map { pase -> pase.id }.toSet()) }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            mutable.update { state ->
                state.copy(aviso = "No pudimos actualizar los pases guardados para uso offline.")
            }
        }
    }

    private fun ejecutar(tarea: suspend () -> Unit) {
        if (state.value.cargando || !sesionVigente()) return
        mutable.update { it.copy(cargando = true, aviso = null) }
        viewModelScope.launch {
            try { tarea() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val mensaje = when (e) {
                    is IOException -> "No hay conexión con D3. Puedes abrir tus pases guardados."
                    is IllegalStateException, is IllegalArgumentException, is ErrorReservaciones -> e.message
                    else -> null
                }
                mutable.update { it.copy(aviso = mensaje ?: "No se pudo consultar la reservación.") }
            } finally { mutable.update { it.copy(cargando = false) } }
        }
    }
}

