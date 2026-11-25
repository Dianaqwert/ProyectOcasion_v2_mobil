package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.ui.cliente.mostrarHorarioSalon
import com.example.ocasion_mobil_v2.ui.cliente.pagos.MisReservacionesActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReservaActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val idSalon = intent.getIntExtra("idSalon", -1)

        if (idSalon <= 0) {
            Toast.makeText(
                this,
                "No se encontró el salón.",
                Toast.LENGTH_SHORT
            ).show()

            finish()
            return
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    ReservaPaso1Fragment().apply {
                        arguments = bundleOf("idSalon" to idSalon)
                    }
                )
                .commit()
        }
    }
}

class ReservaPaso1Fragment : Fragment(R.layout.fragment_reserva_paso1) {

    private val vm: ReservaPaso1ViewModel by viewModels()

    private val formatoFecha = DateTimeFormatter.ofPattern(
        "dd MMM yyyy",
        Locale.forLanguageTag("es-MX")
    )

    private val idSalon: Int
        get() = requireArguments().getInt("idSalon")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val invitados =
            view.findViewById<TextInputEditText>(R.id.etInvitadosReserva)

        invitados.setText(vm.state.value.invitadosTexto)

        invitados.doAfterTextChanged {
            vm.cambiarInvitados(it?.toString().orEmpty())
        }

        view.findViewById<View>(R.id.btnVolverReserva)
            .setOnClickListener {
                volver()
            }

        view.findViewById<View>(R.id.btnReintentarReserva)
            .setOnClickListener {
                vm.cargarCalendario()
            }

        view.findViewById<View>(R.id.btnElegirFechas)
            .setOnClickListener {
                abrirCalendario()
            }

        view.findViewById<View>(R.id.btnSiguienteReserva)
            .setOnClickListener {
                avanzar()
            }

        view.findViewById<View>(R.id.btnHoraInicioReserva).setOnClickListener { elegirHora(true) }
        view.findViewById<View>(R.id.btnHoraFinReserva).setOnClickListener { elegirHora(false) }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) { volver() }

        // Si Android recreó el calendario al girar el dispositivo,
        // volvemos a conectar su resultado al ViewModel.
        @Suppress("UNCHECKED_CAST")
        val calendarioRestaurado = childFragmentManager
            .findFragmentByTag("calendario_reserva")
                as? MaterialDatePicker<Long>

        calendarioRestaurado?.let {
            conectarResultadoCalendario(it)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { estado ->
                    mostrarEstado(view, estado)
                }
            }
        }

        vm.iniciar(idSalon)
    }

    private fun abrirCalendario() {
        val estado = vm.state.value

        if (estado.capacidadMaxima == null || estado.cargando) return
        if (childFragmentManager.isStateSaved) return

        if (childFragmentManager.findFragmentByTag("calendario_reserva") != null) {
            return
        }

        val hoy = FechasReserva.paraCalendario(LocalDate.now())

        val restricciones = CalendarConstraints.Builder()
            .setStart(hoy)
            .setOpenAt(hoy)
            .setValidator(
                FechasReservaValidator(
                    estado.fechasBloqueadas
                        .map { it.toEpochDay() }
                        .toLongArray()
                )
            )
            .build()

        val constructor = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Fecha de tu evento")
            .setCalendarConstraints(restricciones)

        estado.fecha?.let { constructor.setSelection(FechasReserva.paraCalendario(it)) }

        val calendario = constructor.build()

        conectarResultadoCalendario(calendario)

        calendario.show(
            childFragmentManager,
            "calendario_reserva"
        )
    }

    private fun conectarResultadoCalendario(calendario: MaterialDatePicker<Long>) {
        calendario.clearOnPositiveButtonClickListeners()
        calendario.addOnPositiveButtonClickListener(vm::seleccionarFecha)
    }

    private fun elegirHora(esInicio: Boolean) {
        val estado = vm.state.value
        val hora = if (esInicio) estado.horaInicio else estado.horaFin
        TimePickerDialog(requireContext(), { _, h, m ->
            vm.seleccionarHora(esInicio, LocalTime.of(h, m))
        }, hora.hour, hora.minute, true).apply {
            setTitle(if (esInicio) "Hora de inicio" else "Hora de fin")
            show()
        }
    }

    private fun volver() {
        val estado = vm.state.value
        if (estado.enviando) return
        if (estado.resumen && estado.folio == null && !estado.envioIncierto) vm.mostrarResumen(false)
        else requireActivity().finish()
    }

    private fun avanzar() {
        val estado = vm.state.value
        when {
            estado.folio != null || estado.envioIncierto -> {
                startActivity(Intent(requireContext(), MisReservacionesActivity::class.java))
                requireActivity().finish()
            }
            estado.resumen -> vm.enviar()
            else -> {
                view?.let { root ->
                    val teclado = requireContext().getSystemService(android.view.inputmethod.InputMethodManager::class.java)
                    teclado.hideSoftInputFromWindow(root.windowToken, 0)
                }
                vm.mostrarResumen(true)
            }
        }
    }

    private fun mostrarEstado(view: View, estado: ReservaPaso1State) {
        val resultado = estado.folio != null || estado.envioIncierto
        view.findViewById<View>(R.id.tarjetaHorarioReserva)
            .mostrarHorarioSalon(estado.horarioSalon.takeUnless { resultado || estado.resumen })
        view.findViewById<View>(R.id.progresoReserva).isVisible = estado.cargando || estado.enviando
        view.findViewById<View>(R.id.formularioReserva).isVisible =
            !estado.resumen && !resultado && estado.capacidadMaxima != null && !estado.cargando
        view.findViewById<View>(R.id.btnReintentarReserva).isVisible = estado.errorCarga != null && !resultado
        view.findViewById<View>(R.id.btnVolverReserva).apply {
            isEnabled = !estado.enviando
            contentDescription = if (estado.resumen && !resultado) "Editar los datos" else "Volver al salón"
        }
        view.findViewById<TextView>(R.id.tvEtapaReserva).text = when {
            estado.folio != null -> "Solicitud enviada"
            estado.envioIncierto -> "Comprueba tu solicitud"
            estado.resumen -> "Resumen de la solicitud"
            else -> "Fecha, horario e invitados"
        }
        view.findViewById<TextView>(R.id.tvEstadoReserva).text = when {
            estado.folio != null ->
                "Solicitud #${estado.folio} enviada al propietario. Puedes seguirla en Mis reservaciones."
            estado.envioIncierto ->
                "No pudimos confirmar el envío. Revisa Mis reservaciones antes de crear otra solicitud."
            estado.enviando -> "Enviando solicitud…"
            estado.cargando -> "Consultando disponibilidad…"
            estado.errorEnvio != null -> estado.errorEnvio
            estado.errorCarga != null -> estado.errorCarga
            estado.resumen -> estado.errorFecha ?: estado.errorInvitados.orEmpty()
            estado.capacidadMaxima != null -> "Capacidad máxima: ${estado.capacidadMaxima} personas"
            else -> ""
        }

        view.findViewById<TextInputLayout>(R.id.tilInvitadosReserva).error = estado.errorInvitados
        view.findViewById<TextView>(R.id.tvFechasReserva).text =
            estado.fecha?.format(formatoFecha) ?: "Sin fecha seleccionada"
        view.findViewById<TextView>(R.id.tvErrorFechasReserva).apply {
            val error = estado.errorFecha ?: estado.errorHorario
            text = error
            isVisible = error != null
        }
        view.findViewById<MaterialButton>(R.id.btnHoraInicioReserva).text = "Inicio · ${estado.horaInicio}"
        view.findViewById<MaterialButton>(R.id.btnHoraFinReserva).text = "Fin · ${estado.horaFin}"

        view.findViewById<TextView>(R.id.tvResumenReserva).apply {
            isVisible = estado.resumen && !resultado
            if (isVisible) {
                val args = requireActivity().intent
                text = buildString {
                    appendLine(args.getStringExtra("nombreSalon") ?: "Salón #$idSalon")
                    appendLine("${estado.invitadosTexto} invitados")
                    appendLine(estado.fecha?.format(formatoFecha) ?: "Selecciona otra fecha")
                    append("${estado.horaInicio} a ${estado.horaFin} h")
                    val servicios = args.getStringArrayListExtra("serviciosDelSalon").orEmpty()
                    if (servicios.isNotEmpty()) {
                        append("\n\nServicios del salón\n")
                        append(servicios.joinToString("\n") { "• $it" })
                    }
                }
            }
        }

        view.findViewById<MaterialButton>(R.id.btnSiguienteReserva).apply {
            isEnabled = resultado || estado.puedeContinuar
            text = when {
                resultado -> "Ver mis reservaciones"
                estado.resumen -> "Confirmar y enviar solicitud"
                else -> "Ver resumen"
            }
        }
    }
}
