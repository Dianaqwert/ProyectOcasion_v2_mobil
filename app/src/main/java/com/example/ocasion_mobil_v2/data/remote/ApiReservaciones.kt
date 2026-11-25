package com.example.ocasion_mobil_v2.data.remote

import android.content.Context
import com.example.ocasion_mobil_v2.BuildConfig
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiReservaciones {
    @POST("api/v1/reservaciones/solicitar")
    suspend fun solicitar(@Body solicitud: SolicitarReserva): Response<SolicitudEnviada>

    @GET("api/v1/reservaciones/mis-reservaciones")
    suspend fun misReservaciones(): Response<List<Reservacion>>

    @POST("api/v1/reservaciones/{id}/paypal/crear-orden")
    suspend fun iniciarPaypal(@Path("id") id: Long, @Body pago: CrearOrdenPaypal): Response<OrdenPaypal>

    @POST("api/v1/reservaciones/{id}/paypal/capturar-orden")
    suspend fun capturarPaypal(@Path("id") id: Long, @Body orden: CapturarOrdenPaypal): Response<PagoRegistrado>

    @GET("api/v1/reservaciones/salon/{id}/calendario")
    suspend fun obtenerCalendario(
        @Path("id") id: Int
    ): CalendarioReservaDTO

    @GET("api/v1/reservaciones/propietario/bandeja")
    suspend fun bandeja(
        @Query("estado")
        estado: String? = EstadosReservacion.PENDIENTE
    ): Response<List<Reservacion>>

    @POST("api/v1/reservaciones/check-in")
    suspend fun registrarCheckIn(@Query("tokenQr") token: String): Response<CheckInRegistrado>

    @GET("api/v1/reservaciones/{id}/inventario-disponible")
    suspend fun ocupados(
        @Path("id") id: Long
    ): Response<List<RecursoOcupadoDTO>>

    @PUT("api/v1/reservaciones/{id}/cotizar-propietario")
    suspend fun cotizar(
        @Path("id") id: Long,
        @Body body: CotizacionPropietarioDTO
    ): Response<Reservacion>

    @PATCH("api/v1/reservaciones/{id}/estado")
    suspend fun cambiarEstado(
        @Path("id") id: Long,
        @Body cambio: CambioEstadoReservacion
    ): Response<Reservacion>

    @Streaming
    @GET("api/v1/reservaciones/{id}/pdf")
    suspend fun pdf(
        @Path("id") id: Long
    ): Response<ResponseBody>
}

object RetrofitClientReservaciones {

    @Volatile
    private var api: ApiReservaciones? = null

    fun getService(context: Context): ApiReservaciones =
        api ?: synchronized(this) {

            api ?: Retrofit.Builder()
                .baseUrl(BuildConfig.RESERVACIONES_BASE_URL)

                .client(
                    OkHttpClient.Builder()
                        .followSslRedirects(false)
                        .addInterceptor(
                            AuthInterceptor(context.applicationContext)
                        )
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .callTimeout(45, TimeUnit.SECONDS)
                        .build()
                )

                .addConverterFactory(
                    GsonConverterFactory.create(
                        GsonBuilder()
                            .registerTypeAdapter(
                                Reservacion::class.java,
                                ReservacionDeserializer()
                            )
                            .create()
                    )
                )

                .build()
                .create(ApiReservaciones::class.java)
                .also { api = it }
        }
}
