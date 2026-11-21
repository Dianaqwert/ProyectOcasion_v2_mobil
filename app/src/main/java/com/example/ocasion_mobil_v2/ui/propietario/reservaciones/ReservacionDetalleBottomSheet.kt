package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocasion_mobil_v2.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ReservacionDetalleBottomSheet : BottomSheetDialogFragment(R.layout.bottom_sheet_reservacion) {
    private val vm by lazy { ViewModelProvider(requireActivity())[ReservacionesViewModel::class.java] }
    private var rechazo: androidx.appcompat.app.AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view.findViewById<Button>(R.id.btnCerrarDetalle).setOnClickListener { dismiss() }
        view.findViewById<Button>(R.id.btnAprobarReserva).setOnClickListener { vm.seleccion?.let { vm.dictaminar(it, true) } }
        view.findViewById<Button>(R.id.btnRechazarReserva).setOnClickListener { pedirMotivo() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { s ->
                    val r = s.reservas.find { it.id == vm.seleccion }
                    if (r == null) { if (!s.cargando) dismiss(); return@collect }
                    fun texto(id: Int, value: String?) { view.findViewById<TextView>(id).text = value ?: "No disponible" }
                    texto(R.id.tvDetalleSalon, r.nombreSalon ?: "Salón #${r.idSalon ?: "sin dato"}")
                    texto(R.id.tvDetalleReferencia, "Solicitud #${r.id}")
                    texto(R.id.tvDetalleCliente, r.nombreCliente)
                    texto(R.id.tvDetalleContacto, "Correo: ${r.correoCliente ?: "No disponible"}\nTeléfono: ${r.telefonoCliente ?: "No disponible"}")
                    texto(R.id.tvDetalleFechas, fechasReserva(r))
                    texto(R.id.tvDetalleInvitados, "${r.numeroInvitados?.toString() ?: "No disponible"} invitados")
                    texto(R.id.tvDetalleServicios, r.servicios.joinToString("\n") {
                        "• ${it.nombre ?: "Servicio #${it.id ?: "sin dato"}"} · Cantidad: ${it.cantidad ?: "No disponible"}" +
                            (it.precio?.let { precio -> " · Precio contratado: $precio" } ?: "")
                    }.ifEmpty { "Sin servicios registrados en esta solicitud." })
                    texto(R.id.tvDetalleInventario, r.inventario.joinToString("\n") {
                        "• ${it.nombre ?: "Recurso #${it.id ?: "sin dato"}"} · ${it.cantidad ?: "No disponible"} unidades retenidas"
                    }.ifEmpty { "Sin inventario retenido registrado en esta solicitud." })
                    texto(R.id.tvDetalleObservaciones, r.observaciones ?: "Sin observaciones.")
                    val ocupado = s.procesando != null || s.cargando
                    isCancelable = s.procesando == null
                    listOf(R.id.btnAprobarReserva, R.id.btnRechazarReserva, R.id.btnCerrarDetalle).forEach {
                        view.findViewById<Button>(it).isEnabled = !ocupado
                    }
                    view.findViewById<Button>(R.id.btnAprobarReserva).setText(if (s.procesando == r.id) R.string.reserva_procesando else R.string.reserva_aprobar)
                    view.findViewById<TextView>(R.id.tvDetalleError).apply { text = s.error; isVisible = s.error != null }
                }
            }
        }
    }

    private fun pedirMotivo() {
        if (rechazo?.isShowing == true) return

        val content = layoutInflater.inflate(
            R.layout.dialog_rechazar_reservacion,
            null
        )

        val input = content.findViewById<TextInputEditText>(
            R.id.etMotivoRechazo
        )

        val campo = content.findViewById<TextInputLayout>(
            R.id.campoMotivoRechazo
        )

        input.setText(vm.obtenerMotivo())

        rechazo = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.reserva_rechazar)
            .setMessage(
                "Puedes indicar un motivo (opcional). " +
                        "El servidor procesará el rechazo y la liberación " +
                        "de la fecha y los recursos asociados."
            )
            .setView(content)
            .setNegativeButton(R.string.reserva_cancelar, null)
            .setPositiveButton(R.string.reserva_rechazar, null)
            .create()
            .apply {

                setOnShowListener {
                    getButton(
                        DialogInterface.BUTTON_POSITIVE
                    ).setOnClickListener {

                        if (
                            vm.state.value.cargando ||
                            vm.state.value.procesando != null
                        ) {
                            campo.error =
                                "Espera a que termine la actualización."
                            return@setOnClickListener
                        }

                        val motivo = input.text
                            ?.toString()
                            ?.trim()
                            .orEmpty()

                        vm.seleccion?.let {
                            vm.dictaminar(it, false, motivo)
                        }

                        dismiss()
                    }
                }

                setOnDismissListener {
                    vm.guardarMotivo(
                        input.text?.toString().orEmpty()
                    )
                }

                show()
            }
    }

    override fun onDestroyView() { rechazo?.dismiss(); rechazo = null; super.onDestroyView() }
    override fun onDismiss(dialog: DialogInterface) {
        if (!requireActivity().isChangingConfigurations) vm.seleccion = null
        super.onDismiss(dialog)
    }
}
