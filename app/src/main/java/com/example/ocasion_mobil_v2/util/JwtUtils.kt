package com.example.ocasion_mobil_v2.util

import android.util.Base64
import org.json.JSONException
import org.json.JSONObject

object JwtUtils {
    data class IdentidadUsuario(val id: Int, val correo: String)

    fun obtenerIdCliente(token: String?): Int? =
        token?.takeIf { obtenerTipoUser(it) == "CLIENTE" }?.let(::obtenerIdentidadUsuario)?.id

    fun obtenerTipoUser(token: String): String? {
        val roles = obtenerPayload(token)?.optJSONArray("roles") ?: return null
        return roles.optString(0).takeIf { it.isNotEmpty() }
            ?.replace("ROLE_", "")?.uppercase()
    }

    fun obtenerIdentidadUsuario(token: String): IdentidadUsuario? {
        val payload = obtenerPayload(token) ?: return null
        val id = payload.optString("id_usuario").toIntOrNull()?.takeIf { it > 0 } ?: return null
        val correo = (payload.opt("sub") as? String)?.takeIf { it.isNotBlank() } ?: return null
        return IdentidadUsuario(id, correo)
    }

    private fun obtenerPayload(token: String): JSONObject? {
        val payload = token.split('.').getOrNull(1) ?: return null
        return try {
            val decodedBytes = Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP)
            JSONObject(String(decodedBytes, Charsets.UTF_8))
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: JSONException) {
            null
        }
    }
}
