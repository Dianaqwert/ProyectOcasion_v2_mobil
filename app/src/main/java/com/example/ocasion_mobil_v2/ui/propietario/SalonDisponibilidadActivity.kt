package com.example.ocasion_mobil_v2.ui.propietario

import android.app.TimePickerDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import com.google.android.material.button.MaterialButton
import java.util.Calendar
import java.util.Locale

class SalonDisponibilidadActivity : ComponentActivity() {

    private lateinit var calendar: CalendarView
    private lateinit var tvFecha: TextView
    private lateinit var btnHoraInicio: MaterialButton
    private lateinit var btnHoraFin: MaterialButton
    private lateinit var etObservaciones: EditText
    private lateinit var tvError: TextView
    private lateinit var containerBloqueos: LinearLayout

    private var fechaSeleccionada = ""
    private var horaInicio: String? = null
    private var horaFin: String? = null
    private var idSalon: String = ""
    private var nombreSalon: String = ""

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_salon_disponibilidad
        )

        recuperarDatosSalon()

        inicializarVistas()
        configurarCalendario()
        configurarBotones()

        BottomNav.configurar(this)
    }

    private fun inicializarVistas() {

        calendar = findViewById(R.id.calendarDisponibilidad)
        tvFecha = findViewById(R.id.tvFechaSeleccionada)
        btnHoraInicio = findViewById(R.id.btnHoraInicio)
        btnHoraFin = findViewById(R.id.btnHoraFin)
        etObservaciones = findViewById(R.id.etObservaciones)
        tvError = findViewById(R.id.tvErrorDisponibilidad)
        containerBloqueos = findViewById(R.id.containerBloqueos)
    }

    private fun configurarCalendario() {

        calendar.setOnDateChangeListener { _, year, month, dayOfMonth ->

            fechaSeleccionada =
                String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%04d",
                    dayOfMonth,
                    month + 1,
                    year
                )

            tvFecha.text =
                "Fecha: $fechaSeleccionada"
        }
    }

    private fun configurarBotones() {

        findViewById<TextView>(
            R.id.btnBackDisponibilidad
        ).setOnClickListener {
            finish()
        }

        btnHoraInicio.setOnClickListener {
            mostrarTimePicker(true)
        }

        btnHoraFin.setOnClickListener {
            mostrarTimePicker(false)
        }

        findViewById<MaterialButton>(
            R.id.btnBloquearHorario
        ).setOnClickListener {

            if (validarBloqueo()) {
                agregarBloqueo()
            }
        }
    }

    private fun mostrarTimePicker(esInicio: Boolean) {

        val ahora = Calendar.getInstance()

        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->

                val hora = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    hourOfDay,
                    minute
                )

                if (esInicio) {
                    horaInicio = hora
                    btnHoraInicio.text = "Inicio: $hora"
                } else {
                    horaFin = hora
                    btnHoraFin.text = "Final: $hora"
                }

            },
            ahora.get(Calendar.HOUR_OF_DAY),
            ahora.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun validarBloqueo(): Boolean {

        tvError.visibility = View.GONE

        if (fechaSeleccionada.isEmpty()) {
            tvError.text = "Selecciona una fecha."
            tvError.visibility = View.VISIBLE
            return false
        }

        if (horaInicio == null) {
            tvError.text = "Selecciona la hora de inicio."
            tvError.visibility = View.VISIBLE
            return false
        }

        if (horaFin == null) {
            tvError.text = "Selecciona la hora final."
            tvError.visibility = View.VISIBLE
            return false
        }

        if (horaFin!! <= horaInicio!!) {
            tvError.text =
                "La hora final debe ser mayor que la hora de inicio."
            tvError.visibility = View.VISIBLE
            return false
        }

        if (etObservaciones.text.toString().trim().isEmpty()) {
            tvError.text =
                "Escribe una observación para el bloqueo."
            tvError.visibility = View.VISIBLE
            return false
        }

        return true
    }

    private fun agregarBloqueo() {

        val tarjeta = LinearLayout(this)

        tarjeta.orientation = LinearLayout.VERTICAL
        tarjeta.setPadding(
            20,
            20,
            20,
            20
        )

        tarjeta.setBackgroundResource(
            R.drawable.bg_card
        )

        val titulo = TextView(this)

        titulo.text =
            "📅 $fechaSeleccionada   •   $horaInicio - $horaFin"

        titulo.textSize = 15f
        titulo.setTextColor(
            resources.getColor(
                R.color.rojo_error,
                theme
            )
        )

        titulo.setTypeface(
            null,
            Typeface.BOLD
        )

        val observacion = TextView(this)

        observacion.text =
            etObservaciones.text.toString().trim()

        observacion.textSize = 13f
        observacion.setTextColor(
            resources.getColor(
                R.color.texto_secundario,
                theme
            )
        )

        observacion.setPadding(
            0,
            8,
            0,
            0
        )

        tarjeta.addView(titulo)
        tarjeta.addView(observacion)

        val parametros =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        parametros.setMargins(
            0,
            0,
            0,
            12
        )

        tarjeta.layoutParams = parametros

        containerBloqueos.addView(tarjeta)

        Toast.makeText(
            this,
            "Horario bloqueado correctamente",
            Toast.LENGTH_SHORT
        ).show()

        etObservaciones.text.clear()

        horaInicio = null
        horaFin = null

        btnHoraInicio.text = "Hora inicio"
        btnHoraFin.text = "Hora final"
    }

    private fun recuperarDatosSalon() {

        idSalon =
            intent.getStringExtra("idSalon")
                ?: ""

        nombreSalon =
            intent.getStringExtra("nombreSalon")
                ?: "Salón"
    }
}