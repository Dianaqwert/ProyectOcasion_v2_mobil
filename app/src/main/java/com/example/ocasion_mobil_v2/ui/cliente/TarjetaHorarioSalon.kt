package com.example.ocasion_mobil_v2.ui.cliente

import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' uuuu", Locale.forLanguageTag("es-MX"))
private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")

internal fun View.mostrarHorarioSalon(horario: HorarioSalonDTO?) {
    isVisible = horario != null
    if (horario == null) return

    val apertura = LocalTime.parse(horario.horaInicio).format(formatoHora)
    val cierre = LocalTime.parse(horario.horaFin).format(formatoHora)
    val desde = LocalDate.parse(horario.fechaDesde).format(formatoFecha)
    findViewById<TextView>(R.id.tvHorasHabitualesSalon).text =
        context.getString(R.string.horario_horas_diarias, apertura, cierre)
    findViewById<TextView>(R.id.tvHorarioDesdeSalon).text =
        context.getString(R.string.horario_disponible_desde, desde)
}
