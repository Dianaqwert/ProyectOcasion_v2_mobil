package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
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
                requireActivity().finish()
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
                abrirServicios()
            }

        // Si Android recreó el calendario al girar el dispositivo,
        // volvemos a conectar su resultado al ViewModel.
        @Suppress("UNCHECKED_CAST")
        val calendarioRestaurado = childFragmentManager
            .findFragmentByTag("calendario_reserva")
                as? MaterialDatePicker<androidx.core.util.Pair<Long, Long>>

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

    override fun onResume() {
        super.onResume()
        vm.revalidarFechas()
    }

    private fun abrirCalendario() {
        val estado = vm.state.value

        if (!estado.calendarioCargado || estado.cargando) return
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

        val constructor = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Fechas de tu evento")
            .setCalendarConstraints(restricciones)

        val inicio = estado.inicio
        val fin = estado.fin

        if (inicio != null && fin != null) {
            constructor.setSelection(
                androidx.core.util.Pair(
                    FechasReserva.paraCalendario(inicio),
                    FechasReserva.paraCalendario(fin)
                )
            )
        }

        val calendario = constructor.build()

        conectarResultadoCalendario(calendario)

        calendario.show(
            childFragmentManager,
            "calendario_reserva"
        )
    }

    private fun conectarResultadoCalendario(
        calendario: MaterialDatePicker<androidx.core.util.Pair<Long, Long>>
    ) {
        calendario.clearOnPositiveButtonClickListeners()

        calendario.addOnPositiveButtonClickListener { seleccion ->
            val inicio = seleccion.first
            val fin = seleccion.second

            if (inicio != null && fin != null) {
                vm.seleccionarFechas(inicio, fin)
            }
        }
    }

    private fun abrirServicios() {
        vm.revalidarFechas()

        val estado = vm.state.value
        if (!estado.puedeContinuar) return
        if (parentFragmentManager.isStateSaved) return

        val invitados = estado.numeroInvitados ?: return
        val inicio = estado.inicio ?: return
        val fin = estado.fin ?: return

        val fragmento = ReservaServiciosFragment().apply {
            arguments = bundleOf(
                "idSalon" to idSalon,
                "fechaInicio" to inicio.toString(),
                "fechaFin" to fin.toString(),
                "numeroInvitados" to invitados
            )
        }

        parentFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragmento)
            .addToBackStack("paso1_reserva")
            .commit()
    }

    private fun mostrarEstado(
        view: View,
        estado: ReservaPaso1State
    ) {
        view.findViewById<View>(R.id.progresoReserva)
            .isVisible = estado.cargando

        view.findViewById<View>(R.id.formularioReserva)
            .isVisible = estado.calendarioCargado && !estado.cargando

        view.findViewById<View>(R.id.btnReintentarReserva)
            .isVisible = estado.errorCarga != null

        view.findViewById<TextView>(R.id.tvEstadoReserva).text = when {
            estado.cargando -> "Consultando disponibilidad…"

            estado.errorCarga != null -> estado.errorCarga

            estado.calendarioCargado ->
                "Capacidad máxima: ${estado.capacidadMaxima} personas"

            else -> ""
        }

        view.findViewById<TextInputLayout>(R.id.tilInvitadosReserva)
            .error = estado.errorInvitados

        val inicio = estado.inicio
        val fin = estado.fin

        view.findViewById<TextView>(R.id.tvFechasReserva).text =
            if (inicio != null && fin != null) {
                "${inicio.format(formatoFecha)} — ${fin.format(formatoFecha)}"
            } else {
                "Sin fechas seleccionadas"
            }

        view.findViewById<TextView>(R.id.tvErrorFechasReserva).apply {
            text = estado.errorFechas
            isVisible = estado.errorFechas != null
        }

        view.findViewById<MaterialButton>(R.id.btnSiguienteReserva).apply {
            isEnabled = estado.puedeContinuar

            backgroundTintList = ColorStateList.valueOf(
                requireContext().getColor(
                    if (estado.puedeContinuar) {
                        R.color.verde_primario
                    } else {
                        android.R.color.darker_gray
                    }
                )
            )
        }
    }
}

class ReservaServiciosFragment :
    Fragment(R.layout.fragment_reserva_servicios) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val argumentos = requireArguments()

        val formato = DateTimeFormatter.ofPattern(
            "dd MMM yyyy",
            Locale.forLanguageTag("es-MX")
        )

        val inicio = LocalDate.parse(
            argumentos.getString("fechaInicio")
        )

        val fin = LocalDate.parse(
            argumentos.getString("fechaFin")
        )

        val invitados = argumentos.getInt("numeroInvitados")

        view.findViewById<TextView>(R.id.tvResumenReservaServicios).text =
            "$invitados invitados\n" +
                    "${inicio.format(formato)} — ${fin.format(formato)}"

        view.findViewById<View>(R.id.btnEditarFechasReserva)
            .setOnClickListener {
                parentFragmentManager.popBackStack()
            }
    }
}