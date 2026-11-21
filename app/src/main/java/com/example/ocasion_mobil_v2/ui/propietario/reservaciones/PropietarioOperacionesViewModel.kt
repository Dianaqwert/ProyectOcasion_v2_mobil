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
    val precios: Map<Long, String> = emptyMap(),
    val renta: String = "",
    val notas: String = "",
    val verificado: Boolean = false,
    val enviado: Boolean = false,
    val error: String? = null
) {
    val puedeEnviar: Boolean
        get() {
            val solicitud = reserva ?: return false

            return verificado &&
                    !cargando &&
                    !enviando &&
                    !enviado &&
                    solicitud.estado == "SOLICITUD_NUEVA" &&
                    ReglasPropietario.dinero(renta) != null &&

                    stock.all {
                        ReglasPropietario.cantidad(
                            cantidades[it.id] ?: "0",
                            it.disponible
                        ) != null
                    } &&

                    stock.any {
                        (cantidades[it.id]?.toIntOrNull() ?: 0) > 0
                    } &&

                    solicitud.servicios.all {
                        it.id != null &&
                                (it.cantidad ?: 0) > 0 &&
                                ReglasPropietario.dinero(
                                    precios[it.id].orEmpty()
                                ) != null
                    }
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
                    it.copy(reserva = reserva)
                }

                check(reserva.estado == "SOLICITUD_NUEVA") {
                    "Esta solicitud ya cambió de estado. " +
                            "Regresa a la bandeja."
                }

                require(
                    reserva.servicios.all {
                        it.id != null &&
                                (it.cantidad ?: 0) > 0
                    }
                ) {
                    "La solicitud contiene servicios incompletos."
                }

                require(
                    reserva.servicios.map { it.id }
                        .distinct().size ==
                            reserva.servicios.size
                ) {
                    "La solicitud contiene servicios duplicados."
                }

                val stock = repo.stock(reserva)

                validarSesion()

                _state.update { anterior ->
                    anterior.copy(
                        cargando = false,
                        stock = stock,
                        verificado = true,

                        cantidades = anterior.cantidades.filterKeys {
                                recursoId -> stock.any { it.id == recursoId }
                        },

                        precios = reserva.servicios.associate {
                            it.id!! to (
                                    anterior.precios[it.id]
                                        ?: it.precio.orEmpty()
                                    )
                        }
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

    fun precio(id: Long, texto: String) {
        if (_state.value.enviando) return

        _state.update {
            it.copy(precios = it.precios + (id to texto))
        }
    }

    fun renta(texto: String) {
        if (_state.value.enviando) return

        _state.update { it.copy(renta = texto) }
    }

    fun notas(texto: String) {
        if (_state.value.enviando) return

        _state.update { it.copy(notas = texto) }
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
                    actual.estado == "SOLICITUD_NUEVA" &&
                            actual.idSalon == original.idSalon &&
                            actual.fechaInicio == original.fechaInicio &&
                            actual.fechaFin == original.fechaFin &&
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

                    servicios = actual.servicios.map {
                        ServicioCotizadoDTO(
                            idServicio = it.id!!,
                            precioContratado = requireNotNull(
                                ReglasPropietario.dinero(
                                    capturado.precios[it.id].orEmpty()
                                )
                            ),
                            cantidad = it.cantidad!!
                        )
                    },

                    inventario = elegidos.map {
                        RecursoCotizadoDTO(
                            idRecurso = it.key,
                            cantidadAsignada = it.value.toInt()
                        )
                    },

                    observaciones = capturado.notas
                        .trim()
                        .takeIf { it.isNotEmpty() }
                )

                repo.enviar(id, dto)

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