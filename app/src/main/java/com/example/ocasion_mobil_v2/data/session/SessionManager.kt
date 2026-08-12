package com.example.ocasion_mobil_v2.data.session

import android.content.Context
import android.content.SharedPreferences

/**
 * Guarda y recupera el token de sesión localmente en el dispositivo.
 * Úsalo después de un login exitoso, y para adjuntar el token en las
 * peticiones que requieran autenticación (Authorization: Bearer <token>).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ocasion_session", Context.MODE_PRIVATE)

    fun guardarToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun obtenerToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun cerrarSesion() {
        prefs.edit().clear().apply()
    }

    fun haySesionActiva(): Boolean = obtenerToken() != null

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }
}
