package com.example.ocasion_mobil_v2.data.remote

import android.content.Context
import com.example.ocasion_mobil_v2.data.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Este interceptor atrapa TODAS las peticiones antes de que salgan del celular.
 * Busca el token guardado y lo adjunta en los Headers.
 */
class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        // 1. Instanciamos el SessionManager para abrir la "caja fuerte"
        val sessionManager = SessionManager(context)
        val token = sessionManager.obtenerToken()

        // 2. Tomamos la petición original que el desarrollador intentó hacer
        val peticionOriginal = chain.request()
        val constructorPeticion = peticionOriginal.newBuilder()

        // 3. Si existe un token, inyectamos el Header "Authorization: Bearer <token>"
        if (!token.isNullOrEmpty()) {
            constructorPeticion.addHeader("Authorization", "Bearer $token")
        }

        // 4. Dejamos que la petición continúe su viaje hacia el backend
        return chain.proceed(constructorPeticion.build())
    }
}