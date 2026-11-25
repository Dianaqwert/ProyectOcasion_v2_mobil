package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.repository.ResultadoCheckIn
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

class CheckInActivity : ComponentActivity() {
    private val vm: CheckInViewModel by viewModels()
    private val lector = registerForActivityResult(ScanContract()) { resultado ->
        resultado.contents?.let(vm::validar)
    }
    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) abrirCamara()
        else vm.mostrarProblema("Activa el permiso de cámara en los ajustes de Ocasion para escanear el pase.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!vm.sesionVigente()) {
            Toast.makeText(this, "Inicia sesión como propietario para controlar el acceso.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setContentView(R.layout.activity_check_in)
        val pantalla = findViewById<View>(R.id.pantallaCheckIn)
        ViewCompat.setOnApplyWindowInsetsListener(pantalla) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(pantalla)
        findViewById<Button>(R.id.btnVolverCheckIn).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnEscanearCheckIn).setOnClickListener { prepararCamara() }
        findViewById<Button>(R.id.btnVerReservaCheckIn).setOnClickListener {
            val resultado = vm.estado.value.resultado as? ResultadoCheckIn.Concedido ?: return@setOnClickListener
            startActivity(Intent(this, PanelPropietarioActivity::class.java)
                .putExtra(PanelPropietarioActivity.EXTRA_RESERVA_CON_LLEGADA, resultado.reserva.id))
            finish()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { vm.estado.collect(::mostrarEstado) }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!vm.sesionVigente()) finish()
    }

    private fun prepararCamara() {
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
            vm.mostrarProblema("Este dispositivo no tiene una cámara disponible.")
        } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            permiso.launch(Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        if (vm.estado.value.validando || !vm.sesionVigente()) return
        lector.launch(ScanOptions().apply {
            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            setPrompt("Encuadra el código QR del cliente")
            setBeepEnabled(false)
            setOrientationLocked(false)
        })
    }

    private fun mostrarEstado(estado: CheckInViewModel.Estado) {
        val resultado = estado.resultado
        val titulo = findViewById<TextView>(R.id.tituloResultadoCheckIn)
        val detalle = findViewById<TextView>(R.id.detalleResultadoCheckIn)
        val pantalla = findViewById<View>(R.id.pantallaCheckIn)
        findViewById<Button>(R.id.btnVerReservaCheckIn).isVisible =
            resultado is ResultadoCheckIn.Concedido && !estado.validando
        findViewById<View>(R.id.progresoCheckIn).isVisible = estado.validando
        findViewById<Button>(R.id.btnEscanearCheckIn).apply {
            isEnabled = !estado.validando
            text = if (resultado == null) "Escanear código QR" else "Escanear otro código"
        }
        when (resultado) {
            is ResultadoCheckIn.Concedido -> {
                pantalla.setBackgroundColor(getColor(R.color.check_in_concedido))
                titulo.text = "Acceso concedido"
                detalle.text = listOfNotNull(
                    "Reserva #${resultado.reserva.id}",
                    resultado.reserva.nombreCliente,
                    resultado.reserva.nombreSalon,
                    "Llegada registrada · ${resultado.llegada.format(formatoLlegadaPropietario)}"
                ).joinToString("\n")
            }
            is ResultadoCheckIn.Rechazado -> {
                pantalla.setBackgroundColor(getColor(R.color.check_in_rechazado))
                titulo.text = "Acceso rechazado"
                detalle.text = resultado.motivo
            }
            is ResultadoCheckIn.SinConfirmar -> {
                pantalla.setBackgroundColor(getColor(R.color.reserva_superficie_destacada))
                titulo.text = "Acceso sin confirmar"
                detalle.text = resultado.motivo
            }
            null -> {
                pantalla.setBackgroundColor(getColor(R.color.reserva_fondo))
                titulo.text = if (estado.validando) "Validando pase…" else "Registrar llegada"
                detalle.text = if (estado.validando) "Consultando la reserva y registrando el acceso."
                    else "Escanea el pase del cliente para comprobar su reserva y registrar su llegada."
            }
        }
    }
}
