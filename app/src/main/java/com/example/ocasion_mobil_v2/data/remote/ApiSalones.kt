package com.example.ocasion_mobil_v2.data.remote

import com.example.ocasion_mobil_v2.data.remote.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiSalonesService {

    @GET("api/v1/salones/explorar")
    suspend fun explorarSalones(
        @Query("texto") texto: String? = null,
        @Query("capacidadMin") capacidadMin: Int? = null,
        @Query("capacidadMax") capacidadMax: Int? = null,
        @Query("precioHoraMax") precioHoraMax: Double? = null,
        @Query("ciudad") ciudad: String? = null,
        @Query("serviciosIds") serviciosIds: List<Int>? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30
    ): Response<SalonPageResponse>

    @GET("api/v1/servicios")
    suspend fun obtenerCatalogoServicios(): Response<List<ServicioBackend>>

    @GET("api/v1/salones/{id}/detalle")
    suspend fun obtenerDetalleCliente(
        @Path("id") idSalon: Int
    ): Response<SalonDetalleClienteDTO>

    @GET("api/v1/salones/{id}/calendario")
    suspend fun obtenerCalendarioCliente(
        @Path("id") idSalon: Int,
        @Query("mes") mes: Int,
        @Query("anio") anio: Int
    ): Response<List<DisponibilidadMesDTO>>

    // ==========================================
    // SALONES
    // ==========================================

    @GET("api/v1/salones/propietario")
    suspend fun obtenerMisSalones():
            Response<List<SalonPropietarioDTO>>

    @DELETE("api/v1/salones/{id}")
    suspend fun eliminarSalon(
        @Path("id") id: Int
    ): Response<ResponseBody>

    @POST("api/v1/salones/fase1")
    suspend fun crearSalonFase1(
        @Body request: SalonCreateFase1Request
    ): Response<SalonResponse>


    // ==========================================
    // SERVICIOS
    // ==========================================

    @POST("api/v1/salones/{id}/servicios")
    suspend fun guardarServicios(
        @Path("id") idSalon: Int,
        @Body servicios: List<ServicioAsignadoRequest>
    ): Response<ResponseBody>


    @DELETE("api/v1/salones/{id}/servicios/{idServicio}")
    suspend fun desvincularServicio(
        @Path("id") idSalon: Int,
        @Path("idServicio") idServicio: Int
    ): Response<ResponseBody>


    @POST("api/v1/servicios/personalizado")
    suspend fun crearServicioPersonalizado(
        @Query("nombre") nombre: String,
        @Query("descripcion") descripcion: String
    ): Response<ServicioBackend>


    // ==========================================
    // INVENTARIO
    // ==========================================

    @POST("api/v1/salones/{id}/inventario-lote")
    suspend fun guardarInventario(
        @Path("id") idSalon: Int,
        @Body inventario: InventarioBatchDTO
    ): Response<ResponseBody>


    @POST("api/v1/salones/{id}/inventario")
    suspend fun agregarRecurso(
        @Path("id") idSalon: Int,
        @Query("idCategoria") idCategoria: Int,
        @Body recurso: RecursoDTO
    ): Response<ResponseBody>


    @POST("api/v1/salones/{id}/categorias")
    suspend fun crearCategoria(
        @Path("id") idSalon: Int,
        @Query("nombre") nombre: String,
        @Query("descripcion") descripcion: String? = null
    ): Response<ResponseBody>


    @PUT("api/v1/inventario/{id}")
    suspend fun actualizarRecurso(
        @Path("id") idRecurso: Int,
        @Query("nombre") nombre: String,
        @Query("cantidad") cantidad: Int
    ): Response<ResponseBody>


    @DELETE("api/v1/inventario/{id}")
    suspend fun eliminarRecurso(
        @Path("id") idRecurso: Int
    ): Response<ResponseBody>


    @PUT("api/v1/categorias/{id}")
    suspend fun actualizarCategoria(
        @Path("id") idCategoria: Int,
        @Query("nombre") nombre: String,
        @Query("descripcion") descripcion: String
    ): Response<ResponseBody>


    @DELETE("api/v1/categorias/{id}")
    suspend fun eliminarCategoria(
        @Path("id") idCategoria: Int
    ): Response<ResponseBody>


    // ==========================================
    // PUBLICACIÓN
    // ==========================================

    @PATCH("api/v1/salones/{id}/publicar")
    suspend fun publicarSalon(
        @Path("id") idSalon: Int
    ): Response<ResponseBody>
}
/*package com.example.ocasion_mobil_v2.data.remote

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
}*/
