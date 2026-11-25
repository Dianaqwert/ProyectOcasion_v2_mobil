package com.example.ocasion_mobil_v2.cliente

import com.example.ocasion_mobil_v2.data.remote.model.SalonPageDeserializer
import com.example.ocasion_mobil_v2.data.remote.model.SalonPageResponse
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.*
import org.junit.Test

class SalonPageContractTest {
    private val gson = GsonBuilder().registerTypeAdapter(
        SalonPageResponse::class.java, SalonPageDeserializer()
    ).create()

    @Test fun paginaSpringConservaIdsCamposYPaginacion() {
        val page = gson.fromJson("""{
            "content":[{"id_salon":42,"nombreSalon":"Jardín","capacidadPersonas":250,
            "precio_hora":2400.50,"imagen_portada":"https://example.com/1.jpg",
            "ciudad":"León","serviciosDestacados":["Banquete","Sonido"]}],
            "totalPages":3,"totalElements":41,"number":1
        }""", SalonPageResponse::class.java)
        assertEquals(42, page.content.single().idSalon)
        assertEquals(2400.50, page.content.single().precioHora!!, 0.001)
        assertEquals(listOf("Banquete", "Sonido"), page.content.single().servicios)
        assertEquals(3, page.totalPages)
        assertEquals(41L, page.totalElements)
        assertEquals(1, page.number)
        assertFalse(page.last)
    }

    @Test fun listaVaciaEsUnResultadoValidoSinMasPaginas() {
        val page = gson.fromJson("[]", SalonPageResponse::class.java)
        assertTrue(page.content.isEmpty())
        assertEquals(0, page.totalPages)
        assertEquals(0L, page.totalElements)
        assertTrue(page.last)
    }

    @Test fun aceptaMetadatosDePaginaEstablesDeSpring() {
        val page = gson.fromJson("""{"content":[],"page":{"number":2,"totalPages":3,"totalElements":40}}""",
            SalonPageResponse::class.java)
        assertEquals(3, page.totalPages)
        assertEquals(2, page.number)
        assertTrue(page.last)
    }

    @Test(expected = JsonParseException::class)
    fun respuestaInesperadaNoSeConfundeConCeroResultados() {
        gson.fromJson("""{"error":"Respuesta incompleta"}""", SalonPageResponse::class.java)
    }
}
