package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.example.ocasion_mobil_v2.data.repository.*
import com.example.ocasion_mobil_v2.data.session.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.time.YearMonth

data class PanelPropietarioState(
    val cargando: Boolean = false,
    val reservas: List<Reservacion> = emptyList(),
    val mes: YearMonth = YearMonth.now(),
    val error: String? = null,
    val descargando: Long? = null,
    val documento: File? = null
)

class PanelPropietarioViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repo = PropietarioOperacionesRepository(app)
    private val sesion = SessionManager(app)
    private val token = sesion.obtenerToken()

    private val _state =
        MutableStateFlow(PanelPropietarioState())

    val state = _state.asStateFlow()

    fun mes(mes: YearMonth) {
        _state.update { it.copy(mes = mes) }
    }

    fun cargar() {
        if (_state.value.cargando) return

        _state.update {
            it.copy(cargando = true, error = null)
        }

        viewModelScope.launch {
            try {
                check(
                    token != null &&
                            sesion.obtenerToken() == token
                ) {
                    "La sesión cambió. Vuelve a abrir el panel."
                }

                val reservas = repo.historial()

                check(sesion.obtenerToken() == token) {
                    "La sesión cambió. Vuelve a abrir el panel."
                }

                _state.update {
                    it.copy(
                        cargando = false,
                        reservas = reservas,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        cargando = false,
                        reservas = emptyList(),
                        error = mensaje(e)
                    )
                }
            }
        }
    }

    fun descargar(id: Long) {
        if (
            _state.value.descargando != null ||
            _state.value.cargando
        ) return

        _state.update {
            it.copy(descargando = id, error = null)
        }

        viewModelScope.launch {
            try {
                check(
                    token != null &&
                            sesion.obtenerToken() == token
                ) {
                    "La sesión cambió."
                }

                val archivo = repo.pdf(id)

                if (sesion.obtenerToken() != token) {
                    archivo.delete()
                    error("La sesión cambió.")
                }

                _state.update {
                    it.copy(
                        descargando = null,
                        documento = archivo
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        descargando = null,
                        error = mensaje(e)
                    )
                }
            }
        }
    }

    fun documentoAbierto() {
        _state.update { it.copy(documento = null) }
    }
}

data class CotizarState(
    val cargando: Boolean = false,
    val enviando: Boolean = false,
    val reserva: Reservacion? = null,
    val stock: List<StockCotizacion> = emptyList(),
    val cantidades: Map<Long, String> = emptyMap(),
    val preciosServicios: Map<Long, String> = emptyMap(),
    val renta: String = "",
    val verificado: Boolean = false,
    val enviado: Boolean = false,
    val error: String? = null
) {
    val servicios: List<ServicioReservado> get() = reserva?.servicios.orEmpty()

    fun precio(servicio: ServicioReservado): String =
        servicio.id?.let(preciosServicios::get) ?:
            servicios.firstOrNull { servicio.id != null && it.id == servicio.id }?.precioTexto() ?:
            servicio.precioTexto()

    val subtotalServicios: BigDecimal?
        get() {
            var subtotal = BigDecimal.ZERO
            for (servicio in servicios) {
                val unitario = ReglasPropietario.dinero(precio(servicio)) ?: return null
                subtotal += unitario.multiply(BigDecimal.valueOf((servicio.cantidad ?: 1).toLong()))
            }
            return subtotal
        }

    val totalCotizado: BigDecimal?
        get() {
            val base = ReglasPropietario.dinero(renta)?.takeIf { it.signum() > 0 } ?: return null
            return subtotalServicios?.let(base::add)
        }

    private fun ServicioReservado.precioTexto(): String =
        precio?.toBigDecimalOrNull()?.stripTrailingZeros()?.toPlainString() ?: "0"

    fun serviciosCotizados(): List<ServicioCotizadoDTO> =
        servicios.distinctBy { it.id }.mapNotNull { servicio ->
            val idServicio = servicio.id?.takeIf { it > 0 } ?: return@mapNotNull null
            ServicioCotizadoDTO(idServicio, requireNotNull(ReglasPropietario.dinero(precio(servicio))))
        }

    val puedeEnviar: Boolean
        get() {
            val solicitud = reserva ?: return false

            return verificado && !cargando && !enviando && !enviado &&
                ReglasPropietario.puedeCotizar(solicitud) &&
                totalCotizado != null &&
                stock.all { ReglasPropietario.cantidad(cantidades[it.id] ?: "0", it.disponible) != null }
        }
}

class CotizarPropietarioViewModel(
    app: Application
) : AndroidViewModel(app) {

    private val repo = PropietarioOperacionesRepository(app)
    private val sesion = SessionManager(app)
    private val token = sesion.obtenerToken()

    private val _state = MutableStateFlow(CotizarState())
    val state = _state.asStateFlow()

    private var id: Long = 0

    fun iniciar(id: Long) {
        if (this.id == id) return

        this.id = id
        cargar()
    }

    private fun validarSesion() {
        check(
            token != null &&
                    sesion.obtenerToken() == token
        ) {
            "La sesión cambió. Vuelve a abrir la solicitud."
        }
    }

    fun cargar() {
        if (
            _state.value.enviado ||
            _state.value.enviando ||
            _state.value.cargando ||
            id <= 0
        ) return

        _state.update {
            it.copy(
                cargando = true,
                error = null,
                verificado = false
            )
        }

        viewModelScope.launch {
            try {
                validarSesion()

                val reserva = repo.detalle(id)

                validarSesion()

                _state.update {
                    val precios = it.preciosServicios.filterKeys { clave -> reserva.servicios.any { s -> s.id == clave } }
                    val actualizado = it.copy(
                        reserva = reserva,
                        preciosServicios = precios
                    )
                    val anterior = it.reserva
                    if (anterior == null || anterior.costoTotal != reserva.costoTotal ||
                        anterior.servicios != reserva.servicios || anterior.inventario != reserva.inventario ||
                        anterior.estado != reserva.estado) {
                        val guardado = actualizado.copy(preciosServicios = emptyMap())
                        val asignados = reserva.inventario.associate { recurso ->
                            requireNotNull(recurso.id) to requireNotNull(recurso.cantidad).toString()
                        }
                        val base = reserva.costoTotal?.toBigDecimalOrNull()
                            ?.subtract(guardado.subtotalServicios ?: BigDecimal.ZERO)
                            ?.takeIf { monto -> monto.signum() > 0 }
                        guardado.copy(renta = base?.toPlainString().orEmpty(), cantidades = asignados)
                    } else actualizado
                }

                check(ReglasPropietario.puedeCotizar(reserva)) {
                    "Esta solicitud ya cambió de estado. " +
                            "Regresa a la bandeja."
                }

                val stock = repo.stock(reserva)
                check(reserva.inventario.all { retenido -> stock.any { it.id == retenido.id } }) {
                    "Un recurso asignado ya no aparece en el catálogo del salón."
                }

                validarSesion()

                _state.update { anterior ->
                    anterior.copy(
                        cargando = false,
                        stock = stock,
                        verificado = true,

                        cantidades = anterior.cantidades.filterKeys {
                                recursoId -> stock.any { it.id == recursoId }
                        },

                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        cargando = false,
                        verificado = false,
                        error = mensaje(e)
                    )
                }
            }
        }
    }

    fun cantidad(id: Long, texto: String) {
        if (_state.value.enviando) return

        _state.update {
            it.copy(
                cantidades = it.cantidades + (id to texto)
            )
        }
    }

    fun renta(texto: String) {
        if (_state.value.enviando) return

        _state.update { it.copy(renta = texto) }
    }

    fun precioServicio(id: Long, texto: String) {
        if (_state.value.enviando) return
        _state.update { it.copy(preciosServicios = it.preciosServicios + (id to texto)) }
    }

    fun enviar() {
        val capturado = _state.value

        if (!capturado.puedeEnviar) return

        _state.update {
            it.copy(enviando = true, error = null)
        }

        viewModelScope.launch {
            try {
                validarSesion()

                val original =
                    requireNotNull(capturado.reserva)

                val actual = repo.detalle(id)

                check(
                    ReglasPropietario.puedeCotizar(actual) && actual.estado == original.estado &&
                            actual.inventario == original.inventario &&
                            actual.idSalon == original.idSalon &&
                            actual.fechaInicio == original.fechaInicio &&
                            actual.fechaFin == original.fechaFin &&
                            actual.horaInicio == original.horaInicio &&
                            actual.horaFin == original.horaFin &&
                            actual.costoTotal == original.costoTotal &&
                            actual.numeroInvitados ==
                            original.numeroInvitados &&
                            actual.servicios == original.servicios
                ) {
                    "La solicitud cambió. " +
                            "Actualiza los datos antes de enviar."
                }

                val stockActual = repo.stock(actual)

                validarSesion()

                _state.update {
                    it.copy(stock = stockActual)
                }

                val elegidos = capturado.cantidades
                    .filterValues {
                        (it.toIntOrNull() ?: 0) > 0
                    }

                check(
                    elegidos.all { (recursoId, texto) ->
                        stockActual.any {
                            it.id == recursoId &&
                                    ReglasPropietario.cantidad(
                                        texto,
                                        it.disponible
                                    ) != null
                        }
                    }
                ) {
                    "Stock insuficiente para esta fecha. " +
                            "Revisa las cantidades disponibles."
                }

                val dto = CotizacionPropietarioDTO(
                    precioRentaSalon = requireNotNull(
                        ReglasPropietario.dinero(
                            capturado.renta
                        )
                    ),

                    inventario = elegidos.map {
                        RecursoCotizadoDTO(
                            idRecurso = it.key,
                            cantidadAsignada = it.value.toInt()
                        )
                    },
                    servicios = capturado.serviciosCotizados()

                )

                repo.enviar(id, dto, requireNotNull(capturado.totalCotizado))

                validarSesion()

                _state.update {
                    it.copy(
                        enviando = false,
                        enviado = true,
                        verificado = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        enviando = false,
                        verificado = false,
                        error = mensaje(e) +
                                " Actualiza la solicitud antes de reintentar."
                    )
                }
            }
        }
    }
}

private fun mensaje(e: Exception): String =
    when (e) {
        is java.io.IOException ->
            "No pudimos conectar con el servidor. Revisa tu conexión."

        else ->
            e.message ?: "No pudimos cargar la información."
    }
