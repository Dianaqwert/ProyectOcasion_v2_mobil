package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReservacionesActivity
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.CheckInActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.login.LoginActivity

class OwnerHomeActivity : ComponentActivity() {
    private val permisoAlertas = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private lateinit var cardNuevoSalon: LinearLayout
    private lateinit var cardMisSalones: LinearLayout
    private lateinit var cardDisponibilidad: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.owner_home)

        bindViews()
        configurarNavegacion()
        findViewById<Button>(R.id.btnCerrarSesionPropietario).setOnClickListener {
            SessionManager(this).cerrarSesion()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        BottomNav.configurar(this)
        findViewById<TextView>(R.id.btnNotifications).setOnClickListener { abrirSolicitudes() }
        findViewById<Button>(R.id.btnSolicitudes).setOnClickListener { abrirSolicitudes() }
        findViewById<Button>(R.id.btnCheckInPropietario).setOnClickListener {
            startActivity(Intent(this, CheckInActivity::class.java))
        }
        val preferencias = getSharedPreferences("ocasion_permisos", MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 && !preferencias.getBoolean("alertas_solicitadas", false) &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            preferencias.edit().putBoolean("alertas_solicitadas", true).apply()
            permisoAlertas.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun abrirSolicitudes() { startActivity(Intent(this, ReservacionesActivity::class.java)) }

    private fun bindViews() {
        cardNuevoSalon = findViewById(R.id.cardNewSalon)
        cardMisSalones = findViewById(R.id.cardMySalons)
        cardDisponibilidad = findViewById(R.id.cardDisponibilidad)

    }

    private fun configurarNavegacion() {

        // NUEVO SALÓN
        cardNuevoSalon.setOnClickListener {
            startActivity(
                Intent(this, SalonFormActivity::class.java)
            )
        }

        // MIS SALONES
        cardMisSalones.setOnClickListener {
            startActivity(
                Intent(this, MisSalonesActivity::class.java)
            )
        }

        // DISPONIBILIDAD
        cardDisponibilidad.setOnClickListener {

            startActivity(
                Intent(
                    this,
                    MisSalonesActivity::class.java
                ).apply {
                    putExtra(
                        "abrirDisponibilidad",
                        true
                    )
                }
            )
        }

    }
}
