package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ConfirmacionSalonActivity :
    ComponentActivity() {

    private var idSalon = -1

    private var publicando = false


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        setContentView(
            R.layout.activity_confirmacion_salon
        )


        idSalon =
            intent.getIntExtra(
                "idSalon",
                -1
            )


        if (idSalon <= 0) {

            Toast.makeText(
                this,
                "No se encontró el salón.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        mostrarDatos()


        findViewById<TextView>(
            R.id.btnBackConfirmacion
        ).setOnClickListener {

            finish()
        }


        findViewById<MaterialButton>(
            R.id.btnPublicarConfirmacion
        ).setOnClickListener {

            continuarDocumentos()
        }
    }


    private fun mostrarDatos() {

        findViewById<TextView>(
            R.id.tvNombreConfirmacion
        ).text =
            intent.getStringExtra(
                "nombreSalon"
            ).orEmpty()
                .ifBlank {
                    "Salón"
                }


        findViewById<TextView>(
            R.id.tvCapacidadConfirmacion
        ).text =
            "${
                intent.getStringExtra(
                    "capacidadPersonas"
                ).orEmpty()
            } personas"


        findViewById<TextView>(
            R.id.tvPrecioConfirmacion
        ).text =
            "$${
                intent.getStringExtra(
                    "precioHora"
                ).orEmpty()
            } / hora"


        findViewById<TextView>(
            R.id.tvDescripcionConfirmacion
        ).text =
            intent.getStringExtra(
                "descripcion"
            ).orEmpty()
                .ifBlank {
                    "Sin descripción."
                }


        findViewById<TextView>(
            R.id.tvDireccionConfirmacion
        ).text =

            "${
                intent.getStringExtra(
                    "calle"
                ).orEmpty()
            } ${
                intent.getStringExtra(
                    "numero"
                ).orEmpty()
            }\n" +

                    "${
                        intent.getStringExtra(
                            "fraccionamiento"
                        ).orEmpty()
                    }\n" +

                    "${
                        intent.getStringExtra(
                            "ciudad"
                        ).orEmpty()
                    }, CP ${
                        intent.getStringExtra(
                            "codigoPostal"
                        ).orEmpty()
                    }"


        findViewById<TextView>(
            R.id.tvResumenCatalogo
        ).text =

            "${
                intent.getIntExtra(
                    "servicios",
                    0
                )
            } servicios · " +

                    "${
                        intent.getIntExtra(
                            "categorias",
                            0
                        )
                    } categorías · " +

                    "${
                        intent.getIntExtra(
                            "recursos",
                            0
                        )
                    } insumos"
    }


    private fun continuarDocumentos() {
        startActivity(Intent(this, DocumentosSalonActivity::class.java)
            .putExtra("idSalon", idSalon)
            .putExtra("nombreSalon", intent.getStringExtra("nombreSalon")))
    }
}