package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

object ReglasPropietario {

    fun disponible(total: Int, ocupado: Int): Int {
        require(total >= 0 && ocupado >= 0) {
            "El servidor devolvió existencias inválidas."
        }

        return (total - ocupado).coerceAtLeast(0)
    }

    fun cantidad(
        texto: String,
        disponible: Int
    ): Int? {
        return texto.toIntOrNull()
            ?.takeIf { it in 0..disponible }
    }

    fun dinero(texto: String): BigDecimal? {
        val normalizado = texto.trim().replace(',', '.')

        if (!Regex("[0-9]{1,9}(\\.[0-9]{1,2})?")
                .matches(normalizado)
        ) {
            return null
        }

        return normalizado.toBigDecimalOrNull()
    }

    fun fecha(texto: String?): LocalDate? =
        try {
            texto?.let { LocalDate.parse(it) }
        } catch (_: Exception) {
            null
        }

    fun delMes(
        reserva: Reservacion,
        mes: YearMonth
    ): Boolean {
        return fecha(reserva.fechaInicio)?.let {
            YearMonth.from(it) == mes
        } == true
    }

    fun aceptada(estado: String): Boolean =
        estado in setOf(
            "ACEPTADA_CLIENTE",
            "CONFIRMADA"
        )

    fun puedePdf(estado: String): Boolean =
        aceptada(estado)

    fun etiqueta(estado: String): String =
        when (estado) {

            "SOLICITUD_NUEVA",
            "COTIZACION_PENDIENTE",
            "ACEPTADA_PROPIETARIO" -> "Pendiente"

            "ESPERANDO_PAGO",
            "ACEPTADA_CLIENTE" -> "Esperando pago"

            "CONFIRMADA" -> "Confirmada"

            "RECHAZADO",
            "RECHAZADA",
            "RECHAZADA_PROPIETARIO",
            "RECHAZADA_CLIENTE" -> "Rechazada"

            else -> estado.replace('_', ' ')
        }
}