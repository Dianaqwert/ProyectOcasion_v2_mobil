package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.*

class CatalogoSalonRepository(
    context: Context
) {

    private val api =
        RetrofitClientSalones.getService(context)


    companion object {

        /*
         * El backend proporcionado no tiene GET /servicios.
         *
         * Por eso usamos los IDs que debe tener
         * el seed del backend.
         *
         * Si se que los IDs son diferentes,
         * solamente cambia estos números.
         */

        val serviciosBase =
            listOf(

                ServicioBackend(
                    1,
                    "Banquete y Catering",
                    "Alimentos y servicio de catering para eventos."
                ),

                ServicioBackend(
                    2,
                    "Sonido e Iluminación Profesional",
                    "Equipo de audio, iluminación y operación."
                ),

                ServicioBackend(
                    3,
                    "Personal de Meseros y Limpieza",
                    "Personal para atención, montaje y limpieza."
                ),

                ServicioBackend(
                    4,
                    "Seguridad y Control de Acceso",
                    "Personal para control de acceso y seguridad."
                ),

                ServicioBackend(
                    5,
                    "Decoración y Mantelería Fina",
                    "Decoración, mantelería y ambientación."
                ),

                ServicioBackend(
                    6,
                    "Planta de Emergencia / Generador Eléctrico",
                    "Respaldo eléctrico para el evento."
                )
            )
    }


    suspend fun crearServicioPersonalizado(
        nombre: String,
        descripcion: String
    ) =
        api.crearServicioPersonalizado(
            nombre,
            descripcion
        )


    suspend fun guardarServicios(
        idSalon: Int,
        servicios: List<ServicioAsignadoRequest>
    ) =
        api.guardarServicios(
            idSalon,
            servicios
        )


    suspend fun guardarInventario(
        idSalon: Int,
        lote: InventarioBatchDTO
    ) =
        api.guardarInventario(
            idSalon,
            lote
        )


    suspend fun publicarSalon(
        idSalon: Int
    ) =
        api.publicarSalon(idSalon)


    suspend fun actualizarRecurso(
        id: Int,
        nombre: String,
        cantidad: Int
    ) =
        api.actualizarRecurso(
            id,
            nombre,
            cantidad
        )


    suspend fun eliminarRecurso(
        id: Int
    ) =
        api.eliminarRecurso(id)


    suspend fun actualizarCategoria(
        id: Int,
        nombre: String,
        descripcion: String
    ) =
        api.actualizarCategoria(
            id,
            nombre,
            descripcion
        )


    suspend fun eliminarCategoria(
        id: Int
    ) =
        api.eliminarCategoria(id)
}