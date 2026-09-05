package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import com.google.android.material.button.MaterialButton

class SalonPublicacionActivity : ComponentActivity() {

    private lateinit var tvNombre: TextView
    private lateinit var tvCapacidad: TextView
    private lateinit var tvPrecio: TextView
    private lateinit var tvDescripcion: TextView
    private lateinit var tvDireccion: TextView

    private var nombreSalon = ""
    private var capacidadPersonas = ""
    private var descripcion = ""
    private var precioHora = ""

    private var ciudad = ""
    private var codigoPostal = ""
    private var calle = ""
    private var numero = ""
    private var fraccionamiento = ""
    private var latitud = ""
    private var longitud = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_salon_publicacion
        )

        recuperarDatos()
        inicializarVistas()
        mostrarDatos()
        configurarBotones()
    }

    private fun recuperarDatos() {

        nombreSalon =
            intent.getStringExtra("nombreSalon")
                ?: "Salón Jardín"

        capacidadPersonas =
            intent.getStringExtra("capacidadPersonas")
                ?: "150"

        descripcion =
            intent.getStringExtra("descripcion")
                ?: "Espacio ideal para celebraciones y eventos."

        precioHora =
            intent.getStringExtra("precioHora")
                ?: "850"

        ciudad =
            intent.getStringExtra("ciudad")
                ?: "Aguascalientes"

        codigoPostal =
            intent.getStringExtra("codigoPostal")
                ?: "20000"

        calle =
            intent.getStringExtra("calle")
                ?: "Av. Universidad"

        numero =
            intent.getStringExtra("numero")
                ?: "123"

        fraccionamiento =
            intent.getStringExtra("fraccionamiento")
                ?: "Centro"

        latitud =
            intent.getStringExtra("latitud")
                ?: "21.8818"

        longitud =
            intent.getStringExtra("longitud")
                ?: "-102.2916"
    }

    private fun inicializarVistas() {

        tvNombre =
            findViewById(R.id.tvNombrePublicacion)

        tvCapacidad =
            findViewById(R.id.tvCapacidadPublicacion)

        tvPrecio =
            findViewById(R.id.tvPrecioPublicacion)

        tvDescripcion =
            findViewById(R.id.tvDescripcionPublicacion)

        tvDireccion =
            findViewById(R.id.tvDireccionPublicacion)
    }

    private fun mostrarDatos() {

        tvNombre.text = nombreSalon

        tvCapacidad.text =
            "$capacidadPersonas personas"

        tvPrecio.text =
            "$$precioHora / hora"

        tvDescripcion.text =
            descripcion

        tvDireccion.text =
            "$calle $numero\n" +
                    "$fraccionamiento\n" +
                    "$ciudad, CP $codigoPostal"
    }

    private fun configurarBotones() {

        findViewById<TextView>(
            R.id.btnBackPublicacion
        ).setOnClickListener {
            finish()
        }


        // EDITAR NOMBRE

        findViewById<TextView>(
            R.id.btnEditarNombre
        ).setOnClickListener {

            abrirEdicion()
        }


        // EDITAR CAPACIDAD

        findViewById<TextView>(
            R.id.btnEditarCapacidad
        ).setOnClickListener {

            abrirEdicion()
        }


        // EDITAR PRECIO

        findViewById<TextView>(
            R.id.btnEditarPrecio
        ).setOnClickListener {

            abrirEdicion()
        }


        // EDITAR DESCRIPCIÓN

        findViewById<TextView>(
            R.id.btnEditarDescripcion
        ).setOnClickListener {

            abrirEdicion()
        }


        // EDITAR UBICACIÓN

        findViewById<TextView>(
            R.id.btnEditarUbicacion
        ).setOnClickListener {

            val intent = Intent(
                this,
                SalonUbicacionActivity::class.java
            )

            intent.putExtra("nombreSalon", nombreSalon)
            intent.putExtra(
                "capacidadPersonas",
                capacidadPersonas
            )
            intent.putExtra(
                "descripcion",
                descripcion
            )
            intent.putExtra(
                "precioHora",
                precioHora
            )

            startActivity(intent)
        }


        // DISPONIBILIDAD

        findViewById<MaterialButton>(
            R.id.btnDisponibilidad
        ).setOnClickListener {

            val intent = Intent(
                this,
                SalonDisponibilidadActivity::class.java
            )

            startActivity(intent)
        }


        // IMÁGENES

        findViewById<MaterialButton>(
            R.id.btnEditarImagenes
        ).setOnClickListener {

            Toast.makeText(
                this,
                "Aquí se abrirá el selector de imágenes",
                Toast.LENGTH_SHORT
            ).show()
        }


        // PUBLICAR

        findViewById<MaterialButton>(
            R.id.btnPublicarSalon
        ).setOnClickListener {

            Toast.makeText(
                this,
                "El salón está listo para publicar",
                Toast.LENGTH_SHORT
            ).show()

            /*
             * IMPORTANTE:
             * Aquí NO hacemos ningún POST.

             * Conectar esta acción con:
             *
             * POST /api/v1/salones
             */
        }
    }

    private fun abrirEdicion() {

        val intent = Intent(
            this,
            SalonFormActivity::class.java)

        intent.putExtra("modoEdicion", true)

        intent.putExtra("nombreSalon", nombreSalon)

        intent.putExtra("capacidadPersonas", capacidadPersonas)

        intent.putExtra("descripcion", descripcion)

        intent.putExtra("precioHora", precioHora)

        startActivity(intent)
    }
}