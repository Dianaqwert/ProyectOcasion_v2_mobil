package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.model.CategoriaBatchDTO
import com.example.ocasion_mobil_v2.data.remote.model.InventarioBatchDTO
import com.example.ocasion_mobil_v2.data.remote.model.RecursoDTO
import com.example.ocasion_mobil_v2.data.remote.model.ServicioAsignadoRequest
import com.example.ocasion_mobil_v2.data.remote.model.ServicioBackend
import com.example.ocasion_mobil_v2.data.repository.CatalogoSalonRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class CatalogoSalonViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        CatalogoSalonRepository(application)

    private val _state =
        MutableStateFlow(
            CatalogoSalonState()
        )

    val state: StateFlow<CatalogoSalonState> =
        _state.asStateFlow()


    private var nextCategoriaId = 1L

    private var nextRecursoId = 1L


    fun iniciar(idSalon: Int) {

        if (_state.value.idSalon == idSalon)
            return

        _state.value =
            _state.value.copy(
                idSalon = idSalon,
                serviciosDisponibles =
                    CatalogoSalonRepository.serviciosBase,
                mensajeError = null,
                mensajeExito = null
            )
    }


    // ==========================================
    // SERVICIOS
    // ==========================================

    fun toggleServicio(
        servicio: ServicioSeleccionado
    ) {

        val actual = _state.value

        val existe =
            actual.serviciosSeleccionados.any {
                it.id == servicio.id
            }


        if (existe) {

            _state.value =
                actual.copy(
                    serviciosSeleccionados =
                        actual.serviciosSeleccionados
                            .filterNot {
                                it.id == servicio.id
                            },

                    mensajeError = null
                )

            return
        }


        if (actual.serviciosSeleccionados.size >= 10) {

            error(
                "Límite excedido: un salón solo puede tener máximo 10 servicios asociados."
            )

            return
        }


        _state.value =
            actual.copy(
                serviciosSeleccionados =
                    actual.serviciosSeleccionados +
                            servicio,

                mensajeError = null
            )
    }


    fun agregarServicioPersonalizado(
        nombre: String,
        descripcion: String
    ) {

        val n = nombre.trim()
        val d = descripcion.trim()


        if (n.isBlank()) {

            error(
                "El nombre del servicio es obligatorio."
            )

            return
        }


        if (n.length > 100) {

            error(
                "El nombre no puede superar 100 caracteres."
            )

            return
        }


        if (d.length > 255) {

            error(
                "La descripción no puede superar 255 caracteres."
            )

            return
        }


        if (!_state.value.puedeAgregarServicio) {

            error(
                "No puedes agregar más de 10 servicios por salón."
            )

            return
        }


        if (
            _state.value.serviciosSeleccionados.any {
                it.nombre.equals(n, true)
            }
        ) {

            error(
                "Ya seleccionaste un servicio con ese nombre."
            )

            return
        }


        _state.value =
            _state.value.copy(
                guardando = true,
                mensajeError = null
            )


        viewModelScope.launch {

            try {

                val response =
                    repository.crearServicioPersonalizado(
                        n,
                        d
                    )


                if (
                    !response.isSuccessful ||
                    response.body() == null
                ) {

                    _state.value =
                        _state.value.copy(
                            guardando = false,
                            mensajeError =
                                errorFrom(
                                    response,
                                    "No fue posible crear el servicio personalizado."
                                )
                        )

                    return@launch
                }


                val body =
                    response.body()!!


                val servicio =
                    ServicioSeleccionado(

                        id =
                            body.idServicio,

                        nombre =
                            body.nombreServicio,

                        descripcion =
                            body.descripcion.orEmpty(),

                        personalizado = true
                    )


                _state.value =
                    _state.value.copy(

                        serviciosSeleccionados =
                            _state.value
                                .serviciosSeleccionados +
                                    servicio,

                        serviciosDisponibles =
                            _state.value
                                .serviciosDisponibles +
                                    ServicioBackend(
                                        body.idServicio,
                                        body.nombreServicio,
                                        body.descripcion
                                    ),

                        guardando = false,

                        mensajeExito =
                            "Servicio personalizado creado y seleccionado."
                    )

            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(

                        guardando = false,

                        mensajeError =
                            "Error de red: ${
                                e.message ?: "sin conexión"
                            }"
                    )
            }
        }
    }


    // ==========================================
    // CATEGORÍAS
    // ==========================================

    fun agregarCategoria(
        nombre: String
    ): Boolean {

        val n = nombre.trim()


        if (n.isBlank())
            return fail(
                "El nombre de la categoría es obligatorio."
            )


        if (n.length > 100)
            return fail(
                "El nombre de la categoría no puede superar 100 caracteres."
            )


        if (!_state.value.puedeAgregarCategoria)
            return fail(
                "No puedes agregar más de 8 categorías por salón."
            )


        if (
            _state.value.categoriasInventario.any {
                it.nombre.equals(n, true)
            }
        )
            return fail(
                "Ya existe una categoría con ese nombre."
            )


        val nueva =
            CategoriaInventario(
                localId = nextCategoriaId++,
                nombre = n
            )


        _state.value =
            _state.value.copy(
                categoriasInventario =
                    _state.value
                        .categoriasInventario +
                            nueva,

                mensajeError = null
            )


        return true
    }


    fun editarCategoriaLocal(
        localId: Long,
        nombre: String
    ): Boolean {

        val n = nombre.trim()


        if (n.isBlank())
            return fail(
                "El nombre de la categoría es obligatorio."
            )


        if (
            _state.value.categoriasInventario.any {
                it.localId != localId &&
                        it.nombre.equals(n, true)
            }
        )
            return fail(
                "Ya existe una categoría con ese nombre."
            )


        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .map {

                            if (
                                it.localId ==
                                localId
                            )
                                it.copy(
                                    nombre = n
                                )
                            else
                                it
                        }
            )


        return true
    }


    fun eliminarCategoriaLocal(
        localId: Long
    ) {

        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .filterNot {
                            it.localId ==
                                    localId
                        }
            )
    }


    fun cambiarExpansionCategoria(
        localId: Long
    ) {

        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .map {

                            if (
                                it.localId ==
                                localId
                            )
                                it.copy(
                                    expandida =
                                        !it.expandida
                                )
                            else
                                it
                        }
            )
    }


    // ==========================================
    // RECURSOS
    // ==========================================

    fun agregarRecurso(
        categoriaLocalId: Long,
        nombre: String,
        cantidad: Int
    ): Boolean {

        val n = nombre.trim()


        if (n.isBlank())
            return fail(
                "El nombre del recurso es obligatorio."
            )


        if (cantidad !in 1..10_000)
            return fail(
                "La existencia debe estar entre 1 y 10,000 unidades."
            )


        val categoria =
            _state.value
                .categoriasInventario
                .firstOrNull {
                    it.localId ==
                            categoriaLocalId
                }
                ?: return fail(
                    "No se encontró la categoría."
                )


        if (categoria.recursos.size >= 20)
            return fail(
                "No puedes agregar más de 20 recursos en esta categoría."
            )


        if (
            categoria.recursos.any {
                it.nombre.equals(n, true)
            }
        )
            return fail(
                "Ya existe un recurso con ese nombre en esta categoría."
            )


        val nuevo =
            RecursoInventario(

                localId =
                    nextRecursoId++,

                nombre = n,

                cantidadTotal =
                    cantidad
            )


        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .map {

                            if (
                                it.localId ==
                                categoriaLocalId
                            )
                                it.copy(
                                    recursos =
                                        it.recursos +
                                                nuevo
                                )
                            else
                                it
                        },

                mensajeError = null
            )


        return true
    }


    fun editarRecursoLocal(
        categoriaLocalId: Long,
        recursoLocalId: Long,
        nombre: String,
        cantidad: Int
    ): Boolean {

        val n = nombre.trim()


        if (n.isBlank())
            return fail(
                "El nombre del recurso es obligatorio."
            )


        if (cantidad !in 1..10_000)
            return fail(
                "La existencia debe estar entre 1 y 10,000 unidades."
            )


        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .map { categoria ->

                            if (
                                categoria.localId ==
                                categoriaLocalId
                            ) {

                                categoria.copy(

                                    recursos =
                                        categoria
                                            .recursos
                                            .map { recurso ->

                                                if (
                                                    recurso.localId ==
                                                    recursoLocalId
                                                )
                                                    recurso.copy(
                                                        nombre = n,
                                                        cantidadTotal =
                                                            cantidad
                                                    )
                                                else
                                                    recurso
                                            }
                                )

                            } else
                                categoria
                        }
            )


        return true
    }


    fun eliminarRecursoLocal(
        categoriaLocalId: Long,
        recursoLocalId: Long
    ) {

        _state.value =
            _state.value.copy(

                categoriasInventario =
                    _state.value
                        .categoriasInventario
                        .map { categoria ->

                            if (
                                categoria.localId ==
                                categoriaLocalId
                            )
                                categoria.copy(

                                    recursos =
                                        categoria
                                            .recursos
                                            .filterNot {
                                                it.localId ==
                                                        recursoLocalId
                                            }
                                )
                            else
                                categoria
                        }
            )
    }


    // ==========================================
    // GUARDAR SERVICIOS + INVENTARIO
    // ==========================================

    fun guardarCatalogo(
        onSuccess: () -> Unit
    ) {

        val stateActual =
            _state.value


        val idSalon =
            stateActual.idSalon
                ?: return error(
                    "No se encontró el id del salón."
                )


        if (
            stateActual
                .serviciosSeleccionados
                .isEmpty()
        )
            return error(
                "Selecciona al menos un servicio."
            )


        if (
            stateActual
                .serviciosSeleccionados
                .size > 10
        )
            return error(
                "Máximo 10 servicios por salón."
            )


        if (
            stateActual
                .categoriasInventario
                .isEmpty()
        )
            return error(
                "Crea al menos una categoría de inventario."
            )


        if (
            stateActual
                .categoriasInventario
                .any {
                    it.recursos.isEmpty()
                }
        )
            return error(
                "Cada categoría debe tener al menos un insumo."
            )


        if (
            stateActual
                .categoriasInventario
                .size > 8
        )
            return error(
                "Máximo 8 categorías por salón."
            )


        if (
            stateActual
                .categoriasInventario
                .any {
                    it.recursos.size > 20
                }
        )
            return error(
                "Una categoría supera el máximo de 20 recursos."
            )


        _state.value =
            stateActual.copy(
                guardando = true,
                mensajeError = null
            )


        viewModelScope.launch {

            try {

                // ==============================
                // SERVICIOS
                // ==============================

                val servicios =
                    stateActual
                        .serviciosSeleccionados
                        .map {

                            ServicioAsignadoRequest(
                                idServicio = it.id,
                                numeroServiciosSolicitados = 1
                            )
                        }


                val respuestaServicios =
                    repository.guardarServicios(
                        idSalon,
                        servicios
                    )


                if (
                    !respuestaServicios
                        .isSuccessful
                ) {

                    failHttp(
                        respuestaServicios,
                        "No fue posible guardar los servicios."
                    )

                    return@launch
                }


                // ==============================
                // INVENTARIO
                // ==============================

                val lote =
                    InventarioBatchDTO(

                        stateActual
                            .categoriasInventario
                            .map { categoria ->

                                CategoriaBatchDTO(

                                    nombreCategoria =
                                        categoria.nombre,

                                    recursos =
                                        categoria.recursos
                                            .map {

                                                RecursoDTO(

                                                    nombreRecurso =
                                                        it.nombre,

                                                    cantidadTotal =
                                                        it.cantidadTotal
                                                )
                                            }
                                )
                            }
                    )


                val respuestaInventario =
                    repository.guardarInventario(
                        idSalon,
                        lote
                    )


                if (
                    !respuestaInventario
                        .isSuccessful
                ) {

                    failHttp(
                        respuestaInventario,
                        "No fue posible guardar el inventario."
                    )

                    return@launch
                }


                _state.value =
                    _state.value.copy(

                        guardando = false,

                        mensajeExito =
                            "Servicios e inventario guardados correctamente."
                    )


                onSuccess()


            } catch (e: Exception) {

                _state.value =
                    _state.value.copy(

                        guardando = false,

                        mensajeError =
                            "Error de red: ${
                                e.message ?: "sin conexión"
                            }"
                    )
            }
        }
    }


    fun limpiarMensajes() {

        _state.value =
            _state.value.copy(

                mensajeError = null,

                mensajeExito = null
            )
    }


    private fun fail(
        message: String
    ): Boolean {

        error(message)

        return false
    }


    private fun error(
        message: String
    ) {

        _state.value =
            _state.value.copy(

                mensajeError = message,

                mensajeExito = null
            )
    }


    private fun failHttp(
        response: Response<*>,
        fallback: String
    ) {

        _state.value =
            _state.value.copy(

                guardando = false,

                mensajeError =
                    "$fallback (${response.code()}): ${response.message()}"
            )
    }


    private fun errorFrom(
        response: Response<*>,
        fallback: String
    ): String {

        return "$fallback (${response.code()})"
    }
}