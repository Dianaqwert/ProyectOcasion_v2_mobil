package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import com.example.ocasion_mobil_v2.data.remote.model.ServicioBackend

data class ServicioSeleccionado(
    val id: Int,
    val nombre: String,
    val descripcion: String = "",
    val personalizado: Boolean = false
)


data class RecursoInventario(
    val localId: Long,
    val id: Int? = null,
    val nombre: String,
    val cantidadTotal: Int
)


data class CategoriaInventario(
    val localId: Long,
    val id: Int? = null,
    val nombre: String,
    val recursos: List<RecursoInventario> = emptyList(),
    val expandida: Boolean = true
)


data class CatalogoSalonState(
    val idSalon: Int? = null,
    val serviciosDisponibles:
    List<ServicioBackend> = emptyList(),
    val serviciosSeleccionados:
    List<ServicioSeleccionado> = emptyList(),
    val categoriasInventario:
    List<CategoriaInventario> = emptyList(),
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val mensajeError: String? = null,
    val mensajeExito: String? = null

) {

    val puedeAgregarServicio
        get() = serviciosSeleccionados.size < 10

    val puedeAgregarCategoria
        get() = categoriasInventario.size < 8


    val inventarioValido
        get() =
            categoriasInventario.isNotEmpty() &&
                    categoriasInventario.all {
                        it.recursos.isNotEmpty()
                    }
}