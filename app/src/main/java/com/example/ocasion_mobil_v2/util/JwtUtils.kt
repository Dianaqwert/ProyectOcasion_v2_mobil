package com.example.ocasion_mobil_v2.util

import android.util.Base64
import org.json.JSONObject
import android.util.Log // Asegúrate de importar Log
object JwtUtils {
    fun obtenerTipoUser(token: String): String? {
        return try {
            val partes = token.split(".")
            if (partes.size < 2) return null

            val payloadBase64 = partes[1]
            val decodedBytes = Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))

            // Extraemos el arreglo "roles"
            val rolesArray = json.optJSONArray("roles")
            if (rolesArray != null && rolesArray.length() > 0) {
                val rolCompleto = rolesArray.getString(0) // Obtiene "ROLE_Cliente"
                return rolCompleto.replace("ROLE_", "").uppercase() // Retorna "CLIENTE"
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}