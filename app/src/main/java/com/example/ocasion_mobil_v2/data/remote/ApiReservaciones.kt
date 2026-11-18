package com.example.ocasion_mobil_v2.data.remote

import android.content.Context
import com.example.ocasion_mobil_v2.BuildConfig
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

interface ApiReservaciones {
    @GET("api/v1/reservaciones/propietario/bandeja")
    suspend fun bandeja(@Query("estado") estado: String = EstadosReservacion.PENDIENTE): Response<List<Reservacion>>

    @PATCH("api/v1/reservaciones/{id}/estado")
    suspend fun cambiarEstado(@Path("id") id: Long, @Body cambio: CambioEstadoReservacion): Response<Reservacion>
}

object RetrofitClientReservaciones {
    @Volatile private var api: ApiReservaciones? = null
    fun getService(context: Context): ApiReservaciones = api ?: synchronized(this) {
        api ?: Retrofit.Builder()
            .baseUrl(BuildConfig.RESERVACIONES_BASE_URL)
            .client(OkHttpClient.Builder().addInterceptor(AuthInterceptor(context.applicationContext))
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(45, TimeUnit.SECONDS).build())
            .addConverterFactory(GsonConverterFactory.create(GsonBuilder()
                .registerTypeAdapter(Reservacion::class.java, ReservacionDeserializer()).create()))
            .build().create(ApiReservaciones::class.java).also { api = it }
    }
}
