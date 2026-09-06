package com.example.ocasion_mobil_v2.util

import android.util.Base64
import org.json.JSONObject

object JwtUtils {

    fun obtenerTipoUser(token:String): String? {
        return try{
            val partes = token.split(".")
            if (partes.size < 2) return null

            val payloadBase64 = partes[1]
            val decodedBytes = Base64.decode(payloadBase64, Base64.URL_SAFE or Base64.NO_WRAP)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))

            // Cambia "rol" o "tipo_usuario" por el nombre exacto del claim que envía tu backend
            json.optString("rol", json.optString("tipo_usuario", null))
        }catch (e:Exception){
            null
        }
    }
}