package com.example.ocasion_mobil_v2.data.remote.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import java.lang.reflect.Type

/** Acepta el Page de Spring y también la lista vacía documentada por la API. */
class SalonPageDeserializer : JsonDeserializer<SalonPageResponse> {
    override fun deserialize(json: JsonElement, type: Type, context: JsonDeserializationContext): SalonPageResponse {
        val root = json.takeIf { it.isJsonObject }?.asJsonObject
        val content = if (json.isJsonArray) json.asJsonArray else
            root?.get("content")?.takeIf { it.isJsonArray }?.asJsonArray
                ?: throw JsonParseException("La respuesta no contiene una lista de salones")
        val cards = content.map { context.deserialize<SalonCardClienteDTO>(it, SalonCardClienteDTO::class.java) }
        val metadata = root?.get("page")?.takeIf { it.isJsonObject }?.asJsonObject ?: root
        return SalonPageResponse(
            content = cards,
            totalElements = metadata?.get("totalElements")?.asLong ?: cards.size.toLong(),
            totalPages = metadata?.get("totalPages")?.asInt ?: if (cards.isEmpty()) 0 else 1,
            number = metadata?.get("number")?.asInt ?: 0
        )
    }
}
