package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

data class ServicioSeleccionado(
    val id: Int,
    val nombre: String
)

data class RecursoInventario(
    val id: Int? = null,
    val nombre: String,
    val cantidadTotal: Int
)

data class CategoriaInventario(
    val id: Int? = null,
    val nombre: String,
    val recursos: List<RecursoInventario> = emptyList(),
    val expandida: Boolean = true
)

data class CatalogoSalonState(
    // Servicios
    val serviciosDisponibles: List<ServicioSeleccionado> = emptyList(),
    val serviciosSeleccionados: List<ServicioSeleccionado> = emptyList(),

    // Inventario
    val categoriasInventario: List<CategoriaInventario> = emptyList(),

    // Estado de la pantalla
    val cargando: Boolean = false,
    val mensajeError: String? = null
) {
    val puedeAgregarServicio: Boolean
        get() = serviciosSeleccionados.size < 10

    val puedeAgregarCategoria: Boolean
        get() = categoriasInventario.size < 8
}