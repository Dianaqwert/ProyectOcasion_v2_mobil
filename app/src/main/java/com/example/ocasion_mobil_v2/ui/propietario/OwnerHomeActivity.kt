package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import android.widget.LinearLayout

class OwnerHomeActivity : ComponentActivity() {

    private lateinit var cardNuevoSalon: LinearLayout
    private lateinit var cardMisSalones: LinearLayout
    private lateinit var cardDisponibilidad: LinearLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.owner_home)

        bindViews()
        configurarNavegacion()
        BottomNav.configurar(this)
    }

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