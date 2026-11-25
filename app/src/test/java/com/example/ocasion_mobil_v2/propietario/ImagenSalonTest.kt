package com.example.ocasion_mobil_v2.propietario

import com.example.ocasion_mobil_v2.util.ImagenSalonLoader.ImagenRemota
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO
import com.example.ocasion_mobil_v2.util.ImagenSalonLoader
import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class ImagenSalonTest {
    private val base = "http://172.30.241.234:8081/"

    @Test fun respuestaDelPropietarioResuelveFotoConAutenticacion() {
        val salon = Gson().fromJson("""{"id_salon":12,"imagenes":["/api/v1/salones/12/imagenes/34"]}""", SalonPropietarioDTO::class.java)
        val imagen = ImagenSalonLoader.modelo(salon.imagenes!!.first(), base, "token-prueba") as ImagenRemota
        assertEquals(base + "api/v1/salones/12/imagenes/34", imagen.url)
        assertEquals("Bearer token-prueba", imagen.headers["Authorization"])
    }

    @Test fun noEnviaTokenAOtroServidor() {
        for (url in listOf("https://example.com/foto.jpg", "//example.com/foto.jpg", "http://172.30.241.234:8090/foto.jpg")) {
            val imagen = ImagenSalonLoader.modelo(url, base, "privado") as ImagenRemota
            assertNull(imagen.headers["Authorization"])
        }
    }

    @Test fun conservaBase64Antiguo() {
        val data = "data:image/png;base64,iVBORw0KGgo="
        assertEquals(data, ImagenSalonLoader.modelo(data, base, "token"))
    }

    @Test fun permiteFotoAusenteYSesionAusente() {
        assertNull(ImagenSalonLoader.modelo(null, base, null))
        assertNull(ImagenSalonLoader.modelo(" ", base, null))
        assertNull((ImagenSalonLoader.modelo("/api/v1/salones/1/imagenes/2", base, null) as ImagenRemota).headers["Authorization"])
    }
}
