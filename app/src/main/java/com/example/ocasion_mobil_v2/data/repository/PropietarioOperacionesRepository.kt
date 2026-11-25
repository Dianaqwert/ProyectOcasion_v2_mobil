package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import com.example.ocasion_mobil_v2.data.remote.*
import com.example.ocasion_mobil_v2.data.remote.model.*
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReglasPropietario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.io.File
import java.math.BigDecimal

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


        cargarNombres()

        return lista.map { nombres(it) }
    }

    suspend fun detalle(id: Long): Reservacion =
        historial().firstOrNull { it.id == id }
            ?: error("La solicitud no pertenece a tu bandeja.")

    suspend fun stock(
        reserva: Reservacion
    ): List<StockCotizacion> {

        val salon = requireNotNull(reserva.idSalon) {
            "La solicitud no incluye el identificador del salón."
        }

        validarSesion()

        val respuesta = salones.obtenerMisSalones()

        validarSesion()

        val salonPropio = body(respuesta).firstOrNull { it.id_salon == salon }
            ?: error("El salón no aparece en tu catálogo.")
        val catalogo = checkNotNull(salonPropio.inventario) {
            "D2 no devolvió el inventario de este salón. Actualiza para volver a consultarlo."
        }
        if (catalogo.isEmpty()) return emptyList()
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
        dto: CotizacionPropietarioDTO,
        totalEsperado: BigDecimal
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
        check(resultado.costoTotal?.toBigDecimalOrNull()?.compareTo(totalEsperado) == 0) {
            "El servidor guardó un total distinto. Revisa el importe de la cotización en la bandeja."
        }

        return resultado
    }

    suspend fun pdf(id: Long): File {
        val actual = detalle(id)

        check(ReglasPropietario.puedePdf(actual.estado)) {
            "El PDF no está disponible en el estado actual de la reserva."
        }

        val respuesta = consultar {
            api.pdf(id)
        }

        return guardarPdfReserva(appContext.cacheDir, respuesta)
    }
}
