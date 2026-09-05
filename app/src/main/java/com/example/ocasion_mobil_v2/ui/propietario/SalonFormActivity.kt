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


            imageView.setImageURI(
                uri
            )


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