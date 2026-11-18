package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReservacionesInboxFragment : Fragment(R.layout.fragment_reservaciones_inbox) {
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        view?.let { actualizarEstadoAlertas(it) }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val vm = ViewModelProvider(requireActivity())[ReservacionesViewModel::class.java]
        val lista = view.findViewById<RecyclerView>(R.id.rvReservaciones)
        val adapter = ReservacionAdapter { reserva ->
            if (vm.state.value.procesando == null && parentFragmentManager.findFragmentByTag("detalleReserva") == null) {
                vm.seleccion = reserva.id
                ReservacionDetalleBottomSheet().show(parentFragmentManager, "detalleReserva")
            }
        }
        lista.layoutManager = LinearLayoutManager(requireContext())
        lista.adapter = adapter
        val actualizar = view.findViewById<Button>(R.id.btnActualizarReservas)
        actualizar.setOnClickListener { vm.cargar() }
        // Estos controles pertenecen a la vista del fragmento, creada después del onCreate de la Activity.
        view.findViewById<Button>(R.id.btnActivarAlertas).setOnClickListener { configurarAlertas() }
        actualizarEstadoAlertas(view)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    vm.state.collect { s ->
                        adapter.submitList(s.reservas)
                        actualizar.isEnabled = !s.cargando && s.procesando == null
                        view.findViewById<ProgressBar>(R.id.progressReservaciones).isVisible = s.cargando
                        view.findViewById<TextView>(R.id.tvErrorReservaciones).apply { text = s.error; isVisible = s.error != null }
                        view.findViewById<TextView>(R.id.tvReservasVacias).isVisible = !s.cargando && s.error == null && s.reservas.isEmpty()
                        view.findViewById<TextView>(R.id.tvConteoReservas).text = "${s.reservas.size} solicitudes pendientes"
                        if (s.sesionVencida) (activity as? ReservacionesActivity)?.iniciarSesion()
                        s.mensaje?.let { Snackbar.make(view, it, Snackbar.LENGTH_LONG).show(); vm.consumirMensaje() }
                    }
                }
                // Refresco sólo mientras la pantalla está visible.
                launch { while (true) { vm.cargar(); delay(30_000) } }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        view?.let { actualizarEstadoAlertas(it) }
    }

    private fun configurarAlertas() {
        val context = requireContext()
        val prefs = context.getSharedPreferences("ocasion_permisos", Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && !prefs.getBoolean("alertas_solicitadas", false)) {
            prefs.edit().putBoolean("alertas_solicitadas", true).apply()
            permiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            startActivity(Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
        } else {
            startActivity(Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, ReservacionesNotifications.CHANNEL_ID))
        }
    }

    private fun actualizarEstadoAlertas(root: View) {
        root.findViewById<TextView>(R.id.tvEstadoAlertas).text = if (NotificationManagerCompat.from(requireContext()).areNotificationsEnabled())
            "Con la app cerrada, las alertas se revisan periódicamente y pueden tardar 15 minutos o más."
        else "Activa las alertas para recibir avisos de nuevas solicitudes. Puedes revisar la bandeja en cualquier momento."
    }
}
