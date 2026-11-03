package com.example.ocasion_mobil_v2.ui.propietario

import com.example.ocasion_mobil_v2.util.ImagenSalonLoader
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Salon
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MisSalonesActivity : ComponentActivity() {

    private lateinit var containerSalones: LinearLayout
    private lateinit var layoutEmptySalones: LinearLayout
    private lateinit var tvSalonNumber: TextView

    private val salones = mutableListOf<Salon>()

    private var salonFotosPendiente: Int? = null
    private var subiendoFotos = false
    private var carga: kotlinx.coroutines.Job? = null
    private val selectorFotos = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uris ->
        val id = salonFotosPendiente
        salonFotosPendiente = null
        if (id != null && uris.isNotEmpty()) {
            subiendoFotos = true
            carga?.cancel()
            mostrarSalones()
            lifecycleScope.launch {
                try {
                    val respuesta = com.example.ocasion_mobil_v2.data.repository.CrearSalonRepository(this@MisSalonesActivity)
                        .reemplazarFotos(id, uris.map { it.toString() })
                    check(respuesta.isSuccessful) { "No se pudieron actualizar las fotos (${respuesta.code()})." }
                    Toast.makeText(this@MisSalonesActivity, "Fotos actualizadas", Toast.LENGTH_SHORT).show()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Toast.makeText(this@MisSalonesActivity, e.message ?: "No se pudieron subir las fotos.", Toast.LENGTH_LONG).show()
                } finally {
                    subiendoFotos = false
                }
                cargarSalonesReales()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        salonFotosPendiente?.let { outState.putInt("salonFotosPendiente", it) }
        super.onSaveInstanceState(outState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        salonFotosPendiente = savedInstanceState?.getInt("salonFotosPendiente")?.takeIf { it > 0 }
        setContentView(R.layout.activity_salones)

        inicializarVistas()
        configurarNavegacion()

        BottomNav.configurar(this)
    }

    // =====================================================
    // VISTAS
    // =====================================================

    private fun inicializarVistas() {
        containerSalones = findViewById(R.id.containerSalones)
        layoutEmptySalones = findViewById(R.id.layoutEmptySalones)
        tvSalonNumber = findViewById(R.id.tvSalonNumber)
    }

    // =====================================================
    // NAVEGACIÓN
    // =====================================================

    private fun configurarNavegacion() {
        val abrirFormulario = {
            val intent = Intent(this, SalonFormActivity::class.java)
            startActivity(intent)
        }

        findViewById<LinearLayout>(R.id.btnNuevoSalon).setOnClickListener {
            abrirFormulario()
        }

        findViewById<FloatingActionButton>(R.id.fabAddSalon).setOnClickListener {
            abrirFormulario()
        }
    }

    // =====================================================
    // LLAMADA A LA API (GET)
    // =====================================================

    private fun cargarSalonesReales() {
        if (subiendoFotos) return
        carga?.cancel()
        carga = lifecycleScope.launch {
            try {
                // Llamamos a Retrofit (el token se inyecta gracias al AuthInterceptor)
                val response = RetrofitClientSalones.getService(this@MisSalonesActivity).obtenerMisSalones()

                if (response.isSuccessful) {
                    val listaBackend = response.body() ?: emptyList()

                    // Mapeamos el JSON del backend a tu modelo visual 'Salon'
                    salones.clear()
                    listaBackend.forEach { dto ->
                        salones.add(
                            Salon(
                                id = dto.id_salon.toString(),
                                nombreSalon = dto.nombreSalon ?: "Sin nombre",
                                capacidadPersonas = dto.capacidadPersonas ?: 0,
                                descripcion = dto.descripcion ?: "",
                                precioHora = dto.precio_hora ?: 0.0,
                                ciudad = dto.ubicacion?.ciudad ?: "",
                                codigoPostal = dto.ubicacion?.cp ?: "",
                                calle = dto.ubicacion?.direccion ?: "",
                                numero = "",
                                fraccionamiento = "",
                                latitud = dto.ubicacion?.latitud ?: 0.0,
                                longitud = dto.ubicacion?.longitud ?: 0.0,
                                imagenes = dto.imagenes.orEmpty().filter { it.isNotBlank() },
                                estado = dto.estado,
                                estadoRevisionAdmin = dto.documentosValidacionSalonEntity?.estadoRevisionAdmin
                                    ?: if (dto.verificado) "APROBADO" else null,
                                observacionesAdmin = dto.documentosValidacionSalonEntity?.observacionesAdmin
                            )
                        )
                    }
                    mostrarSalones()
                } else {
                    Log.e("MisSalones", "Error HTTP al cargar salones: ${response.code()}")
                    Toast.makeText(
                        this@MisSalonesActivity,
                        "No se pudieron cargar los salones (${response.code()})",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("MisSalones", "Falló la carga o lectura de la respuesta de salones", e)
                Toast.makeText(
                    this@MisSalonesActivity,
                    "No se pudieron leer los salones. Revisa Logcat para ver el detalle.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // =====================================================
    // MOSTRAR SALONES EN PANTALLA
    // =====================================================

    private fun mostrarSalones() {
        containerSalones.removeAllViews()
        tvSalonNumber.text = salones.size.toString()

        if (salones.isEmpty()) {
            layoutEmptySalones.visibility = LinearLayout.VISIBLE
            return
        } else {
            layoutEmptySalones.visibility = LinearLayout.GONE
        }

        salones.forEach { salon ->
            val itemView = LayoutInflater.from(this).inflate(R.layout.item_salon, containerSalones, false)
            configurarItemSalon(itemView, salon)
            containerSalones.addView(itemView)
        }
    }

    // =====================================================
    // CONFIGURAR CADA TARJETA DE SALÓN
    // =====================================================

    private fun configurarItemSalon(view: View, salon: Salon) {
        val imgSalon = view.findViewById<ImageView>(R.id.imgSalon)
        val tvSalonName = view.findViewById<TextView>(R.id.tvSalonName)
        val tvSalonLocation = view.findViewById<TextView>(R.id.tvSalonLocation)
        val tvCapacity = view.findViewById<TextView>(R.id.tvCapacity)
        val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        val tvPriceBadge = view.findViewById<TextView>(R.id.tvPriceBadge)
        val tvDescription = view.findViewById<TextView>(R.id.tvDescription)

        // Textos
        tvSalonName.text = salon.nombreSalon
        tvSalonLocation.text = "📍 ${salon.ciudad}"
        tvCapacity.text = "👥 ${salon.capacidadPersonas} personas"
        tvPrice.text = "$${salon.precioHora} / hora"
        tvPriceBadge.text = "$${salon.precioHora} / hora"
        tvDescription.text = salon.descripcion
        view.findViewById<TextView>(R.id.tvEstadoRevisionSalon).text = when (salon.estadoRevisionAdmin) {
            "APROBADO" -> "Publicado · Verificado por el administrador"
            "PENDIENTE" -> "Publicado · Verificación pendiente"
            "RECHAZADO" -> "Publicado · Documentos rechazados"
            "SIN_ENVIAR" -> "Publicado · Documentos guardados, falta enviar"
            else -> if (salon.estado == "PUBLICADO") "Publicado · Sin verificar" else "${salon.estado ?: "Estado no disponible"} · Registro sin completar"
        }
        view.findViewById<TextView>(R.id.tvObservacionesSalon).apply {
            text = salon.observacionesAdmin?.let { "Observaciones: $it" }
            visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        }
        view.findViewById<MaterialButton>(R.id.btnDocumentosSalon).apply {
            text = when (salon.estadoRevisionAdmin) {
                "APROBADO", "PENDIENTE" -> "Consultar verificación"
                "RECHAZADO" -> "Corregir documentos"
                "SIN_ENVIAR" -> "Continuar envío a revisión"
                else -> "Subir documentos para verificación"
            }
            setOnClickListener {
                startActivity(Intent(this@MisSalonesActivity, DocumentosSalonActivity::class.java)
                    .putExtra("idSalon", salon.id.toInt()).putExtra("nombreSalon", salon.nombreSalon))
            }
        }

        ImagenSalonLoader.cargar(imgSalon, salon.imagenes.firstOrNull())

        view.findViewById<MaterialButton>(R.id.btnActualizarFotosSalon).apply {
            isEnabled = !subiendoFotos
            text = if (subiendoFotos) "Subiendo fotos…" else "Actualizar fotos"
            setOnClickListener {
                AlertDialog.Builder(this@MisSalonesActivity)
                    .setTitle("Actualizar fotos de ${salon.nombreSalon}")
                    .setMessage("Selecciona de 1 a 5 fotos JPG o PNG, de hasta 5 MB cada una. Reemplazarán todas las fotos actuales del salón.")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Seleccionar") { _, _ ->
                        salonFotosPendiente = salon.id.toIntOrNull()
                        selectorFotos.launch(androidx.activity.result.PickVisualMediaRequest(
                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }.show()
            }
        }
        imgSalon.setOnClickListener { ImagenSalonLoader.cargar(imgSalon, salon.imagenes.firstOrNull()) }

        // Botones
        view.findViewById<MaterialButton>(R.id.btnEditSalon).setOnClickListener {
            abrirEdicion(salon)
        }

        view.findViewById<MaterialButton>(R.id.btnDeleteSalon).setOnClickListener {
            confirmarBaja(salon)
        }

        view.findViewById<MaterialButton>(R.id.btnDisponibilidadSalon).setOnClickListener {
            abrirDisponibilidad(salon)
        }
    }

    // =====================================================
    // NAVEGACIÓN A EDITAR / DISPONIBILIDAD
    // =====================================================

    private fun abrirEdicion(salon: Salon) {
        val intent = Intent(this, SalonFormActivity::class.java)
        intent.putExtra("modoEdicion", true)
        intent.putExtra("idSalon", salon.id)
        intent.putExtra("nombreSalon", salon.nombreSalon)
        intent.putExtra("capacidadPersonas", salon.capacidadPersonas.toString())
        intent.putExtra("precioHora", salon.precioHora.toString())
        intent.putExtra("descripcion", salon.descripcion)
        intent.putExtra("ciudad", salon.ciudad)
        intent.putExtra("codigoPostal", salon.codigoPostal)
        intent.putExtra("calle", salon.calle)
        intent.putExtra("numero", salon.numero)
        intent.putExtra("fraccionamiento", salon.fraccionamiento)
        intent.putExtra("latitud", salon.latitud.toString())
        intent.putExtra("longitud", salon.longitud.toString())
        intent.putStringArrayListExtra("imagenes", ArrayList(salon.imagenes))

        startActivity(intent)
    }

    private fun abrirDisponibilidad(salon: Salon) {
        val intent = Intent(this, SalonDisponibilidadActivity::class.java)
        intent.putExtra("idSalon", salon.id)
        intent.putExtra("nombreSalon", salon.nombreSalon)
        startActivity(intent)
    }

    // =====================================================
    // ELIMINAR SALÓN (DELETE)
    // =====================================================

    private fun confirmarBaja(salon: Salon) {
        AlertDialog.Builder(this)
            .setTitle("Dar de baja salón")
            .setMessage("¿Seguro que deseas dar de baja \"${salon.nombreSalon}\"?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Dar de baja") { _, _ ->

                val idSalon = salon.id.toIntOrNull() ?: return@setPositiveButton

                lifecycleScope.launch {
                    try {
                        // Llamamos al DELETE en el servidor
                        val response = RetrofitClientSalones.getService(this@MisSalonesActivity).eliminarSalon(idSalon)

                        if (response.isSuccessful) {
                            salones.remove(salon)
                            mostrarSalones()
                            Toast.makeText(this@MisSalonesActivity, "Salón eliminado exitosamente.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MisSalonesActivity, "No se pudo eliminar el salón", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MisSalonesActivity, "Fallo de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }

    // =====================================================
    // ACTUALIZAR AL REGRESAR DE OTRA PANTALLA
    // =====================================================

    override fun onResume() {
        super.onResume()
        if (::containerSalones.isInitialized) {
            // Refresca la lista desde el servidor cada vez que regresas a esta pantalla
            cargarSalonesReales()
        }
    }
}
