
package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.widget.EditText
import android.widget.TextView
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
    private lateinit var mapaWebView: WebView
    private var ubicacionSeleccionada = false

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

        configurarMapa()

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

        etLatitud.isFocusable = false
        etLatitud.isClickable = false
        etLongitud.isFocusable = false
        etLongitud.isClickable = false


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

        mapaWebView = findViewById(R.id.webViewMapaSalon)
    }


    private fun configurarBotones() {

        // REGRESAR

        findViewById<TextView>(
            R.id.btnBackUbicacion
        ).setOnClickListener {

            finish()
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


    @SuppressLint("SetJavaScriptEnabled")
    private fun configurarMapa() {
        mapaWebView.settings.javaScriptEnabled = true
        mapaWebView.settings.domStorageEnabled = true
        mapaWebView.settings.userAgentString = "${mapaWebView.settings.userAgentString} OcasionMobil/1.0"
        mapaWebView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onLocationSelected(latitude: Double, longitude: Double) {
                runOnUiThread {
                    ubicacionSeleccionada = true
                    etLatitud.setText(latitude.toString())
                    etLongitud.setText(longitude.toString())
                    tvMapaPlaceholder.visibility = View.GONE
                }
            }
        }, "AndroidBridge")
        mapaWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                tvMapaPlaceholder.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    tvMapaPlaceholder.text = "No se pudo abrir el mapa. Verifica tu conexión a internet."
                    tvMapaPlaceholder.visibility = View.VISIBLE
                }
            }
        }
        mapaWebView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE ->
                    view.parent.requestDisallowInterceptTouchEvent(true)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
        mapaWebView.loadUrl("file:///android_asset/mapa_ubicacion.html")
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

        if (!ubicacionSeleccionada || latitud == null || latitud !in -90.0..90.0) {

            mostrarError(
                tvErrorLatitud,
                "Selecciona una ubicación válida en el mapa."
            )

            valido = false
        }


        // LONGITUD

        val longitud =
            etLongitud.text
                .toString()
                .trim()
                .toDoubleOrNull()

        if (!ubicacionSeleccionada || longitud == null || longitud !in -180.0..180.0) {

            mostrarError(
                tvErrorLongitud,
                "Selecciona una ubicación válida en el mapa."
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

        val intent =
            Intent(
                this,
                SalonDisponibilidadActivity::class.java
            )


        intent.putExtra(
            "nombreSalon",
            nombreSalon
        )

        intent.putExtra(
            "capacidadPersonas",
            capacidadPersonas
        )

        intent.putExtra(
            "precioHora",
            precioHora
        )

        intent.putExtra(
            "descripcion",
            descripcion
        )


        intent.putStringArrayListExtra(
            "imagenes",
            imagenes
        )


        intent.putExtra(
            "ciudad",
            etCiudad.text.toString().trim()
        )

        intent.putExtra(
            "codigoPostal",
            etCodigoPostal.text.toString().trim()
        )

        intent.putExtra(
            "calle",
            etCalle.text.toString().trim()
        )

        intent.putExtra(
            "numero",
            etNumero.text.toString().trim()
        )

        intent.putExtra(
            "fraccionamiento",
            etFraccionamiento.text.toString().trim()
        )

        intent.putExtra(
            "latitud",
            etLatitud.text.toString().trim()
        )

        intent.putExtra(
            "longitud",
            etLongitud.text.toString().trim()
        )


        startActivity(intent)
    }

    override fun onDestroy() {
        if (::mapaWebView.isInitialized) {
            mapaWebView.removeJavascriptInterface("AndroidBridge")
            mapaWebView.stopLoading()
            mapaWebView.destroy()
        }
        super.onDestroy()
    }
}
