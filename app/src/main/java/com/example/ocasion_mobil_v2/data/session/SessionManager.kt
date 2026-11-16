package com.example.ocasion_mobil_v2.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.ocasion_mobil_v2.data.remote.model.PerfilUsuario
import com.google.gson.Gson
import com.google.gson.JsonParseException

/**
 * Guarda y recupera el token de sesión localmente en el dispositivo.
 * Úsalo después de un login exitoso, y para adjuntar el token en las
 * peticiones que requieran autenticación (Authorization: Bearer <token>).
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ocasion_session", Context.MODE_PRIVATE)
    private val perfilPrefs by lazy {
        context.getSharedPreferences("ocasion_perfil_propietario", Context.MODE_PRIVATE)
    }

    fun guardarToken(token: String) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    fun obtenerToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun guardarPerfilPropietario(perfil: PerfilUsuario) {
        perfilPrefs.edit().putString(KEY_PERFIL_PROPIETARIO, Gson().toJson(perfil)).apply()
    }

    fun obtenerPerfilPropietario(idUsuario: Int, correo: String): PerfilUsuario? {
        val json = perfilPrefs.getString(KEY_PERFIL_PROPIETARIO, null) ?: return null
        return try {
            Gson().fromJson(json, PerfilUsuario::class.java)?.takeIf {
                it.id == idUsuario && it.gmail.equals(correo, ignoreCase = true)
            }
        } catch (_: JsonParseException) {
            null
        }
    }

    fun invalidarSesion() {
        prefs.edit().clear().apply()
    }

    fun cerrarSesion() {
        invalidarSesion()
        perfilPrefs.edit().clear().apply()
    }

    fun haySesionActiva(): Boolean = obtenerToken() != null

    //------ EXTRACCION DE ROLES - D
    fun guardarRol(rol:String?){
        prefs.edit().putString(KEY_ROL,rol).apply()
    }
    //------ GUARDAR EL TOCKEN Y EL ROL
    fun guardarSesion(token: String, rol: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ROL, rol)
            .apply()
    }

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_ROL = "auth_user_role"
        private const val KEY_PERFIL_PROPIETARIO = "perfil_propietario"
    }
}
