package com.example.ocasion_mobil_v2.ui.cliente

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.util.ImagenSalonLoader
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.remote.model.SalonDetalleClienteDTO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class DetalleSalonActivity : FragmentActivity() {

    companion object {
        const val EXTRA_ID_SALON = "idSalon"
        const val EXTRA_NOMBRE = "nombre"
        const val EXTRA_IMAGEN = "imagen"
        const val EXTRA_PRECIO = "precio"
        const val EXTRA_CAPACIDAD = "capacidad"
        const val EXTRA_CIUDAD = "ciudad"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val id = intent.getIntExtra(EXTRA_ID_SALON, -1)
        if (id <= 0) {
            Toast.makeText(this, "No se encontró el salón.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (savedInstanceState == null) {
            val fragment = DetalleSalonFragment().apply {
                arguments = Bundle(intent.extras ?: Bundle())
            }

            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }
    }
}

class DetalleSalonFragment : Fragment(R.layout.fragment_detalle_salon) {

    private val idSalon: Int
        get() = requireArguments().getInt(DetalleSalonActivity.EXTRA_ID_SALON)

    private val localeMx = Locale.forLanguageTag("es-MX")
    private val moneda = NumberFormat.getCurrencyInstance(localeMx)
    private val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", localeMx)

    private var mes = YearMonth.now()
    private var calendarioAbierto = false
    private var consultaCalendario: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.getString("mes")?.let {
            mes = YearMonth.parse(it)
        }
        calendarioAbierto = savedInstanceState?.getBoolean("calendarioAbierto") ?: false

        val args = requireArguments()
        view.findViewById<TextView>(R.id.tvNombreDetalle).text =
            args.getString(DetalleSalonActivity.EXTRA_NOMBRE) ?: "Detalle del salón"

        mostrarResumen(
            view,
            args.getString(DetalleSalonActivity.EXTRA_CIUDAD),
            args.getInt(DetalleSalonActivity.EXTRA_CAPACIDAD, -1).takeIf { it >= 0 },
            args.getDouble(DetalleSalonActivity.EXTRA_PRECIO, -1.0).takeIf { it >= 0 }
        )

        view.findViewById<Button>(R.id.btnVolverDetalle).setOnClickListener {
            requireActivity().finish()
        }
        view.findViewById<Button>(R.id.btnReintentarDetalle).setOnClickListener {
            cargarDetalle(view)
        }
        view.findViewById<View>(R.id.btnDisponibilidadDetalle).setOnClickListener {
            calendarioAbierto = true
            cargarCalendario(view)
        }
        view.findViewById<Button>(R.id.btnMesAnteriorDetalle).setOnClickListener {
            mes = mes.minusMonths(1)
            cargarCalendario(view)
        }
        view.findViewById<Button>(R.id.btnMesSiguienteDetalle).setOnClickListener {
            mes = mes.plusMonths(1)
            cargarCalendario(view)
        }
        view.findViewById<Button>(R.id.btnReintentarCalendarioDetalle)
            .setOnClickListener { cargarCalendario(view) }

        cargarDetalle(view)
        if (calendarioAbierto) cargarCalendario(view)
    }

    private fun cargarDetalle(view: View) {
        val estado = view.findViewById<TextView>(R.id.tvEstadoDetalle)
        val retry = view.findViewById<Button>(R.id.btnReintentarDetalle)

        estado.isVisible = true
        estado.text = "Cargando ficha…"
        retry.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClientSalones.getService(requireContext())
                val respuesta = api.obtenerDetalleCliente(idSalon)

                if (!respuesta.isSuccessful) {
                    estado.text = if (respuesta.code() == 404) {
                        "Este salón ya no está disponible para consulta."
                    } else {
                        "No se pudo cargar la ficha (${respuesta.code()})."
                    }
                    retry.isVisible = respuesta.code() != 404
                    return@launch
                }

                val salon = checkNotNull(respuesta.body())
                mostrarDetalle(view, salon)
                estado.isVisible = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                estado.text = "No se pudo leer la ficha. Revisa la conexión y reintenta."
                retry.isVisible = true
            }
        }
    }

    private fun mostrarDetalle(view: View, salon: SalonDetalleClienteDTO) {
        view.findViewById<View>(R.id.contenidoDetalle).isVisible = true

        salon.nombreSalon?.let {
            view.findViewById<TextView>(R.id.tvNombreDetalle).text = it
        }
        mostrarResumen(view, salon.ciudad, salon.capacidadPersonas, salon.precioHora)

        view.findViewById<TextView>(R.id.tvDescripcionDetalle).text =
            salon.descripcion.orEmpty().ifBlank { "Sin descripción registrada." }

        view.findViewById<TextView>(R.id.tvServiciosDetalle).text =
            salon.serviciosCompletos.orEmpty()
                .filter { it.isNotBlank() }
                .joinToString("\n") { "• $it" }
                .ifBlank { "Sin servicios registrados." }

        val portada = requireArguments().getString(DetalleSalonActivity.EXTRA_IMAGEN)
        val fotos = salon.galeriaFotos.orEmpty()
            .filter { it.isNotBlank() }
            .ifEmpty { listOfNotNull(portada?.takeIf { it.isNotBlank() }) }
            .distinct()
            .take(5)

        val fila = view.findViewById<LinearLayout>(R.id.filaFotosDetalle)
        fila.removeAllViews()
        view.findViewById<View>(R.id.scrollFotosDetalle).isVisible = fotos.isNotEmpty()

        fotos.forEachIndexed { indice, url ->
            val imagen = ImageView(requireContext()).apply {
                contentDescription = "Foto ${indice + 1} de ${fotos.size}"
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val params = LinearLayout.LayoutParams(dp(270), dp(175)).apply {
                marginEnd = dp(8)
            }
            fila.addView(imagen, params)

            ImagenSalonLoader.cargar(imagen, url)
        }

        val direccion = listOfNotNull(salon.direccion, salon.ciudad)
            .filter { it.isNotBlank() }
            .joinToString(", ")

        view.findViewById<TextView>(R.id.tvDireccionDetalle).text =
            direccion.ifBlank { "Dirección pendiente de registrar." }

        val lat = salon.latitud
        val lon = salon.longitud
        val consultaMapa = if (lat != null && lon != null) "$lat,$lon" else direccion

        view.findViewById<Button>(R.id.btnMapsDetalle).apply {
            isVisible = consultaMapa.isNotBlank()
            setOnClickListener {
                val uri = Uri.parse("https://www.google.com/maps/search/")
                    .buildUpon()
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("query", consultaMapa)
                    .build()

                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        requireContext(),
                        "No hay una aplicación disponible para abrir el mapa.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun mostrarResumen(
        view: View,
        ciudad: String?,
        capacidad: Int?,
        precio: Double?
    ) {
        view.findViewById<TextView>(R.id.tvResumenDetalle).text =
            listOfNotNull(
                ciudad?.takeIf { it.isNotBlank() },
                capacidad?.let { "$it personas" },
                precio?.let { "${moneda.format(it)} / hr" }
            ).joinToString(" · ")
    }

    private fun cargarCalendario(view: View) {
        // Si el cliente cambia rápido de mes, solo se muestra la consulta más reciente.
        consultaCalendario?.cancel()

        val mesConsultado = mes
        val grid = view.findViewById<GridLayout>(R.id.gridDiasDetalle)
        val estado = view.findViewById<TextView>(R.id.tvEstadoCalendarioDetalle)
        val retry = view.findViewById<Button>(R.id.btnReintentarCalendarioDetalle)

        view.findViewById<View>(R.id.panelCalendarioDetalle).isVisible = true
        view.findViewById<TextView>(R.id.tvMesDetalle).text =
            mesConsultado.format(formatoMes).replaceFirstChar { it.uppercase() }

        // Al cambiar de mes se retiran los días anteriores antes de consultar.
        grid.removeAllViews()
        retry.isVisible = false
        estado.text = "Consultando disponibilidad…"

        consultaCalendario = viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = RetrofitClientSalones.getService(requireContext())
                val respuesta = api.obtenerCalendarioCliente(
                    idSalon = idSalon,
                    mes = mesConsultado.monthValue,
                    anio = mesConsultado.year
                )
                check(respuesta.isSuccessful)

                val bloqueos = checkNotNull(respuesta.body())
                val ocupados = bloqueos.map { LocalDate.parse(it.fecha) }.toSet()

                estado.text = "Verde: libre · Rojo: con horario bloqueado"
                dibujarMes(grid, mesConsultado, ocupados, estado)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                grid.removeAllViews()
                estado.text = "No se pudo confirmar la disponibilidad de este mes."
                retry.isVisible = true
            }
        }
    }

    private fun dibujarMes(
        grid: GridLayout,
        mesConsultado: YearMonth,
        ocupados: Set<LocalDate>,
        estado: TextView
    ) {
        fun celda(texto: String): TextView {
            val celda = TextView(requireContext()).apply {
                text = texto
                gravity = Gravity.CENTER
            }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(44)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(1), dp(1), dp(1), dp(1))
            }
            grid.addView(celda, params)
            return celda
        }

        listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do").forEach { celda(it) }

        // LocalDate numera lunes = 1 y domingo = 7.
        repeat(mesConsultado.atDay(1).dayOfWeek.value - 1) { celda("") }

        for (dia in 1..mesConsultado.lengthOfMonth()) {
            val fecha = mesConsultado.atDay(dia)
            val libre = fecha !in ocupados

            celda(dia.toString()).apply {
                val color = if (libre) R.color.verde_primario else R.color.rojo_error
                setBackgroundColor(requireContext().getColor(color))
                setTextColor(Color.WHITE)
                contentDescription = if (libre) {
                    "$fecha, disponible"
                } else {
                    "$fecha, con horario bloqueado"
                }

                isEnabled = libre
                if (libre) {
                    setOnClickListener { estado.text = "$fecha · Disponible" }
                }
            }
        }
    }

    private fun dp(valor: Int): Int =
        (valor * resources.displayMetrics.density + 0.5f).toInt()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("mes", mes.toString())
        outState.putBoolean("calendarioAbierto", calendarioAbierto)
    }
}