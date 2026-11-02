package com.example.ocasion_mobil_v2.ui.cliente

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.slider.RangeSlider
import com.google.android.material.slider.Slider
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class FiltrosSalonesBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val RESULT_KEY = "resultado_filtros_salones"
        private const val BORRADOR = "borrador_filtros"
    }

    private val viewModel: ExplorarSalonesViewModel by activityViewModels()
    private val serviciosSeleccionados = mutableSetOf<Int>()

    private lateinit var capacidad: RangeSlider
    private lateinit var precio: Slider
    private lateinit var ciudadCp: AutoCompleteTextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.bottom_sheet_filtros_salones,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        capacidad = view.findViewById(R.id.sliderCapacidad)
        precio = view.findViewById(R.id.sliderPrecio)
        ciudadCp = view.findViewById(R.id.etCiudadCp)

        val textoCapacidad = view.findViewById<TextView>(R.id.tvRangoCapacidad)
        val textoPrecio = view.findViewById<TextView>(R.id.tvPrecioMaximo)
        val chips = view.findViewById<ChipGroup>(R.id.chipsServiciosFiltro)
        val estado = view.findViewById<TextView>(R.id.tvEstadoServiciosFiltro)
        val reintentar = view.findViewById<MaterialButton>(
            R.id.btnReintentarServiciosFiltro
        )

        // Al abrir toma los filtros del menú. Al girar el teléfono
        // recupera los cambios que el cliente todavía no ha aplicado.
        val inicial = savedInstanceState?.getBundle(BORRADOR)
            ?: arguments
            ?: Bundle()

        serviciosSeleccionados.clear()
        serviciosSeleccionados.addAll(
            inicial.getIntegerArrayList("servicios").orEmpty()
        )

        capacidad.valueFrom = 50f
        capacidad.valueTo = 1500f
        capacidad.stepSize = 1f
        capacidad.values = listOf(
            inicial.getInt("capacidadMin", 50).toFloat(),
            inicial.getInt("capacidadMax", 1500).toFloat()
        )

        precio.valueFrom = 0f
        precio.valueTo = 10_000f
        precio.stepSize = 50f
        precio.value = inicial.getFloat("precioMax", 10_000f)

        val moneda = NumberFormat.getCurrencyInstance(
            Locale.forLanguageTag("es-MX")
        )

        fun actualizarEtiquetas() {
            val minimo = capacidad.values.first().toInt()
            val maximo = capacidad.values.last().toInt()
            textoCapacidad.text = "Capacidad: $minimo a $maximo personas"

            textoPrecio.text = if (precio.value == precio.valueTo) {
                "Precio por hora: sin límite"
            } else {
                "Precio máximo: ${moneda.format(precio.value.toDouble())} / hr"
            }
        }

        capacidad.addOnChangeListener { _, _, _ -> actualizarEtiquetas() }
        precio.addOnChangeListener { _, _, _ -> actualizarEtiquetas() }
        actualizarEtiquetas()

        // D2 permite buscar por ciudad o CP, pero las tarjetas solo
        // incluyen ciudad. Para sugerir CP usamos búsquedas anteriores.
        val prefs = requireContext().getSharedPreferences(
            "exploracion_cliente",
            Context.MODE_PRIVATE
        )
        val historial = prefs.getStringSet("ubicaciones", emptySet()).orEmpty()
        val ciudades = viewModel.state.value.salones.mapNotNull { it.ciudad }
        val sugerencias = (ciudades + historial)
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        ciudadCp.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                sugerencias
            )
        )
        ciudadCp.setText(inicial.getString("ciudad").orEmpty(), false)

        fun cargarServicios() {
            estado.isVisible = true
            estado.text = "Cargando servicios…"
            reintentar.isVisible = false

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val api = RetrofitClientSalones.getService(requireContext())
                    val respuesta = api.obtenerCatalogoServicios()
                    if (!respuesta.isSuccessful) {
                        error("HTTP ${respuesta.code()}")
                    }

                    val servicios = checkNotNull(respuesta.body())
                    chips.removeAllViews()

                    for (servicio in servicios) {
                        val chip = Chip(requireContext()).apply {
                            text = servicio.nombreServicio
                            isCheckable = true
                            isChecked = servicio.idServicio in serviciosSeleccionados

                            setOnCheckedChangeListener { _, marcado ->
                                if (marcado) {
                                    serviciosSeleccionados.add(servicio.idServicio)
                                } else {
                                    serviciosSeleccionados.remove(servicio.idServicio)
                                }
                            }
                        }
                        chips.addView(chip)
                    }

                    estado.text = "No hay servicios registrados."
                    estado.isVisible = servicios.isEmpty()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    estado.text = "No se pudo cargar el catálogo de servicios."
                    reintentar.isVisible = true
                }
            }
        }

        reintentar.setOnClickListener { cargarServicios() }
        cargarServicios()

        view.findViewById<MaterialButton>(R.id.btnAplicarFiltros)
            .setOnClickListener {
                val lugar = ciudadCp.text.toString().trim()
                if (lugar.isNotEmpty()) {
                    val guardadas = prefs.getStringSet("ubicaciones", emptySet())
                        .orEmpty()
                    prefs.edit()
                        .putStringSet("ubicaciones", guardadas + lugar)
                        .apply()
                }

                parentFragmentManager.setFragmentResult(
                    RESULT_KEY,
                    leerFiltros()
                )
                dismiss()
            }

        view.findViewById<MaterialButton>(R.id.btnLimpiarFiltros)
            .setOnClickListener {
                val resultado = Bundle().apply {
                    putBoolean("limpiar", true)
                }
                parentFragmentManager.setFragmentResult(RESULT_KEY, resultado)
                dismiss()
            }
    }

    private fun leerFiltros(): Bundle {
        return Bundle().apply {
            putInt("capacidadMin", capacidad.values.first().toInt())
            putInt("capacidadMax", capacidad.values.last().toInt())
            putFloat("precioMax", precio.value)
            putString("ciudad", ciudadCp.text.toString().trim())
            putIntegerArrayList("servicios", ArrayList(serviciosSeleccionados))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (view != null) {
            outState.putBundle(BORRADOR, leerFiltros())
        }
    }
}