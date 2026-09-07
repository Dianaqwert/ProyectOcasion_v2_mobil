package com.example.ocasion_mobil_v2.data.remote

// Importaciones de los modelos
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.SalonResponse
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO

import okhttp3.ResponseBody
// ESTA ES LA IMPORTACIÓN CLAVE QUE SUELE FALLAR:
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
interface ApiSalonesService {

    // Aquí iremos agregando todos los endpoints que probamos en Postman.
    // Por ahora, dejamos la estructura lista.

    // 1. Obtener salones del propietario (Endpoint de tu compañero)
    @GET("api/v1/salones/propietario")
    suspend fun obtenerMisSalones(): Response<List<SalonPropietarioDTO>>

    // 2. Eliminar salón (Endpoint de tu compañero)
    @DELETE("api/v1/salones/{id}")
    suspend fun eliminarSalon(@Path("id") id: Int): Response<ResponseBody>

    // 3. Crear Salón Fase 1 (Tu endpoint)
    @POST("api/v1/salones/fase1")
    suspend fun crearSalonFase1(@Body request: SalonCreateFase1Request): Response<SalonResponse>
}