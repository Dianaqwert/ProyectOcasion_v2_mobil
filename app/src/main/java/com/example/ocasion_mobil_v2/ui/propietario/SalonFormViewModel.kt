package com.example.ocasion_mobil_v2.ui.propietario

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SalonFormViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            SalonFormState()
        )

    val uiState: StateFlow<SalonFormState> =
        _uiState.asStateFlow()


    // NOMBRE

    fun actualizarNombre(
        nombre: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                nombreSalon = nombre,
                errorNombre = validarNombre(nombre)
            )

        actualizarValidez()
    }


    // CAPACIDAD

    fun actualizarCapacidad(
        capacidad: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                capacidadPersonas = capacidad,
                errorCapacidad =
                    validarCapacidad(capacidad)
            )

        actualizarValidez()
    }


    // PRECIO

    fun actualizarPrecio(
        precio: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                precioHora = precio,
                errorPrecio =
                    validarPrecio(precio)
            )

        actualizarValidez()
    }


    // DESCRIPCIÓN

    fun actualizarDescripcion(
        descripcion: String
    ) {

        _uiState.value =
            _uiState.value.copy(
                descripcion = descripcion,
                errorDescripcion =
                    validarDescripcion(descripcion)
            )

        actualizarValidez()
    }


    // IMÁGENES

    fun actualizarCantidadImagenes(
        cantidad: Int
    ) {

        val error =
            when {

                cantidad == 0 ->
                    "Agrega al menos una imagen del salón."

                cantidad > 5 ->
                    "Solo puedes tener máximo 5 imágenes."

                else ->
                    null
            }

        _uiState.value =
            _uiState.value.copy(
                cantidadImagenes = cantidad,
                errorImagenes = error
            )

        actualizarValidez()
    }


    // VALIDAR NOMBRE

    private fun validarNombre(
        nombre: String
    ): String? {

        return when {

            nombre.trim().isEmpty() ->
                "Ingresa el nombre del salón."

            else ->
                null
        }
    }


    // VALIDAR CAPACIDAD

    private fun validarCapacidad(
        capacidad: String
    ): String? {

        if (capacidad.trim().isEmpty()) {

            return "Ingresa la capacidad."
        }

        val numero =
            capacidad
                .trim()
                .toIntOrNull()

        return when {

            numero == null ->
                "Ingresa una capacidad válida."

            numero <= 0 ->
                "La capacidad debe ser mayor que 0."

            else ->
                null
        }
    }


    // VALIDAR PRECIO

    private fun validarPrecio(
        precio: String
    ): String? {

        if (precio.trim().isEmpty()) {

            return "Ingresa el precio por hora."
        }

        val numero =
            precio
                .trim()
                .toDoubleOrNull()

        return when {

            numero == null ->
                "Ingresa un precio válido."

            numero <= 0 ->
                "El precio debe ser mayor que 0."

            else ->
                null
        }
    }


    // VALIDAR DESCRIPCIÓN

    private fun validarDescripcion(
        descripcion: String
    ): String? {

        return when {

            descripcion.trim().isEmpty() ->
                "Ingresa una descripción."

            else ->
                null
        }
    }


    // VALIDAR TODO EL FORMULARIO

    private fun actualizarValidez() {

        val estado =
            _uiState.value

        val valido =
            estado.nombreSalon
                .trim()
                .isNotEmpty() &&

                    estado.capacidadPersonas
                        .trim()
                        .isNotEmpty() &&

                    estado.precioHora
                        .trim()
                        .isNotEmpty() &&

                    estado.descripcion
                        .trim()
                        .isNotEmpty() &&

                    estado.cantidadImagenes in 1..5 &&

                    estado.errorNombre == null &&

                    estado.errorCapacidad == null &&

                    estado.errorPrecio == null &&

                    estado.errorDescripcion == null &&

                    estado.errorImagenes == null


        _uiState.value =
            estado.copy(
                formularioValido = valido
            )
    }


    // VALIDACIÓN FINAL

    fun validarFormularioCompleto() {

        val estado =
            _uiState.value

        _uiState.value =
            estado.copy(

                errorNombre =
                    validarNombre(
                        estado.nombreSalon
                    ),

                errorCapacidad =
                    validarCapacidad(
                        estado.capacidadPersonas
                    ),

                errorPrecio =
                    validarPrecio(
                        estado.precioHora
                    ),

                errorDescripcion =
                    validarDescripcion(
                        estado.descripcion
                    ),

                errorImagenes =
                    when {

                        estado.cantidadImagenes == 0 ->
                            "Agrega al menos una imagen del salón."

                        estado.cantidadImagenes > 5 ->
                            "Solo puedes tener máximo 5 imágenes."

                        else ->
                            null
                    }
            )

        actualizarValidez()
    }
}