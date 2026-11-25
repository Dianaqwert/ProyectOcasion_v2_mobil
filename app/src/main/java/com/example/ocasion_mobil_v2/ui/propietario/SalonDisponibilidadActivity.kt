package com.example.ocasion_mobil_v2.ui.propietario

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.ui.legal.ConfirmacionLegal
import com.example.ocasion_mobil_v2.ui.legal.ContextoAviso
import com.example.ocasion_mobil_v2.data.remote.model.DisponibilidadInicialRequest
import com.example.ocasion_mobil_v2.data.remote.model.HorarioSalonDTO
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionRequest
import com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon.CatalogoSalonActivity
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId

class SalonDisponibilidadActivity : ComponentActivity() {
    private val modelo: DisponibilidadSalonViewModel by viewModels()
    private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
    private val nombre get() = intent.getStringExtra("nombreSalon").orEmpty()
    private val idSalon get() = intent.getIntExtra("idSalon", 0).takeIf { it > 0 }

    private var fechaDesde = LocalDate.now()
    private var apertura = LocalTime.MIDNIGHT
    private var cierre = LocalTime.of(23, 59)
    private var fechaBloqueo = LocalDate.now()
    private var inicioBloqueo = LocalTime.MIDNIGHT
    private var finBloqueo = LocalTime.of(23, 59)
    private var horarioCargado = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_salon_disponibilidad)
        if (intent.hasExtra("idSalon") && idSalon == null) {
            mostrarError("El identificador del salón no es válido.")
            findViewById<View>(R.id.formularioDisponibilidad).isVisible = false
            boton(R.id.btnVolverDisponibilidad).setOnClickListener { finish() }
            return
        }
        recuperarFormulario(savedInstanceState)
        modelo.iniciar(
            idSalon, savedInstanceState?.getInt("idCreado", 0)?.takeIf { it > 0 },
            savedInstanceState?.getParcelableArrayList<Bundle>("pendientes").orEmpty().map {
                DisponibilidadInicialRequest(
                    it.getString("inicio").orEmpty(), it.getString("fin").orEmpty(),
                    it.getString("fecha").orEmpty(), it.getString("nota")
                )
            }
        )
        findViewById<TextView>(R.id.tituloDisponibilidad).text =
            if (nombre.isBlank()) "Disponibilidad" else "Disponibilidad · $nombre"
        idSalon?.let { id ->
            val estado = when (intent.getStringExtra("estadoSalon")) {
                "PUBLICADO" -> getString(R.string.salon_visible_cliente)
                "BORRADOR" -> getString(R.string.salon_borrador_oculto)
                else -> null
            }
            findViewById<TextView>(R.id.identificadorDisponibilidad).apply {
                isVisible = true
                text = if (estado == null) getString(R.string.salon_identificador, id.toString())
                    else getString(R.string.salon_identificador_estado, id.toString(), estado)
            }
        }
        boton(R.id.btnVolverDisponibilidad).setOnClickListener { finish() }
        boton(R.id.btnFechaDesde).setOnClickListener {
            elegirFecha(fechaDesde) { fechaDesde = it }
        }
        boton(R.id.btnApertura).setOnClickListener {
            elegirHora(apertura) { apertura = it }
        }
        boton(R.id.btnCierre).setOnClickListener {
            elegirHora(cierre) { cierre = it }
        }
        boton(R.id.btnFechaBloqueo).setOnClickListener {
            elegirFecha(fechaBloqueo, futura = true) { fechaBloqueo = it }
        }
        boton(R.id.btnInicioBloqueo).setOnClickListener {
            elegirHora(inicioBloqueo) { inicioBloqueo = it }
        }
        boton(R.id.btnFinBloqueo).setOnClickListener {
            elegirHora(finBloqueo) { finBloqueo = it }
        }
        boton(R.id.btnGuardarHorario).setOnClickListener {
            val id = idSalon ?: return@setOnClickListener
            horarioValidado()?.let { modelo.guardarHorario(id, it) }
        }
        boton(R.id.btnAgregarBloqueo).setOnClickListener { agregarBloqueo() }
        val panelAviso = findViewById<View>(R.id.panelConfirmacionLegal)
        panelAviso.isVisible = idSalon == null
        val aviso = ConfirmacionLegal(panelAviso, ContextoAviso.SALON)
        boton(R.id.btnContinuarDisponibilidad).setOnClickListener {
            if (!aviso.verificar()) return@setOnClickListener
            horarioValidado()?.let { modelo.crearYContinuar(solicitudAlta(it)) }
        }
        boton(R.id.btnRecargarDisponibilidad).setOnClickListener { idSalon?.let(modelo::cargar) }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                modelo.estado.collect(::mostrarEstado)
            }
        }
    }

    private fun mostrarEstado(estado: DisponibilidadSalonViewModel.Estado) {
        if (idSalon != null && estado.cargado && !horarioCargado) {
            horarioCargado = true
            estado.horario?.let {
                fechaDesde = LocalDate.parse(it.fechaDesde)
                apertura = LocalTime.parse(it.horaInicio)
                cierre = LocalTime.parse(it.horaFin)
            }
        }
        pintarCampos()
        findViewById<View>(R.id.formularioDisponibilidad).isVisible = estado.cargado
        findViewById<View>(R.id.progresoDisponibilidad).isVisible = estado.ocupado
        boton(R.id.btnRecargarDisponibilidad).isVisible = !estado.cargado && !estado.ocupado && estado.error != null
        boton(R.id.btnGuardarHorario).isVisible = idSalon != null
        boton(R.id.btnContinuarDisponibilidad).apply {
            isVisible = idSalon == null
            text = if (estado.idCreado == null) "Guardar y continuar" else "Continuar al catálogo"
        }
        val editable = !estado.ocupado
        listOf(R.id.btnFechaDesde, R.id.btnApertura, R.id.btnCierre).forEach {
            boton(it).isEnabled = editable && estado.idCreado == null
        }
        listOf(R.id.btnFechaBloqueo, R.id.btnInicioBloqueo, R.id.btnFinBloqueo, R.id.notaBloqueo,
            R.id.btnGuardarHorario, R.id.btnAgregarBloqueo, R.id.btnContinuarDisponibilidad).forEach {
            findViewById<View>(it).isEnabled = editable
        }
        mostrarError(estado.error)
        pintarBloqueos(estado)
        estado.aviso?.let {
            if (it.startsWith("Bloqueo") && it != "Bloqueo eliminado.") nota().text.clear()
            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            modelo.consumirAviso()
        }
        if (estado.abrirCatalogo) {
            startActivity(Intent(this, CatalogoSalonActivity::class.java).apply {
                intent.extras?.let { putExtras(it) }
                putExtra("idSalon", checkNotNull(estado.idCreado))
            })
            finish()
        }
    }

    private fun pintarCampos() {
        boton(R.id.btnFechaDesde).text = "Disponible desde · ${fechaDesde.format(formatoFecha)}"
        boton(R.id.btnApertura).text = "Apertura · ${apertura.format(formatoHora)}"
        boton(R.id.btnCierre).text = "Cierre · ${cierre.format(formatoHora)}"
        boton(R.id.btnFechaBloqueo).text = "Fecha · ${fechaBloqueo.format(formatoFecha)}"
        boton(R.id.btnInicioBloqueo).text = "Inicio · ${inicioBloqueo.format(formatoHora)}"
        boton(R.id.btnFinBloqueo).text = "Fin · ${finBloqueo.format(formatoHora)}"
    }

    private fun pintarBloqueos(estado: DisponibilidadSalonViewModel.Estado) {
        val lista = findViewById<LinearLayout>(R.id.listaBloqueos)
        lista.removeAllViews()
        val id = idSalon
        if (id == null) {
            estado.pendientes.forEach { bloqueo ->
                agregarFila(lista, bloqueo.horaInicio, bloqueo.horaFin, bloqueo.observaciones, estado.ocupado) {
                    modelo.quitarPendiente(bloqueo)
                }
            }
        } else {
            estado.bloqueos.forEach { bloqueo ->
                agregarFila(lista, bloqueo.horaInicio, bloqueo.horaFin, bloqueo.observaciones, estado.ocupado) {
                    AlertDialog.Builder(this)
                        .setTitle("Eliminar bloqueo")
                        .setMessage("El intervalo volverá a estar disponible si no hay otro bloqueo.")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Eliminar") { _, _ -> modelo.eliminarBloqueo(id, bloqueo.id) }
                        .show()
                }
            }
        }
        if (lista.childCount == 0) {
            lista.addView(TextView(this).apply { text = "Sin bloqueos registrados" })
        }
    }

    private fun agregarFila(
        lista: LinearLayout, desde: String, hasta: String, observacion: String?,
        ocupado: Boolean, quitar: () -> Unit
    ) {
        val inicio = LocalDateTime.parse(desde)
        val fin = LocalDateTime.parse(hasta)
        val intervalo = "${inicio.format(formatoFecha)} · ${inicio.format(formatoHora)} a ${fin.format(formatoHora)}"
        lista.addView(TextView(this).apply {
            setPadding(0, (12 * resources.displayMetrics.density).toInt(), 0, 0)
            text = listOfNotNull(intervalo, observacion?.takeIf { it.isNotBlank() }).joinToString("\n")
        })
        lista.addView(Button(this).apply {
            text = "Quitar bloqueo"
            contentDescription = "Quitar bloqueo del $intervalo"
            isEnabled = !ocupado
            setOnClickListener { quitar() }
        })
    }

    private fun agregarBloqueo() {
        if (fechaBloqueo.isBefore(LocalDate.now()) || !finBloqueo.isAfter(inicioBloqueo)) {
            mostrarError("Elige una fecha vigente y una hora de fin posterior a la de inicio.")
            return
        }
        modelo.agregarBloqueo(idSalon, DisponibilidadInicialRequest(
            fechaBloqueo.atTime(inicioBloqueo).toString(),
            fechaBloqueo.atTime(finBloqueo).toString(),
            fechaBloqueo.toString(),
            nota().text.toString().trim().ifBlank { null }
        ))
    }

    private fun horarioValidado(): HorarioSalonDTO? {
        if (!cierre.isAfter(apertura)) {
            mostrarError("La hora de cierre debe ser posterior a la de apertura.")
            return null
        }
        return HorarioSalonDTO(fechaDesde.toString(), apertura.toString(), cierre.toString())
    }

    private fun solicitudAlta(horario: HorarioSalonDTO): SalonCreateFase1Request {
        fun dato(clave: String) = intent.getStringExtra(clave).orEmpty()
        return SalonCreateFase1Request(
            nombreSalon = nombre,
            capacidadPersonas = dato("capacidadPersonas").toIntOrNull() ?: 0,
            precioHora = dato("precioHora").toDoubleOrNull() ?: 0.0,
            descripcion = dato("descripcion"),
            imagenes = intent.getStringArrayListExtra("imagenes").orEmpty(),
            ubicacion = UbicacionRequest(
                direccion = "${dato("calle")} ${dato("numero")}, ${dato("fraccionamiento")}",
                latitud = dato("latitud").toDoubleOrNull() ?: 0.0,
                longitud = dato("longitud").toDoubleOrNull() ?: 0.0,
                ciudad = dato("ciudad"),
                codigoPostal = dato("codigoPostal")
            ),
            disponibilidadInicial = horario.comoDisponibilidadInicial()
        )
    }

    private fun elegirFecha(fecha: LocalDate, futura: Boolean = false, usar: (LocalDate) -> Unit) {
        val dialogo = DatePickerDialog(this, { _, anio, mes, dia ->
            usar(LocalDate.of(anio, mes + 1, dia))
            pintarCampos()
        }, fecha.year, fecha.monthValue - 1, fecha.dayOfMonth)
        if (futura) dialogo.datePicker.minDate =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        dialogo.show()
    }

    private fun elegirHora(hora: LocalTime, usar: (LocalTime) -> Unit) {
        TimePickerDialog(this, { _, horas, minutos ->
            usar(LocalTime.of(horas, minutos))
            pintarCampos()
        }, hora.hour, hora.minute, true).show()
    }

    private fun mostrarError(error: String?) {
        findViewById<TextView>(R.id.errorDisponibilidad).apply {
            text = error
            isVisible = error != null
        }
        if (error != null) Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
    }

    private fun recuperarFormulario(datos: Bundle?) {
        datos ?: return
        fechaDesde = datos.getString("fechaDesde")?.let(LocalDate::parse) ?: fechaDesde
        apertura = datos.getString("apertura")?.let(LocalTime::parse) ?: apertura
        cierre = datos.getString("cierre")?.let(LocalTime::parse) ?: cierre
        fechaBloqueo = datos.getString("fechaBloqueo")?.let(LocalDate::parse) ?: fechaBloqueo
        inicioBloqueo = datos.getString("inicioBloqueo")?.let(LocalTime::parse) ?: inicioBloqueo
        finBloqueo = datos.getString("finBloqueo")?.let(LocalTime::parse) ?: finBloqueo
        horarioCargado = datos.getBoolean("horarioCargado")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("fechaDesde", fechaDesde.toString())
        outState.putString("apertura", apertura.toString())
        outState.putString("cierre", cierre.toString())
        outState.putString("fechaBloqueo", fechaBloqueo.toString())
        outState.putString("inicioBloqueo", inicioBloqueo.toString())
        outState.putString("finBloqueo", finBloqueo.toString())
        outState.putBoolean("horarioCargado", horarioCargado)
        val estado = modelo.estado.value
        outState.putInt("idCreado", estado.idCreado ?: 0)
        outState.putParcelableArrayList("pendientes", ArrayList(estado.pendientes.map {
            Bundle().apply {
                putString("fecha", it.fecha)
                putString("inicio", it.horaInicio)
                putString("fin", it.horaFin)
                putString("nota", it.observaciones)
            }
        }))
    }

    private fun boton(id: Int) = findViewById<Button>(id)
    private fun nota() = findViewById<EditText>(R.id.notaBloqueo)
}

