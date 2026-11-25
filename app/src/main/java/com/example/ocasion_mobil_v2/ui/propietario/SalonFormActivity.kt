package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionRequest
import com.google.android.material.button.MaterialButton


class SalonFormActivity : ComponentActivity() {
    private val viewModel: SalonFormViewModel by viewModels()

    // CAMPOS DEL FORMULARIO

    private lateinit var etNombreSalon: EditText
    private lateinit var etCapacidad: EditText
    private lateinit var etPrecioHora: EditText
    private lateinit var etDescripcion: EditText


    // MENSAJES DE ERROR

    private lateinit var tvErrorNombre: TextView
    private lateinit var tvErrorCapacidad: TextView
    private lateinit var tvErrorPrecio: TextView
    private lateinit var tvErrorDescripcion: TextView
    private lateinit var tvErrorImagenes: TextView



    // IMÁGENES

    private lateinit var containerImages: LinearLayout
    private lateinit var tvCantidadImagenes: TextView
    private lateinit var btnAddImages: MaterialButton


    // LISTA DE IMÁGENES SELECCIONADAS

    private val imagenesSeleccionadas =
        mutableListOf<Uri>()


    // PHOTO PICKER

    private val photoPicker =
        registerForActivityResult(
            ActivityResultContracts.PickMultipleVisualMedia(
                5
            )
        ) { uris ->

            procesarImagenesSeleccionadas(
                uris
            )
        }


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_salon_form
        )

        inicializarVistas()

        configurarBotones()

        configurarValidaciones()

        observarEstado()

        cargarDatosEdicion()

        actualizarContadorImagenes()
        if (intent.hasExtra("idSalon")) {
            val id = intent.getStringExtra("idSalon")?.toIntOrNull()
            if (id == null || id <= 0) {
                Toast.makeText(this, "Vuelve a seleccionar el salón.", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            configurarEdicionSalon(id)
        }
    }

    private fun configurarEdicionSalon(id: Int) {
        findViewById<TextView>(R.id.tvFormTitle).text = "Editar salón"
        findViewById<TextView>(R.id.tvFormSubtitle).text = "Puedes cambiar la galería desde Mis salones → Actualizar fotos."
        findViewById<View>(R.id.seccionFotosAlta).visibility = View.GONE
        findViewById<View>(R.id.ubicacionEdicion).visibility = View.VISIBLE
        fun campo(vista: Int, extra: String) = findViewById<EditText>(vista).apply {
            setText(intent.getStringExtra(extra))
        }
        val direccion = campo(R.id.direccionEdicion, "calle")
        val ciudad = campo(R.id.ciudadEdicion, "ciudad")
        val cp = campo(R.id.cpEdicion, "codigoPostal")
        val latitud = campo(R.id.latitudEdicion, "latitud")
        val longitud = campo(R.id.longitudEdicion, "longitud")
        val guardar = findViewById<MaterialButton>(R.id.btnContinuarUbicacion)
        guardar.text = "Guardar cambios"
        guardar.setOnClickListener {
            viewModel.validarFormularioCompleto()
            val datos = viewModel.uiState.value
            if (listOf(datos.errorNombre, datos.errorCapacidad, datos.errorPrecio, datos.errorDescripcion).any { it != null })
                return@setOnClickListener
            val vacio = listOf(direccion, ciudad, cp).firstOrNull { it.text.isBlank() }
            if (vacio != null) {
                vacio.error = "Completa este campo"
                vacio.requestFocus()
                return@setOnClickListener
            }
            val lat = latitud.text.toString().toDoubleOrNull()
            val lon = longitud.text.toString().toDoubleOrNull()
            if (lat == null || lat !in -90.0..90.0) {
                latitud.error = "Ingresa una latitud entre −90 y 90"
                return@setOnClickListener
            }
            if (lon == null || lon !in -180.0..180.0) {
                longitud.error = "Ingresa una longitud entre −180 y 180"
                return@setOnClickListener
            }
            val solicitud = SalonCreateFase1Request(
                nombreSalon = datos.nombreSalon.trim(), capacidadPersonas = datos.capacidadPersonas.trim().toInt(),
                precioHora = datos.precioHora.trim().toDouble(), descripcion = datos.descripcion.trim(),
                ubicacion = UbicacionRequest(direccion.text.toString().trim(), lat, lon,
                    ciudad.text.toString().trim(), cp.text.toString().trim())
            )
            guardar.isEnabled = false
            guardar.text = "Guardando…"
            lifecycleScope.launch {
                try {
                    val respuesta = RetrofitClientSalones.getService(this@SalonFormActivity).actualizarSalon(id, solicitud)
                    if (respuesta.isSuccessful) {
                        Toast.makeText(this@SalonFormActivity, "Salón actualizado", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        respuesta.errorBody()?.close()
                        Toast.makeText(this@SalonFormActivity, "D2 no aceptó los cambios (HTTP ${respuesta.code()}).", Toast.LENGTH_LONG).show()
                    }
                } catch (e: java.io.IOException) {
                    Toast.makeText(this@SalonFormActivity, "No se pudo confirmar el guardado. Revisa tu conexión.", Toast.LENGTH_LONG).show()
                } finally {
                    guardar.isEnabled = true
                    guardar.text = "Guardar cambios"
                }
            }
        }
    }


    private fun inicializarVistas() {


        // FORMULARIO

        etNombreSalon =
            findViewById(
                R.id.etNombreSalon
            )

        etCapacidad =
            findViewById(
                R.id.etCapacidad
            )

        etPrecioHora =
            findViewById(
                R.id.etPrecioHora
            )

        etDescripcion =
            findViewById(
                R.id.etDescripcion
            )


        // ERRORES

        tvErrorNombre =
            findViewById(
                R.id.tvErrorNombre
            )

        tvErrorCapacidad =
            findViewById(
                R.id.tvErrorCapacidad
            )

        tvErrorPrecio =
            findViewById(
                R.id.tvErrorPrecio
            )

        tvErrorDescripcion =
            findViewById(
                R.id.tvErrorDescripcion
            )

        tvErrorImagenes =
            findViewById(
                R.id.tvErrorImagenes
            )


        // IMÁGENES

        containerImages =
            findViewById(
                R.id.containerImages
            )

        tvCantidadImagenes =
            findViewById(
                R.id.tvCantidadImagenes
            )

        btnAddImages =
            findViewById(
                R.id.btnAddImages
            )
    }


    private fun configurarBotones() {


        // REGRESAR

        findViewById<TextView>(
            R.id.btnBack
        ).setOnClickListener {

            finish()
        }


        // AGREGAR IMÁGENES

        btnAddImages.setOnClickListener {

            abrirSelectorImagenes()
        }


        // CONTINUAR A UBICACIÓN

        findViewById<MaterialButton>(
            R.id.btnContinuarUbicacion
        ).setOnClickListener {

            if (
                validarFormulario()
            ) {

                abrirUbicacion()
            }
        }
    }


    // ABRIR PHOTO PICKER

    private fun abrirSelectorImagenes() {

        if (
            imagenesSeleccionadas.size >= 5
        ) {

            Toast.makeText(
                this,
                "Solo puedes seleccionar un máximo de 5 imágenes.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }


        photoPicker.launch(
            PickVisualMediaRequest(
                ActivityResultContracts
                    .PickVisualMedia
                    .ImageOnly
            )
        )
    }


    // PROCESAR IMÁGENES

    private fun procesarImagenesSeleccionadas(
        nuevasImagenes: List<Uri>
    ) {

        val espaciosDisponibles =
            5 - imagenesSeleccionadas.size


        val imagenesParaAgregar =
            nuevasImagenes.take(
                espaciosDisponibles
            )


        imagenesSeleccionadas.addAll(
            imagenesParaAgregar
        )


        if (
            nuevasImagenes.size >
            espaciosDisponibles
        ) {

            Toast.makeText(
                this,
                "Solo se agregaron las imágenes permitidas hasta completar 5.",
                Toast.LENGTH_SHORT
            ).show()
        }


        mostrarImagenes()

        actualizarContadorImagenes()


        tvErrorImagenes.visibility =
            View.GONE
    }


    // MOSTRAR IMÁGENES

    private fun mostrarImagenes() {

        containerImages.removeAllViews()


        imagenesSeleccionadas.forEach { uri ->


            val imageView =
                ImageView(this)


            val parametros =
                LinearLayout.LayoutParams(
                    100,
                    100
                )


            parametros.marginEnd =
                10


            imageView.layoutParams =
                parametros


            com.example.ocasion_mobil_v2.util.ImagenSalonLoader.cargar(imageView, uri.toString())


            imageView.scaleType =
                ImageView.ScaleType.CENTER_CROP


            imageView.contentDescription =
                "Imagen seleccionada"


            // ELIMINAR CON TOQUE LARGO

            imageView.setOnLongClickListener {

                imagenesSeleccionadas.remove(
                    uri
                )


                mostrarImagenes()

                actualizarContadorImagenes()


                Toast.makeText(
                    this,
                    "Imagen eliminada.",
                    Toast.LENGTH_SHORT
                ).show()


                true
            }


            containerImages.addView(
                imageView
            )
        }
    }


    // CONTADOR
    private fun actualizarContadorImagenes() {

        tvCantidadImagenes.text =
            "${imagenesSeleccionadas.size} de 5 imágenes seleccionadas"


        if (
            imagenesSeleccionadas.size >= 5
        ) {

            btnAddImages.isEnabled =
                false

            btnAddImages.text =
                "Máximo de imágenes alcanzado"

        } else {

            btnAddImages.isEnabled =
                true

            btnAddImages.text =
                "📷   Agregar imágenes"
        }
        viewModel.actualizarCantidadImagenes(
            imagenesSeleccionadas.size
        )
    }

    // VALIDAR FORMULARIO

    private fun validarFormulario(): Boolean {

        viewModel.validarFormularioCompleto()

        return viewModel.uiState.value.formularioValido
    }

    // LIMPIAR ERRORES

    private fun limpiarErrores() {

        tvErrorNombre.visibility =
            View.GONE


        tvErrorCapacidad.visibility =
            View.GONE


        tvErrorPrecio.visibility =
            View.GONE


        tvErrorDescripcion.visibility =
            View.GONE


        tvErrorImagenes.visibility =
            View.GONE
    }

    // ABRIR UBICACIÓN

    private fun abrirUbicacion() {

        val ubicacionIntent =
            Intent(
                this,
                SalonUbicacionActivity::class.java
            )

        // DATOS DEL SALÓN

        ubicacionIntent.putExtra(
            "nombreSalon",
            etNombreSalon.text
                .toString()
        )


        ubicacionIntent.putExtra(
            "capacidadPersonas",
            etCapacidad.text
                .toString()
        )


        ubicacionIntent.putExtra(
            "precioHora",
            etPrecioHora.text
                .toString()
        )


        ubicacionIntent.putExtra(
            "descripcion",
            etDescripcion.text
                .toString()
        )

        // IMÁGENES

        val imagenesString =
            ArrayList<String>()


        imagenesSeleccionadas.forEach {

            imagenesString.add(
                it.toString()
            )
        }


        ubicacionIntent.putStringArrayListExtra(
            "imagenes",
            imagenesString
        )


        startActivity(
            ubicacionIntent
        )
    }


    // MODO EDICIÓN

    private fun cargarDatosEdicion() {

        val modoEdicion =
            intent.getBooleanExtra(
                "modoEdicion",
                false
            )


        if (
            !modoEdicion
        ) {

            return
        }


        etNombreSalon.setText(
            intent.getStringExtra(
                "nombreSalon"
            )
        )


        etCapacidad.setText(
            intent.getStringExtra(
                "capacidadPersonas"
            )
        )


        etPrecioHora.setText(
            intent.getStringExtra(
                "precioHora"
            )
        )


        etDescripcion.setText(
            intent.getStringExtra(
                "descripcion"
            )
        )
    }

    private fun configurarValidaciones() {

        etNombreSalon.doAfterTextChanged {

            viewModel.actualizarNombre(
                it.toString()
            )
        }


        etCapacidad.doAfterTextChanged {

            viewModel.actualizarCapacidad(
                it.toString()
            )
        }


        etPrecioHora.doAfterTextChanged {

            viewModel.actualizarPrecio(
                it.toString()
            )
        }


        etDescripcion.doAfterTextChanged {

            viewModel.actualizarDescripcion(
                it.toString()
            )
        }
    }

    private fun observarEstado() {

        lifecycleScope.launch {

            viewModel.uiState.collect { estado ->

                // NOMBRE

                if (estado.errorNombre != null) {

                    tvErrorNombre.text =
                        estado.errorNombre

                    tvErrorNombre.visibility =
                        View.VISIBLE

                } else {

                    tvErrorNombre.visibility =
                        View.GONE
                }

                // CAPACIDAD

                if (estado.errorCapacidad != null) {

                    tvErrorCapacidad.text =
                        estado.errorCapacidad

                    tvErrorCapacidad.visibility =
                        View.VISIBLE

                } else {

                    tvErrorCapacidad.visibility =
                        View.GONE
                }

                // PRECIO

                if (estado.errorPrecio != null) {

                    tvErrorPrecio.text =
                        estado.errorPrecio

                    tvErrorPrecio.visibility =
                        View.VISIBLE

                } else {

                    tvErrorPrecio.visibility =
                        View.GONE
                }

                // DESCRIPCIÓN

                if (estado.errorDescripcion != null) {

                    tvErrorDescripcion.text =
                        estado.errorDescripcion

                    tvErrorDescripcion.visibility =
                        View.VISIBLE

                } else {

                    tvErrorDescripcion.visibility =
                        View.GONE
                }

                // IMÁGENES

                if (estado.errorImagenes != null) {

                    tvErrorImagenes.text =
                        estado.errorImagenes

                    tvErrorImagenes.visibility =
                        View.VISIBLE

                } else {

                    tvErrorImagenes.visibility =
                        View.GONE
                }
            }
        }
    }
}
