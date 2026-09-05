package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Salon
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MisSalonesActivity : ComponentActivity() {

    private lateinit var containerSalones: LinearLayout
    private lateinit var layoutEmptySalones: LinearLayout

    private lateinit var tvSalonNumber: TextView

    private val salones =
        mutableListOf<Salon>()


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_salones
        )

        inicializarVistas()

        configurarNavegacion()

        cargarSalonesPrueba()

        mostrarSalones()

        BottomNav.configurar(this)
    }


    // =====================================================
    // VISTAS
    // =====================================================

    private fun inicializarVistas() {

        containerSalones =
            findViewById(
                R.id.containerSalones
            )

        layoutEmptySalones =
            findViewById(
                R.id.layoutEmptySalones
            )

        tvSalonNumber =
            findViewById(
                R.id.tvSalonNumber
            )
    }


    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private fun configurarNavegacion() {

        val abrirFormulario = {

            val intent = Intent(
                this,
                SalonFormActivity::class.java
            )

            startActivity(intent)
        }


        findViewById<LinearLayout>(
            R.id.btnNuevoSalon
        ).setOnClickListener {

            abrirFormulario()
        }


        findViewById<FloatingActionButton>(
            R.id.fabAddSalon
        ).setOnClickListener {

            abrirFormulario()
        }
    }


    // =====================================================
    // DATOS DE PRUEBA
    // =====================================================

    private fun cargarSalonesPrueba() {

        /*
         * TEMPORAL
         *
         * Esto solamente sirve para comprobar el front.
         *
         * Después será reemplazado por:
         *
         * GET /api/v1/salones
         *
         * usando Retrofit.
         */

        salones.clear()

        salones.add(
            Salon(
                id = "1",
                nombreSalon = "Salón Jardín",
                capacidadPersonas = 150,
                descripcion =
                    "Espacio ideal para celebraciones, reuniones y todo tipo de eventos.",
                precioHora = 850.0,

                ciudad = "Aguascalientes",
                codigoPostal = "20000",
                calle = "Av. Universidad",
                numero = "123",
                fraccionamiento = "Centro",

                latitud = 21.8818,
                longitud = -102.2916,

                imagenes = emptyList()
            )
        )

        salones.add(
            Salon(
                id = "2",
                nombreSalon = "Salón Los Arcos",
                capacidadPersonas = 100,
                descripcion =
                    "Salón amplio y cómodo para eventos familiares y reuniones.",
                precioHora = 700.0,

                ciudad = "Aguascalientes",
                codigoPostal = "20100",
                calle = "Av. Convención",
                numero = "456",
                fraccionamiento = "San Marcos",

                latitud = 21.8850,
                longitud = -102.3000,

                imagenes = emptyList()
            )
        )
    }


    // =====================================================
    // MOSTRAR SALONES
    // =====================================================

    private fun mostrarSalones() {

        containerSalones.removeAllViews()


        // CONTADOR

        tvSalonNumber.text =
            salones.size.toString()


        // SIN SALONES

        if (salones.isEmpty()) {

            layoutEmptySalones.visibility =
                LinearLayout.VISIBLE

            return

        } else {

            layoutEmptySalones.visibility =
                LinearLayout.GONE
        }


        // CREAR CADA TARJETA

        salones.forEach { salon ->

            val itemView =
                LayoutInflater.from(this)
                    .inflate(
                        R.layout.item_salon,
                        containerSalones,
                        false
                    )


            configurarItemSalon(
                itemView,
                salon
            )


            containerSalones.addView(
                itemView
            )
        }
    }


    // =====================================================
    // CONFIGURAR ITEM
    // =====================================================

    private fun configurarItemSalon(
        view: View,
        salon: Salon
    ) {

        val imgSalon =
            view.findViewById<ImageView>(
                R.id.imgSalon
            )

        val tvSalonName =
            view.findViewById<TextView>(
                R.id.tvSalonName
            )

        val tvSalonLocation =
            view.findViewById<TextView>(
                R.id.tvSalonLocation
            )

        val tvCapacity =
            view.findViewById<TextView>(
                R.id.tvCapacity
            )

        val tvPrice =
            view.findViewById<TextView>(
                R.id.tvPrice
            )

        val tvPriceBadge =
            view.findViewById<TextView>(
                R.id.tvPriceBadge
            )

        val tvDescription =
            view.findViewById<TextView>(
                R.id.tvDescription
            )


        // INFORMACIÓN

        tvSalonName.text =
            salon.nombreSalon

        tvSalonLocation.text =
            "📍 ${salon.ciudad}"

        tvCapacity.text =
            "👥 ${salon.capacidadPersonas} personas"

        tvPrice.text =
            "$${salon.precioHora} / hora"

        tvPriceBadge.text =
            "$${salon.precioHora} / hora"

        tvDescription.text =
            salon.descripcion


        // IMAGEN

        if (salon.imagenes.isEmpty()) {

            imgSalon.setImageResource(
                R.drawable.ic_launcher_background
            )
        }


        // EDITAR

        view.findViewById<
                MaterialButton
                >(R.id.btnEditSalon)
            .setOnClickListener {

                abrirEdicion(salon)
            }


        // DAR DE BAJA

        view.findViewById<
                MaterialButton
                >(R.id.btnDeleteSalon)
            .setOnClickListener {

                confirmarBaja(salon)
            }


        // DISPONIBILIDAD

        view.findViewById<
                MaterialButton
                >(R.id.btnDisponibilidadSalon)
            .setOnClickListener {

                abrirDisponibilidad(salon)
            }
    }


    // =====================================================
    // EDITAR
    // =====================================================

    private fun abrirEdicion(
        salon: Salon
    ) {

        val intent =
            Intent(
                this,
                SalonFormActivity::class.java
            )

        intent.putExtra(
            "modoEdicion",
            true
        )

        intent.putExtra(
            "idSalon",
            salon.id
        )

        intent.putExtra(
            "nombreSalon",
            salon.nombreSalon
        )

        intent.putExtra(
            "capacidadPersonas",
            salon.capacidadPersonas.toString()
        )

        intent.putExtra(
            "precioHora",
            salon.precioHora.toString()
        )

        intent.putExtra(
            "descripcion",
            salon.descripcion
        )

        intent.putExtra(
            "ciudad",
            salon.ciudad
        )

        intent.putExtra(
            "codigoPostal",
            salon.codigoPostal
        )

        intent.putExtra(
            "calle",
            salon.calle
        )

        intent.putExtra(
            "numero",
            salon.numero
        )

        intent.putExtra(
            "fraccionamiento",
            salon.fraccionamiento
        )

        intent.putExtra(
            "latitud",
            salon.latitud.toString()
        )

        intent.putExtra(
            "longitud",
            salon.longitud.toString()
        )

        intent.putStringArrayListExtra(
            "imagenes",
            ArrayList(
                salon.imagenes
            )
        )

        startActivity(intent)
    }


    // =====================================================
    // DISPONIBILIDAD
    // =====================================================

    private fun abrirDisponibilidad(
        salon: Salon
    ) {

        val intent =
            Intent(
                this,
                SalonDisponibilidadActivity::class.java
            )

        intent.putExtra(
            "idSalon",
            salon.id
        )

        intent.putExtra(
            "nombreSalon",
            salon.nombreSalon
        )

        startActivity(intent)
    }


    // =====================================================
    // DAR DE BAJA
    // =====================================================

    private fun confirmarBaja(
        salon: Salon
    ) {

        AlertDialog.Builder(this)
            .setTitle("Dar de baja salón")
            .setMessage(
                "¿Seguro que deseas dar de baja \"${salon.nombreSalon}\"?"
            )
            .setNegativeButton(
                "Cancelar",
                null
            )
            .setPositiveButton(
                "Dar de baja"
            ) { _, _ ->

                /*
                 * TEMPORAL
                 *
                 * Después aquí irá:
                 *
                 * DELETE /api/v1/salones/{id}
                 */

                salones.remove(
                    salon
                )

                mostrarSalones()

                Toast.makeText(
                    this,
                    "Salón dado de baja.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .show()
    }


    // =====================================================
    // ACTUALIZAR AL REGRESAR
    // =====================================================

    override fun onResume() {
        super.onResume()

        if (
            ::containerSalones.isInitialized
        ) {

            mostrarSalones()
        }
    }
}