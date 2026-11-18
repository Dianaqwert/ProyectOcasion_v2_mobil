package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Button
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReservacionesActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

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
        BottomNav.configurar(this)
        findViewById<TextView>(R.id.btnNotifications).setOnClickListener { abrirSolicitudes() }
        findViewById<Button>(R.id.btnSolicitudes).setOnClickListener { abrirSolicitudes() }
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
