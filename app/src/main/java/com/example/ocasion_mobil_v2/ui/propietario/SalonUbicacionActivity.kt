package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import com.google.android.material.button.MaterialButton

private var imagenes =
    arrayListOf<String>()

class SalonUbicacionActivity : ComponentActivity() {

    // CAMPOS

    private lateinit var etCiudad: EditText
    private lateinit var etCodigoPostal: EditText
    private lateinit var etCalle: EditText
    private lateinit var etNumero: EditText
    private lateinit var etFraccionamiento: EditText
    private lateinit var etLatitud: EditText
    private lateinit var etLongitud: EditText


    // ERRORES

    private lateinit var tvErrorCiudad: TextView
    private lateinit var tvErrorCodigoPostal: TextView
    private lateinit var tvErrorCalle: TextView
    private lateinit var tvErrorNumero: TextView
    private lateinit var tvErrorFraccionamiento: TextView
    private lateinit var tvErrorLatitud: TextView
    private lateinit var tvErrorLongitud: TextView

    // MAPA

    private lateinit var tvMapaPlaceholder: TextView

    // DATOS RECIBIDOS DEL FORMULARIO DEL SALÓN

    private var nombreSalon = ""
    private var capacidadPersonas = ""
    private var precioHora = ""
    private var descripcion = ""


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_salon_ubicacion
        )

        recibirDatosSalon()

        inicializarVistas()

        configurarBotones()
    }


    private fun recibirDatosSalon() {

        nombreSalon =
            intent.getStringExtra(
                "nombreSalon"
            ) ?: ""

        capacidadPersonas =
            intent.getStringExtra(
                "capacidadPersonas"
            ) ?: ""

        precioHora =
            intent.getStringExtra(
                "precioHora"
            ) ?: ""

        descripcion =
            intent.getStringExtra(
                "descripcion"
            ) ?: ""

        // IMÁGENES

        imagenes =
            intent.getStringArrayListExtra(
                "imagenes"
            ) ?: arrayListOf()
    }


    private fun inicializarVistas() {

        // CAMPOS

        etCiudad =
            findViewById(R.id.etCiudad)

        etCodigoPostal =
            findViewById(R.id.etCodigoPostal)

        etCalle =
            findViewById(R.id.etCalle)

        etNumero =
            findViewById(R.id.etNumero)

        etFraccionamiento =
            findViewById(R.id.etFraccionamiento)

        etLatitud =
            findViewById(R.id.etLatitud)

        etLongitud =
            findViewById(R.id.etLongitud)


        // MENSAJES DE ERROR

        tvErrorCiudad =
            findViewById(R.id.tvErrorCiudad)

        tvErrorCodigoPostal =
            findViewById(R.id.tvErrorCodigoPostal)

        tvErrorCalle =
            findViewById(R.id.tvErrorCalle)

        tvErrorNumero =
            findViewById(R.id.tvErrorNumero)

        tvErrorFraccionamiento =
            findViewById(
                R.id.tvErrorFraccionamiento
            )

        tvErrorLatitud =
            findViewById(R.id.tvErrorLatitud)

        tvErrorLongitud =
            findViewById(R.id.tvErrorLongitud)


        // MAPA

        tvMapaPlaceholder =
            findViewById(
                R.id.tvMapaPlaceholder
            )
    }


    private fun configurarBotones() {

        // REGRESAR

        findViewById<TextView>(
            R.id.btnBackUbicacion
        ).setOnClickListener {

            finish()
        }

        // MOSTRAR UBICACIÓN

        findViewById<MaterialButton>(
            R.id.btnMostrarUbicacion
        ).setOnClickListener {

            mostrarUbicacion()
        }

        // CONTINUAR

        findViewById<MaterialButton>(
            R.id.btnContinuarPublicacion
        ).setOnClickListener {

            if (validarFormulario()) {

                abrirPublicacion()
            }
        }
    }


    private fun mostrarUbicacion() {

        val latitud =
            etLatitud.text
                .toString()
                .trim()
                .toDoubleOrNull()

        val longitud =
            etLongitud.text
                .toString()
                .trim()
                .toDoubleOrNull()


        if (
            latitud == null ||
            longitud == null
        ) {

            tvMapaPlaceholder.text =
                "Ingresa coordenadas válidas."

            return
        }


        tvMapaPlaceholder.text =
            "Ubicación seleccionada:\n\n" +
                    "Latitud: $latitud\n" +
                    "Longitud: $longitud"


        // MÁS ADELANTE:
        // AQUÍ SE COLOCARÁ GOOGLE MAPS
    }


    private fun validarFormulario(): Boolean {

        var valido = true

        limpiarErrores()


        // CIUDAD

        if (
            etCiudad.text
                .toString()
                .trim()
                .isEmpty()
        ) {

            mostrarError(
                tvErrorCiudad,
                "Ingresa la ciudad."
            )

            valido = false
        }


        // CÓDIGO POSTAL

        val codigoPostal =
            etCodigoPostal.text
                .toString()
                .trim()

        if (
            codigoPostal.length != 5
        ) {

            mostrarError(
                tvErrorCodigoPostal,
                "El código postal debe tener 5 dígitos."
            )

            valido = false
        }


        // CALLE

        if (
            etCalle.text
                .toString()
                .trim()
                .isEmpty()
        ) {

            mostrarError(
                tvErrorCalle,
                "Ingresa la calle."
            )

            valido = false
        }


        // NÚMERO

        if (
            etNumero.text
                .toString()
                .trim()
                .isEmpty()
        ) {

            mostrarError(
                tvErrorNumero,
                "Ingresa el número."
            )

            valido = false
        }


        // FRACCIONAMIENTO

        if (
            etFraccionamiento.text
                .toString()
                .trim()
                .isEmpty()
        ) {

            mostrarError(
                tvErrorFraccionamiento,
                "Ingresa el fraccionamiento."
            )

            valido = false
        }


        // LATITUD

        val latitud =
            etLatitud.text
                .toString()
                .trim()
                .toDoubleOrNull()

        if (latitud == null) {

            mostrarError(
                tvErrorLatitud,
                "Ingresa una latitud válida."
            )

            valido = false
        }


        // LONGITUD

        val longitud =
            etLongitud.text
                .toString()
                .trim()
                .toDoubleOrNull()

        if (longitud == null) {

            mostrarError(
                tvErrorLongitud,
                "Ingresa una longitud válida."
            )

            valido = false
        }


        return valido
    }


    private fun mostrarError(
        textView: TextView,
        mensaje: String
    ) {

        textView.text =
            mensaje

        textView.visibility =
            View.VISIBLE
    }


    private fun limpiarErrores() {

        tvErrorCiudad.visibility =
            View.GONE

        tvErrorCodigoPostal.visibility =
            View.GONE

        tvErrorCalle.visibility =
            View.GONE

        tvErrorNumero.visibility =
            View.GONE

        tvErrorFraccionamiento.visibility =
            View.GONE

        tvErrorLatitud.visibility =
            View.GONE

        tvErrorLongitud.visibility =
            View.GONE
    }


    private fun abrirPublicacion() {

        val publicacionIntent = Intent(
            this,
            SalonPublicacionActivity::class.java
        )


        // DATOS DEL SALÓN

        publicacionIntent.putExtra("nombreSalon", nombreSalon)

        publicacionIntent.putExtra("capacidadPersonas", capacidadPersonas)

        publicacionIntent.putExtra("precioHora", precioHora)

        publicacionIntent.putExtra("descripcion", descripcion)

        // IMÁGENES

        publicacionIntent.putStringArrayListExtra(
            "imagenes",
            imagenes
        )

        // DATOS DE UBICACIÓN

        publicacionIntent.putExtra("ciudad", etCiudad.text.toString())

        publicacionIntent.putExtra("codigoPostal", etCodigoPostal.text.toString())

        publicacionIntent.putExtra("calle", etCalle.text.toString())

        publicacionIntent.putExtra("numero", etNumero.text.toString())

        publicacionIntent.putExtra("fraccionamiento", etFraccionamiento.text.toString())

        publicacionIntent.putExtra("latitud", etLatitud.text.toString())

        publicacionIntent.putExtra("longitud", etLongitud.text.toString())


        startActivity(
            publicacionIntent
        )
    }
}