package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.SalonCreateFase1Request
import com.example.ocasion_mobil_v2.data.remote.model.UbicacionRequest

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


        // ==========================================
        // PUBLICAR (Fase 1: Guardar Borrador)
        // ==========================================

        findViewById<MaterialButton>(R.id.btnPublicarSalon).setOnClickListener {
            val btn = it as MaterialButton

            // Bloqueamos el botón para evitar doble clic
            btn.isEnabled = false
            btn.text = "Guardando borrador..."

            lifecycleScope.launch {
                try {
                    // Preparamos el objeto con la ubicación
                    val ubicacionRequest = UbicacionRequest(
                        latitud = latitud.toDoubleOrNull() ?: 0.0,
                        longitud = longitud.toDoubleOrNull() ?: 0.0,
                        calle = calle,
                        numero = numero,
                        fraccionamiento = fraccionamiento,
                        ciudad = ciudad,
                        codigoPostal = codigoPostal
                    )

                    // Preparamos el objeto principal
                    val request = SalonCreateFase1Request(
                        nombreSalon = nombreSalon,
                        capacidadPersonas = capacidadPersonas.toIntOrNull() ?: 0,
                        precioHora = precioHora.toDoubleOrNull() ?: 0.0,
                        descripcion = descripcion,
                        ubicacion = ubicacionRequest
                    )

                    // Enviamos la petición al servidor
                    val response = RetrofitClientSalones.getService(this@SalonPublicacionActivity).crearSalonFase1(request)

                    if (response.isSuccessful) {
                        Toast.makeText(this@SalonPublicacionActivity, "¡Borrador guardado con éxito!", Toast.LENGTH_LONG).show()

                        // Regresamos a la pantalla de Mis Salones limpiando el historial intermedio
                        val intent = Intent(this@SalonPublicacionActivity, MisSalonesActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()

                    } else {
                        Toast.makeText(this@SalonPublicacionActivity, "Error al guardar el salón: ${response.code()}", Toast.LENGTH_LONG).show()
                        // Restauramos el botón si hubo error
                        btn.isEnabled = true
                        btn.text = "Publicar salón"
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SalonPublicacionActivity, "Fallo de red: ${e.message}", Toast.LENGTH_LONG).show()
                    // Restauramos el botón si hubo error de conexión
                    btn.isEnabled = true
                    btn.text = "Publicar salón"
                }
            }
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