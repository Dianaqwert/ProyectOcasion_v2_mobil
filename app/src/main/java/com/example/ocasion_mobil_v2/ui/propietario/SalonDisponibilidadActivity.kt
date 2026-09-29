package com.example.ocasion_mobil_v2.ui.propietario

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CalendarView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.DisponibilidadInicialRequest
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionRequest
import com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon.CatalogoSalonActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class SalonDisponibilidadActivity :
    ComponentActivity() {

    private lateinit var calendar:
            CalendarView

    private lateinit var tvFecha:
            TextView

    private lateinit var btnHoraInicio:
            MaterialButton

    private lateinit var btnHoraFin:
            MaterialButton

    private lateinit var etObservaciones:
            EditText

    private lateinit var tvError:
            TextView

    private lateinit var containerBloqueos:
            LinearLayout

    private lateinit var btnContinuar:
            MaterialButton


    private var fechaSeleccionada = ""

    private var horaInicio:
            String? = null

    private var horaFin:
            String? = null


    private var nombreSalon = ""

    private var capacidadPersonas = ""

    private var precioHora = ""

    private var descripcion = ""

    private var ciudad = ""

    private var codigoPostal = ""

    private var calle = ""

    private var numero = ""

    private var fraccionamiento = ""

    private var latitud = ""

    private var longitud = ""

    private var imagenes =
        arrayListOf<String>()


    private var guardando = false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_salon_disponibilidad
        )


        recuperarDatos()

        inicializarVistas()

        configurarCalendario()

        configurarBotones()
    }


    private fun recuperarDatos() {

        nombreSalon =
            intent.getStringExtra(
                "nombreSalon"
            ).orEmpty()

        capacidadPersonas =
            intent.getStringExtra(
                "capacidadPersonas"
            ).orEmpty()

        precioHora =
            intent.getStringExtra(
                "precioHora"
            ).orEmpty()

        descripcion =
            intent.getStringExtra(
                "descripcion"
            ).orEmpty()

        ciudad =
            intent.getStringExtra(
                "ciudad"
            ).orEmpty()

        codigoPostal =
            intent.getStringExtra(
                "codigoPostal"
            ).orEmpty()

        calle =
            intent.getStringExtra(
                "calle"
            ).orEmpty()

        numero =
            intent.getStringExtra(
                "numero"
            ).orEmpty()

        fraccionamiento =
            intent.getStringExtra(
                "fraccionamiento"
            ).orEmpty()

        latitud =
            intent.getStringExtra(
                "latitud"
            ).orEmpty()

        longitud =
            intent.getStringExtra(
                "longitud"
            ).orEmpty()

        imagenes =
            intent.getStringArrayListExtra(
                "imagenes"
            ) ?: arrayListOf()
    }


    private fun inicializarVistas() {

        calendar =
            findViewById(
                R.id.calendarDisponibilidad
            )

        tvFecha =
            findViewById(
                R.id.tvFechaSeleccionada
            )

        btnHoraInicio =
            findViewById(
                R.id.btnHoraInicio
            )

        btnHoraFin =
            findViewById(
                R.id.btnHoraFin
            )

        etObservaciones =
            findViewById(
                R.id.etObservaciones
            )

        tvError =
            findViewById(
                R.id.tvErrorDisponibilidad
            )

        containerBloqueos =
            findViewById(
                R.id.containerBloqueos
            )

        btnContinuar =
            findViewById(
                R.id.btnContinuarCatalogo
            )


        findViewById<TextView>(
            R.id.btnBackDisponibilidad
        ).setOnClickListener {

            finish()
        }
    }


    private fun configurarCalendario() {

        val hoy =
            Calendar.getInstance()


        fechaSeleccionada =
            String.format(
                Locale.getDefault(),

                "%04d-%02d-%02d",

                hoy.get(
                    Calendar.YEAR
                ),

                hoy.get(
                    Calendar.MONTH
                ) + 1,

                hoy.get(
                    Calendar.DAY_OF_MONTH
                )
            )


        tvFecha.text =
            "Fecha: ${
                formatearFechaVisible(
                    fechaSeleccionada
                )
            }"


        calendar.setOnDateChangeListener {

                _,
                year,
                month,
                day ->

            fechaSeleccionada =
                String.format(
                    Locale.getDefault(),

                    "%04d-%02d-%02d",

                    year,

                    month + 1,

                    day
                )


            tvFecha.text =
                "Fecha: ${
                    formatearFechaVisible(
                        fechaSeleccionada
                    )
                }"
        }
    }


    private fun configurarBotones() {

        btnHoraInicio.setOnClickListener {

            mostrarTimePicker(
                true
            )
        }


        btnHoraFin.setOnClickListener {

            mostrarTimePicker(
                false
            )
        }


        findViewById<MaterialButton>(
            R.id.btnBloquearHorario
        ).setOnClickListener {

            if (
                validarBloqueo()
            ) {

                agregarBloqueo()
            }
        }


        btnContinuar.setOnClickListener {

            guardarFase1YContinuar()
        }
    }


    private fun mostrarTimePicker(
        esInicio: Boolean
    ) {

        val ahora =
            Calendar.getInstance()


        TimePickerDialog(

            this,

            { _, hour, minute ->

                val hora =
                    String.format(
                        Locale.getDefault(),
                        "%02d:%02d",
                        hour,
                        minute
                    )


                if (esInicio) {

                    horaInicio =
                        hora

                    btnHoraInicio.text =
                        "Inicio: $hora"

                } else {

                    horaFin =
                        hora

                    btnHoraFin.text =
                        "Final: $hora"
                }
            },

            ahora.get(
                Calendar.HOUR_OF_DAY
            ),

            ahora.get(
                Calendar.MINUTE
            ),

            true

        ).show()
    }


    private fun validarBloqueo():
            Boolean {

        tvError.visibility =
            View.GONE


        if (
            fechaSeleccionada.isBlank()
        )
            return mostrarError(
                "Selecciona una fecha."
            )


        if (
            horaInicio == null
        )
            return mostrarError(
                "Selecciona la hora de inicio."
            )


        if (
            horaFin == null
        )
            return mostrarError(
                "Selecciona la hora final."
            )


        if (
            horaFin!! <=
            horaInicio!!
        )
            return mostrarError(
                "La hora final debe ser mayor que la hora de inicio."
            )


        if (
            etObservaciones
                .text
                .toString()
                .trim()
                .isBlank()
        )
            return mostrarError(
                "Escribe una observación para el horario."
            )


        return true
    }


    private fun mostrarError(
        mensaje: String
    ): Boolean {

        tvError.text =
            mensaje

        tvError.visibility =
            View.VISIBLE

        return false
    }


    private fun agregarBloqueo() {

        val tarjeta =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    18,
                    18,
                    18,
                    18
                )

                setBackgroundResource(
                    R.drawable.bg_card
                )
            }


        val titulo =
            TextView(this).apply {

                text =
                    "📅 ${
                        formatearFechaVisible(
                            fechaSeleccionada
                        )
                    }  •  $horaInicio - $horaFin"

                textSize = 14f

                setTextColor(
                    getColor(
                        R.color.rojo_error
                    )
                )

                setTypeface(
                    null,
                    android.graphics.Typeface.BOLD
                )
            }


        val observacion =
            TextView(this).apply {

                text =
                    etObservaciones
                        .text
                        .toString()
                        .trim()

                textSize = 12f

                setTextColor(
                    getColor(
                        R.color.texto_secundario
                    )
                )

                setPadding(
                    0,
                    8,
                    0,
                    0
                )
            }


        tarjeta.addView(
            titulo
        )

        tarjeta.addView(
            observacion
        )


        val params =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )


        params.setMargins(
            0,
            0,
            0,
            10
        )


        tarjeta.layoutParams =
            params


        containerBloqueos.addView(
            tarjeta
        )


        Toast.makeText(
            this,
            "Horario agregado",
            Toast.LENGTH_SHORT
        ).show()


        etObservaciones
            .text
            .clear()


        horaInicio = null

        horaFin = null


        btnHoraInicio.text =
            "Hora inicio"

        btnHoraFin.text =
            "Hora final"
    }


    private fun guardarFase1YContinuar() {

        if (
            containerBloqueos
                .childCount == 0
        ) {

            mostrarError(
                "Agrega al menos un horario antes de continuar."
            )

            return
        }


        if (guardando)
            return


        guardando = true


        btnContinuar.isEnabled =
            false


        btnContinuar.text =
            "Creando salón..."


        val horaInicioFinal =
            obtenerHoraDelPrimerBloqueo(
                true
            )


        val horaFinFinal =
            obtenerHoraDelPrimerBloqueo(
                false
            )


        val observacionFinal =
            obtenerObservacionPrimerBloqueo()


        if (
            horaInicioFinal == null ||
            horaFinFinal == null
        ) {

            guardando = false

            btnContinuar.isEnabled =
                true

            btnContinuar.text =
                "Guardar disponibilidad y continuar"


            mostrarError(
                "No se pudo leer el primer horario."
            )

            return
        }


        val request =
            SalonCreateFase1Request(

                nombreSalon =
                    nombreSalon,

                capacidadPersonas =
                    capacidadPersonas
                        .toIntOrNull()
                        ?: 0,

                precioHora =
                    precioHora
                        .toDoubleOrNull()
                        ?: 0.0,

                descripcion =
                    descripcion,

                imagenes =
                    imagenes,

                ubicacion =
                    UbicacionRequest(

                        direccion =
                            "$calle $numero, $fraccionamiento",

                        latitud =
                            latitud
                                .toDoubleOrNull()
                                ?: 0.0,

                        longitud =
                            longitud
                                .toDoubleOrNull()
                                ?: 0.0,

                        ciudad =
                            ciudad,

                        codigoPostal =
                            codigoPostal
                    ),

                disponibilidadInicial =
                    DisponibilidadInicialRequest(

                        horaInicio =
                            "${fechaSeleccionada}T${horaInicioFinal}:00",

                        horaFin =
                            "${fechaSeleccionada}T${horaFinFinal}:00",

                        fecha =
                            fechaSeleccionada,

                        observaciones =
                            observacionFinal
                    )
            )


        lifecycleScope.launch {

            try {

                val response =
                    RetrofitClientSalones

                        .getService(
                            this@SalonDisponibilidadActivity
                        )

                        .crearSalonFase1(
                            request
                        )


                if (
                    response.isSuccessful &&
                    response.body() != null
                ) {

                    val idSalon =
                        response.body()!!
                            .idSalon


                    Toast.makeText(
                        this@SalonDisponibilidadActivity,
                        "Datos iniciales guardados",
                        Toast.LENGTH_SHORT
                    ).show()


                    startActivity(

                        Intent(
                            this@SalonDisponibilidadActivity,
                            CatalogoSalonActivity::class.java
                        ).apply {

                            putExtra(
                                "idSalon",
                                idSalon
                            )

                            putExtra(
                                "nombreSalon",
                                nombreSalon
                            )

                            putExtra(
                                "capacidadPersonas",
                                capacidadPersonas
                            )

                            putExtra(
                                "precioHora",
                                precioHora
                            )

                            putExtra(
                                "descripcion",
                                descripcion
                            )

                            putExtra(
                                "ciudad",
                                ciudad
                            )

                            putExtra(
                                "codigoPostal",
                                codigoPostal
                            )

                            putExtra(
                                "calle",
                                calle
                            )

                            putExtra(
                                "numero",
                                numero
                            )

                            putExtra(
                                "fraccionamiento",
                                fraccionamiento
                            )
                        }
                    )


                    finish()

                } else {

                    mostrarError(
                        "No se pudo crear el salón (${response.code()})."
                    )


                    guardando = false

                    btnContinuar.isEnabled =
                        true

                    btnContinuar.text =
                        "Guardar disponibilidad y continuar"
                }

            } catch (e: Exception) {

                mostrarError(
                    "Error de red: ${
                        e.message ?: "sin conexión"
                    }"
                )


                guardando = false

                btnContinuar.isEnabled =
                    true

                btnContinuar.text =
                    "Guardar disponibilidad y continuar"
            }
        }
    }


    private fun obtenerHoraDelPrimerBloqueo(
        inicio: Boolean
    ): String? {

        if (
            containerBloqueos
                .childCount == 0
        )
            return null


        val tarjeta =
            containerBloqueos
                .getChildAt(0)
                    as? LinearLayout
                ?: return null


        val titulo =
            tarjeta.getChildAt(0)
                    as? TextView
                ?: return null


        val partes =
            titulo.text
                .toString()
                .split("•")


        if (
            partes.size < 2
        )
            return null


        val horas =
            partes[1]
                .trim()
                .split("-")


        if (
            horas.size < 2
        )
            return null


        return if (inicio)
            horas[0].trim()
        else
            horas[1].trim()
    }


    private fun obtenerObservacionPrimerBloqueo():
            String? {

        if (
            containerBloqueos
                .childCount == 0
        )
            return null


        val tarjeta =
            containerBloqueos
                .getChildAt(0)
                    as? LinearLayout
                ?: return null


        val observacion =
            tarjeta.getChildAt(1)
                    as? TextView
                ?: return null


        return observacion.text
            .toString()
            .trim()
            .ifBlank {
                null
            }
    }


    private fun formatearFechaVisible(
        iso: String
    ): String {

        val partes =
            iso.split("-")


        return if (
            partes.size == 3
        )
            "${partes[2]}/${partes[1]}/${partes[0]}"
        else
            iso
    }
}
/* Lo de antes
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
}*/