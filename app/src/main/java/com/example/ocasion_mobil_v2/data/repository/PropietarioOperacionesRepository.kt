package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import com.example.ocasion_mobil_v2.data.remote.*
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReglasPropietario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.io.DataInputStream
import java.io.File
import java.util.UUID

data class StockCotizacion(
    val id: Long,
    val nombre: String,
    val total: Int,
    val ocupado: Int
) {
    val disponible: Int
        get() = ReglasPropietario.disponible(total, ocupado)
}

class PropietarioOperacionesRepository(context: Context) {

    private val appContext = context.applicationContext

    private val api =
        RetrofitClientReservaciones.getService(appContext)

    private val salones =
        RetrofitClientSalones.getService(appContext)

    private val session = SessionManager(appContext)
    private val tokenInicial = session.obtenerToken()

    private var idsPropios: Set<Long>? = null

    private val nombresSalones = mutableMapOf<Int, String>()
    private val nombresServicios = mutableMapOf<Long, String>()

    private fun validarSesion() {
        check(
            tokenInicial != null &&
                    session.obtenerToken() == tokenInicial
        ) {
            "La sesión cambió. Vuelve a abrir la pantalla."
        }
    }

    private fun nombres(r: Reservacion): Reservacion =
        r.copy(
            nombreSalon = r.nombreSalon
                ?: nombresSalones[r.idSalon],

            servicios = r.servicios.map {
                it.copy(
                    nombre = it.nombre
                        ?: nombresServicios[it.id]
                )
            }
        )

    private suspend fun cargarNombres() {
        try {
            withTimeoutOrNull(5_000) {
                consultar {
                    salones.obtenerMisSalones()
                }.forEach { salon ->
                    salon.nombreSalon?.let {
                        nombresSalones[salon.id_salon] = it
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Se conservan las referencias si D2 no responde.
        }

        try {
            withTimeoutOrNull(5_000) {
                consultar {
                    salones.obtenerCatalogoServicios()
                }.forEach { servicio ->
                    nombresServicios[
                        servicio.idServicio.toLong()
                    ] = servicio.nombreServicio
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            // Se conservan los IDs de servicios.
        }

        validarSesion()
    }

    private suspend fun <T> consultar(
        block: suspend () -> Response<T>
    ): T {
        validarSesion()

        val respuesta = block()

        if (session.obtenerToken() != tokenInicial) {
            respuesta.errorBody()?.close()

            (respuesta.body() as? okhttp3.ResponseBody)
                ?.close()

            error("La sesión cambió. Vuelve a abrir la pantalla.")
        }

        return body(respuesta)
    }

    private fun <T> body(respuesta: Response<T>): T {
        if (!respuesta.isSuccessful) {
            respuesta.errorBody()?.close()

            throw ErrorReservaciones(
                respuesta.code(),
                when (respuesta.code()) {
                    401 ->
                        "Tu sesión venció. Vuelve a iniciar sesión."

                    403 ->
                        "No tienes permiso para consultar esta información."

                    409, 422 ->
                        "La reservación o las existencias cambiaron. Actualiza antes de continuar."

                    else ->
                        "No pudimos completar la consulta (HTTP ${respuesta.code()})."
                }
            )
        }

        return respuesta.body()
            ?: error("El servidor devolvió una respuesta vacía.")
    }

    suspend fun historial(): List<Reservacion> {
        val lista = consultar {
            api.bandeja(null)
        }.distinctBy { it.id }

        idsPropios = lista.map { it.id }.toSet()

        cargarNombres()

        return lista.map { nombres(it) }
    }

    suspend fun detalle(id: Long): Reservacion {
        validarSesion()

        if (idsPropios == null) {
            historial()
        }

        check(id in idsPropios.orEmpty()) {
            "La solicitud no pertenece a tu bandeja."
        }

        val reserva = consultar {
            api.detalle(id)
        }

        check(reserva.id == id) {
            "El servidor devolvió una solicitud diferente."
        }

        return nombres(reserva)
    }

    suspend fun stock(
        reserva: Reservacion
    ): List<StockCotizacion> {

        val salon = requireNotNull(reserva.idSalon) {
            "La solicitud no incluye el identificador del salón."
        }

        validarSesion()

        val respuesta = salones.inventarioParaCotizar(salon)

        validarSesion()

        if (respuesta.code() == 404 || respuesta.code() == 405) {
            respuesta.errorBody()?.close()

            error(
                "El catálogo de inventario todavía no está disponible " +
                        "en el servidor. No podemos verificar el stock."
            )
        }

        val catalogo = body(respuesta)

        val ocupados = consultar {
            api.ocupados(reserva.id)
        }

        require(
            catalogo.all {
                it.idRecurso != null &&
                        it.idRecurso > 0 &&
                        !it.nombreRecurso.isNullOrBlank() &&
                        it.cantidadTotal != null &&
                        it.cantidadTotal >= 0
            }
        ) {
            "El catálogo recibido está incompleto."
        }

        require(
            catalogo.map { it.idRecurso }.distinct().size
                    == catalogo.size
        ) {
            "El catálogo tiene recursos duplicados."
        }

        require(
            ocupados.all {
                it.idRecurso != null &&
                        it.idRecurso > 0 &&
                        it.cantidadOcupada != null &&
                        it.cantidadOcupada >= 0
            }
        ) {
            "No se pudo interpretar el inventario ocupado."
        }

        require(
            ocupados.map { it.idRecurso }.distinct().size
                    == ocupados.size
        ) {
            "El inventario ocupado contiene duplicados."
        }

        require(
            ocupados.all { ocupado ->
                catalogo.any {
                    it.idRecurso == ocupado.idRecurso
                }
            }
        ) {
            "El catálogo y el inventario ocupado no coinciden."
        }

        val ocupacion = ocupados.associate {
            it.idRecurso!! to it.cantidadOcupada!!
        }

        return catalogo.map {
            StockCotizacion(
                id = it.idRecurso!!,
                nombre = it.nombreRecurso!!,
                total = it.cantidadTotal!!,
                ocupado = ocupacion[it.idRecurso] ?: 0
            )
        }
    }

    suspend fun enviar(
        id: Long,
        dto: CotizacionPropietarioDTO
    ): Reservacion {

        val resultado = consultar {
            api.cotizar(id, dto)
        }

        check(
            resultado.id == id &&
                    resultado.estado == "ESPERANDO_PAGO"
        ) {
            "El servidor no confirmó el envío. " +
                    "Actualiza la bandeja antes de reintentar."
        }

        return resultado
    }

    suspend fun pdf(id: Long): File {
        val actual = detalle(id)

        check(ReglasPropietario.puedePdf(actual.estado)) {
            "El cliente aún no ha aceptado la cotización, " +
                    "o el evento dejó de estar activo."
        }

        val respuesta = consultar {
            api.pdf(id)
        }

        return withContext(Dispatchers.IO) {

            val carpeta = File(
                appContext.cacheDir,
                "contratos"
            ).apply {
                mkdirs()
            }

            carpeta.listFiles()
                ?.filter {
                    System.currentTimeMillis() - it.lastModified() >
                            86_400_000L
                }
                ?.forEach {
                    it.delete()
                }

            val archivo = File(
                carpeta,
                "Cotizacion_${UUID.randomUUID()}.pdf"
            )

            try {
                respuesta.use { body ->
                    body.byteStream().use { input ->
                        archivo.outputStream().use { output ->

                            val buffer = ByteArray(8192)
                            var total = 0L

                            while (true) {
                                val n = input.read(buffer)

                                if (n < 0) break

                                total += n

                                check(total <= 20L * 1024 * 1024) {
                                    "El documento supera el tamaño permitido."
                                }

                                output.write(buffer, 0, n)
                            }
                        }
                    }
                }

                val firma = archivo.inputStream().use {
                    val bytes = ByteArray(5)
                    DataInputStream(it).readFully(bytes)
                    bytes.toString(Charsets.US_ASCII)
                }

                check(firma == "%PDF-") {
                    "El servidor no devolvió un PDF válido."
                }

                archivo
            } catch (e: Exception) {
                archivo.delete()
                throw e
            }
        }
    }
}