package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CatalogoSalonViewModel : ViewModel() {

    private val _state = MutableStateFlow(CatalogoSalonState())
    val state: StateFlow<CatalogoSalonState> = _state.asStateFlow()

    // SERVICIOS

    fun cargarServiciosDisponibles(servicios: List<ServicioSeleccionado>) {
        _state.value = _state.value.copy(
            serviciosDisponibles = servicios
        )
    }

    fun agregarServicio(servicio: ServicioSeleccionado): Boolean {

        val estadoActual = _state.value

        // Evitar servicios repetidos
        if (estadoActual.serviciosSeleccionados.any { it.id == servicio.id }) {
            return false
        }

        // Máximo 10 servicios
        if (estadoActual.serviciosSeleccionados.size >= 10) {
            _state.value = estadoActual.copy(
                mensajeError = "No puedes agregar más de 10 servicios por salón."
            )
            return false
        }

        _state.value = estadoActual.copy(
            serviciosSeleccionados =
                estadoActual.serviciosSeleccionados + servicio,
            mensajeError = null
        )

        return true
    }

    fun eliminarServicio(servicioId: Int) {

        val serviciosActualizados =
            _state.value.serviciosSeleccionados
                .filter { it.id != servicioId }

        _state.value = _state.value.copy(
            serviciosSeleccionados = serviciosActualizados,
            mensajeError = null
        )
    }

    // CATEGORÍAS DE INVENTARIO

    fun agregarCategoria(nombre: String): Boolean {

        val nombreLimpio = nombre.trim()

        if (nombreLimpio.isEmpty()) {
            _state.value = _state.value.copy(
                mensajeError = "El nombre de la categoría es obligatorio."
            )
            return false
        }

        // Máximo 8 categorías
        if (_state.value.categoriasInventario.size >= 8) {
            _state.value = _state.value.copy(
                mensajeError = "No puedes agregar más de 8 categorías."
            )
            return false
        }

        // Evitar categorías repetidas
        if (_state.value.categoriasInventario.any {
                it.nombre.equals(nombreLimpio, ignoreCase = true)
            }) {

            _state.value = _state.value.copy(
                mensajeError = "Ya existe una categoría con ese nombre."
            )
            return false
        }

        val nuevaCategoria = CategoriaInventario(
            nombre = nombreLimpio
        )

        _state.value = _state.value.copy(
            categoriasInventario =
                _state.value.categoriasInventario + nuevaCategoria,
            mensajeError = null
        )

        return true
    }

    fun eliminarCategoria(categoriaId: Int) {

        val categoriasActualizadas =
            _state.value.categoriasInventario
                .filter { it.id != categoriaId }

        _state.value = _state.value.copy(
            categoriasInventario = categoriasActualizadas,
            mensajeError = null
        )
    }

    fun cambiarExpansionCategoria(categoriaId: Int) {

        val categoriasActualizadas =
            _state.value.categoriasInventario.map { categoria ->

                if (categoria.id == categoriaId) {
                    categoria.copy(
                        expandida = !categoria.expandida
                    )
                } else {
                    categoria
                }
            }

        _state.value = _state.value.copy(
            categoriasInventario = categoriasActualizadas
        )
    }


    // RECURSOS

    fun agregarRecurso(
        categoriaId: Int,
        nombre: String,
        cantidadTotal: Int
    ): Boolean {

        val nombreLimpio = nombre.trim()

        if (nombreLimpio.isEmpty()) {
            _state.value = _state.value.copy(
                mensajeError = "El nombre del recurso es obligatorio."
            )
            return false
        }

        // Stock permitido: 1 - 10,000
        if (cantidadTotal !in 1..10_000) {
            _state.value = _state.value.copy(
                mensajeError = "La existencia debe estar entre 1 y 10,000."
            )
            return false
        }

        val categoriasActualizadas =
            _state.value.categoriasInventario.map { categoria ->

                if (categoria.id == categoriaId) {

                    // Máximo 20 recursos por categoría
                    if (categoria.recursos.size >= 20) {

                        _state.value = _state.value.copy(
                            mensajeError =
                                "No puedes agregar más de 20 recursos en esta categoría."
                        )

                        return false
                    }

                    // Evitar recursos repetidos
                    if (categoria.recursos.any {
                            it.nombre.equals(
                                nombreLimpio,
                                ignoreCase = true
                            )
                        }) {

                        _state.value = _state.value.copy(
                            mensajeError =
                                "Ya existe un recurso con ese nombre en esta categoría."
                        )

                        return false
                    }

                    val nuevoRecurso = RecursoInventario(
                        nombre = nombreLimpio,
                        cantidadTotal = cantidadTotal
                    )

                    categoria.copy(
                        recursos = categoria.recursos + nuevoRecurso
                    )

                } else {
                    categoria
                }
            }

        _state.value = _state.value.copy(
            categoriasInventario = categoriasActualizadas,
            mensajeError = null
        )

        return true
    }


    fun eliminarRecurso(
        categoriaId: Int,
        recursoId: Int?
    ) {

        val categoriasActualizadas =
            _state.value.categoriasInventario.map { categoria ->

                if (categoria.id == categoriaId) {

                    categoria.copy(
                        recursos = categoria.recursos.filter {
                            it.id != recursoId
                        }
                    )

                } else {
                    categoria
                }
            }

        _state.value = _state.value.copy(
            categoriasInventario = categoriasActualizadas,
            mensajeError = null
        )
    }


    fun editarStock(
        categoriaId: Int,
        recursoId: Int?,
        nuevaCantidad: Int
    ): Boolean {

        // Validación 1 - 10,000
        if (nuevaCantidad !in 1..10_000) {
            _state.value = _state.value.copy(
                mensajeError = "La existencia debe estar entre 1 y 10,000."
            )
            return false
        }

        val categoriasActualizadas =
            _state.value.categoriasInventario.map { categoria ->

                if (categoria.id == categoriaId) {

                    categoria.copy(
                        recursos = categoria.recursos.map { recurso ->

                            if (recurso.id == recursoId) {
                                recurso.copy(
                                    cantidadTotal = nuevaCantidad
                                )
                            } else {
                                recurso
                            }
                        }
                    )

                } else {
                    categoria
                }
            }

        _state.value = _state.value.copy(
            categoriasInventario = categoriasActualizadas,
            mensajeError = null
        )

        return true
    }


    // MENSAJES

    fun limpiarMensaje() {
        _state.value = _state.value.copy(
            mensajeError = null
        )
    }
}