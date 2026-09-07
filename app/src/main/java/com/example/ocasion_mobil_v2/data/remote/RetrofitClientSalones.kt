package com.example.ocasion_mobil_v2.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClientSalones {

    // IP y Puerto del Dominio 2 (Microservicio de Salones)
    private const val BASE_URL_SALONES = "http://172.30.241.234:8081/"
    private var apiService: ApiSalonesService? = null

    /**
     * Obtenemos la instancia del servicio pasándole el Contexto de la pantalla
     * actual (Activity o Fragment) para poder acceder a las SharedPreferences.
     */
    fun getService(context: Context): ApiSalonesService {
        if (apiService == null) {

            // Interceptor para ver los JSONs en el Logcat (Consola)
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            // Nuestro NUEVO interceptor que pega el Token mágicamente
            val authInterceptor = AuthInterceptor(context)

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .addInterceptor(authInterceptor) // <-- Se agrega a la cadena
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL_SALONES)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            apiService = retrofit.create(ApiSalonesService::class.java)
        }
        return apiService!!
    }
}