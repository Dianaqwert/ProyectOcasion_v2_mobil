package com.example.ocasion_mobil_v2.ui.cliente

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.SalonCardClienteDTO
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SalonFilters(
    val capacidadMin: Int = 50,
    val capacidadMax: Int = 1500,
    val precioMax: Double = 10_000.0,
    val ciudad: String = "",
    val serviciosIds: Set<Int> = emptySet()
) {
    fun isDefault(): Boolean = this == SalonFilters()
}

data class ExploreSalonesState(
    val consulta: String = "",
    val filtros: SalonFilters = SalonFilters(),
    val salones: List<SalonCardClienteDTO> = emptyList(),
    val total: Long = 0L,
    val cargando: Boolean = false,
    val hayMas: Boolean = false,
    val error: String? = null
)

class ExplorarSalonesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val api = RetrofitClientSalones.getService(application)

    private val _state = MutableStateFlow(ExploreSalonesState())
    val state = _state.asStateFlow()

    private var solicitud: Job? = null
    private var siguientePagina = 0

    init {
        iniciarBusqueda()
    }

    fun cambiarConsulta(texto: String) {
        if (texto == _state.value.consulta) return

        // Da tiempo a que el cliente termine de escribir.
        iniciarBusqueda(consulta = texto, esperaMs = 350)
    }

    fun aplicarFiltros(filtros: SalonFilters) {
        iniciarBusqueda(filtros = filtros)
    }

    fun limpiarFiltros() {
        // Conserva el texto del buscador al limpiar solo el panel.
        iniciarBusqueda(filtros = SalonFilters())
    }

    fun cargarSiguientePagina() {
        val actual = _state.value
        if (actual.cargando || !actual.hayMas) return

        cargarPagina(siguientePagina)
    }

    fun reintentar() {
        if (_state.value.cargando) return

        // Si falló la página 2, vuelve a pedir la página 2.
        cargarPagina(siguientePagina)
    }

    private fun iniciarBusqueda(
        consulta: String = _state.value.consulta,
        filtros: SalonFilters = _state.value.filtros,
        esperaMs: Long = 0
    ) {
        // Evita que una búsqueda anterior sobrescriba los resultados nuevos.
        solicitud?.cancel()
        siguientePagina = 0
        _state.value = ExploreSalonesState(
            consulta = consulta,
            filtros = filtros
        )
        cargarPagina(numeroPagina = 0, esperaMs = esperaMs)
    }

    private fun cargarPagina(numeroPagina: Int, esperaMs: Long = 0) {
        val anterior = _state.value
        val filtros = anterior.filtros
        _state.value = anterior.copy(cargando = true, error = null)

        solicitud = viewModelScope.launch {
            try {
                if (esperaMs > 0) delay(esperaMs)

                val respuesta = api.explorarSalones(
                    texto = anterior.consulta.trim().ifEmpty { null },
                    capacidadMin = filtros.capacidadMin,
                    capacidadMax = filtros.capacidadMax,
                    precioHoraMax = filtros.precioMax.takeIf { it < 10_000.0 },
                    ciudad = filtros.ciudad.trim().ifEmpty { null },
                    serviciosIds = filtros.serviciosIds.toList().ifEmpty { null },
                    page = numeroPagina,
                    size = 30
                )

                if (respuesta.code() == 400) {
                    siguientePagina = 0
                    _state.value = ExploreSalonesState(
                        consulta = anterior.consulta,
                        filtros = SalonFilters(),
                        error = "Los filtros no son válidos. Se restablecieron sus valores. Toca Reintentar."
                    )
                    return@launch
                }

                if (!respuesta.isSuccessful) {
                    val mensaje = when (respuesta.code()) {
                        401 -> "Tu sesión venció. Vuelve a iniciar sesión."
                        403 -> "Tu cuenta no tiene acceso al catálogo."
                        else -> "No se pudieron cargar los salones (${respuesta.code()})."
                    }
                    _state.value = anterior.copy(cargando = false, error = mensaje)
                    return@launch
                }

                val pagina = respuesta.body()
                if (pagina == null) {
                    _state.value = anterior.copy(
                        cargando = false,
                        error = "El servidor respondió sin los datos del catálogo."
                    )
                    return@launch
                }

                val salones = if (numeroPagina == 0) {
                    pagina.content
                } else {
                    (anterior.salones + pagina.content).distinctBy { it.idSalon }
                }

                siguientePagina = numeroPagina + 1
                _state.value = anterior.copy(
                    salones = salones,
                    total = pagina.totalElements,
                    cargando = false,
                    hayMas = !pagina.last && pagina.content.isNotEmpty(),
                    error = null
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                _state.value = anterior.copy(
                    cargando = false,
                    error = "No se pudo conectar con Salones. Revisa tu red o VPN y reintenta."
                )
            } catch (e: Exception) {
                _state.value = anterior.copy(
                    cargando = false,
                    error = "No se pudo leer la respuesta del catálogo."
                )
            }
        }
    }
}