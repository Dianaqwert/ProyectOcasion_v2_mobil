package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.data.remote.model.ServicioReservado
import com.example.ocasion_mobil_v2.data.repository.StockCotizacion
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CotizarPropietarioActivity : PantallaPropietario() {

    private val vm by lazy {
        ViewModelProvider(this)[
            CotizarPropietarioViewModel::class.java
        ]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!preparar("Armar cotización")) return

        val id = intent.getLongExtra("idReservacion", -1)

        if (id <= 0) {
            finish()
            return
        }

        val scroll = NestedScrollView(this)

        raiz.addView(
            scroll,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        val formulario = columna()
        scroll.addView(formulario)

        val resumen = formulario.texto(
            "Cargando solicitud…",
            20f
        )

        val error = formulario.texto("")

        val reintentar = formulario.boton(
            "Actualizar solicitud / Reintentar"
        ) {
            vm.cargar()
        }

        val renta = formulario.entrada(
            "Precio de renta del salón",
            InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL
        )

        renta.input.filters =
            arrayOf(InputFilter.LengthFilter(12))

        renta.input.doAfterTextChanged {
            vm.renta(it.toString())
        }

        formulario.texto("Servicios solicitados", 20f)

        val servicios = columna()
        formulario.addView(servicios)

        val camposServicios = mutableMapOf<Long, Entrada>()
        var serviciosMostrados: List<ServicioReservado>? = null

        formulario.texto(
            "Inventario para estas fechas",
            20f
        )

        val inventario = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(
                this@CotizarPropietarioActivity
            )
            isNestedScrollingEnabled = false
            itemAnimator = null
        }

        val adapter = InventarioCotizacionAdapter {
                recursoId, texto ->
            vm.cantidad(recursoId, texto)
        }

        inventario.adapter = adapter

        formulario.addView(
            inventario,
            LinearLayout.LayoutParams(-1, -2)
        )

        val stockVacio = formulario.texto("")

        val notas = formulario.entrada(
            "Observaciones (opcional)",
            InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        )

        notas.input.filters =
            arrayOf(InputFilter.LengthFilter(500))

        notas.input.doAfterTextChanged {
            vm.notas(it.toString())
        }

        formulario.texto(
            "El servidor calculará el total final de la cotización."
        )

        val enviar = formulario.boton(
            "Enviar cotización"
        ) {
            vm.enviar()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    while (true) {
                        delay(30_000)

                        if (vm.state.value.verificado) {
                            vm.cargar()
                        }
                    }
                }

                vm.state.collect { estado ->

                    if (estado.enviado) {
                        Toast.makeText(
                            this@CotizarPropietarioActivity,
                            "Cotización enviada",
                            Toast.LENGTH_LONG
                        ).show()

                        setResult(RESULT_OK)
                        finish()
                        return@collect
                    }

                    resumen.text = estado.reserva?.let {
                        desglose(it)
                    } ?: "Cargando solicitud…"

                    error.mostrarError(estado.error)

                    reintentar.isEnabled =
                        !estado.cargando && !estado.enviando

                    reintentar.text =
                        if (estado.cargando) {
                            "Consultando disponibilidad…"
                        } else {
                            "Actualizar solicitud / Reintentar"
                        }

                    val editable =
                        estado.verificado &&
                                !estado.cargando &&
                                !estado.enviando

                    renta.input.isEnabled = editable
                    notas.input.isEnabled = editable

                    if (
                        renta.input.text.toString() !=
                        estado.renta
                    ) {
                        renta.input.setText(estado.renta)
                    }

                    if (
                        notas.input.text.toString() !=
                        estado.notas
                    ) {
                        notas.input.setText(estado.notas)
                    }

                    renta.campo.error =
                        if (
                            estado.renta.isNotEmpty() &&
                            ReglasPropietario.dinero(
                                estado.renta
                            ) == null
                        ) {
                            "Ingresa un precio válido, con hasta dos decimales."
                        } else {
                            null
                        }

                    val lista =
                        estado.reserva?.servicios.orEmpty()

                    if (serviciosMostrados != lista) {
                        serviciosMostrados = lista

                        servicios.removeAllViews()
                        camposServicios.clear()

                        if (lista.isEmpty()) {
                            servicios.texto(
                                "Sin servicios adicionales solicitados."
                            )
                        }

                        lista.forEach { servicio ->
                            servicios.texto(
                                "${servicio.nombre ?: "Servicio #${servicio.id}"}" +
                                        " · Cantidad: ${servicio.cantidad ?: "—"}"
                            )

                            servicio.id?.let { servicioId ->

                                val campo = servicios.entrada(
                                    "Precio unitario contratado",
                                    InputType.TYPE_CLASS_NUMBER or
                                            InputType.TYPE_NUMBER_FLAG_DECIMAL
                                )

                                campo.input.filters = arrayOf(
                                    InputFilter.LengthFilter(12)
                                )

                                campo.input.doAfterTextChanged {
                                    vm.precio(
                                        servicioId,
                                        it.toString()
                                    )
                                }

                                camposServicios[servicioId] =
                                    campo
                            }
                        }
                    }

                    camposServicios.forEach {
                            (servicioId, campo) ->

                        val valor =
                            estado.precios[servicioId].orEmpty()

                        if (
                            campo.input.text.toString() != valor
                        ) {
                            campo.input.setText(valor)
                        }

                        campo.input.isEnabled = editable

                        campo.campo.error =
                            if (
                                editable &&
                                ReglasPropietario.dinero(valor) == null
                            ) {
                                "Ingresa el precio de este servicio."
                            } else {
                                null
                            }
                    }

                    adapter.actualizar(
                        estado.stock,
                        estado.cantidades,
                        editable
                    )

                    stockVacio.isVisible =
                        estado.verificado &&
                                estado.stock.isEmpty()

                    stockVacio.text =
                        "El salón no tiene recursos registrados para asignar."

                    enviar.isEnabled = estado.puedeEnviar

                    enviar.text =
                        if (estado.enviando) {
                            "Verificando y enviando…"
                        } else {
                            "Enviar cotización"
                        }
                }
            }
        }

        vm.iniciar(id)
    }
}

private class InventarioCotizacionAdapter(
    private val cambiar: (Long, String) -> Unit
) : RecyclerView.Adapter<InventarioCotizacionAdapter.Fila>() {

    private var items = emptyList<StockCotizacion>()
    private var cantidades = emptyMap<Long, String>()
    private var editable = false

    inner class Fila(
        private val panel: LinearLayout
    ) : RecyclerView.ViewHolder(panel) {

        private val nombre = panel.texto("", 18f)
        private val stock = panel.texto("")

        private val cantidad = panel.entrada(
            "Unidades a asignar",
            InputType.TYPE_CLASS_NUMBER
        )

        private val menos = panel.boton(
            "− Quitar una unidad"
        ) {
            ajustar(-1)
        }

        private val mas = panel.boton(
            "+ Agregar una unidad"
        ) {
            ajustar(1)
        }

        private var recursoId: Long? = null
        private var vinculando = false

        init {
            cantidad.input.filters = arrayOf(
                InputFilter.LengthFilter(10)
            )

            cantidad.input.doAfterTextChanged {
                if (!vinculando) {
                    recursoId?.let { id ->
                        cambiar(id, it.toString())
                    }
                }
            }
        }

        private fun ajustar(delta: Int) {
            val recurso = items.find {
                it.id == recursoId
            } ?: return

            if (!editable) return

            val actual =
                cantidades[recurso.id]?.toIntOrNull() ?: 0

            val siguiente = actual.toLong() + delta

            if (siguiente > recurso.disponible) {
                Toast.makeText(
                    panel.context,
                    "Stock insuficiente para esta fecha",
                    Toast.LENGTH_SHORT
                ).show()

                return
            }

            if (siguiente < 0) return

            cambiar(
                recurso.id,
                siguiente.toString()
            )

            if (
                delta > 0 &&
                siguiente == recurso.disponible.toLong()
            ) {
                Toast.makeText(
                    panel.context,
                    "Stock insuficiente para esta fecha",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fun bind(recurso: StockCotizacion) {
            recursoId = recurso.id
            vinculando = true

            nombre.text = recurso.nombre

            stock.text =
                "Total: ${recurso.total}" +
                        " · Ocupado: ${recurso.ocupado}" +
                        " · Disponible: ${recurso.disponible}"

            val texto = cantidades[recurso.id] ?: "0"

            if (
                cantidad.input.text.toString() != texto
            ) {
                cantidad.input.setText(texto)
                cantidad.input.setSelection(texto.length)
            }

            val valido = ReglasPropietario.cantidad(
                texto,
                recurso.disponible
            )

            cantidad.campo.error =
                if (valido == null) {
                    if (
                        (texto.toLongOrNull() ?: -1) >
                        recurso.disponible
                    ) {
                        "Stock insuficiente para esta fecha"
                    } else {
                        "Ingresa una cantidad válida."
                    }
                } else {
                    null
                }

            cantidad.campo.helperText =
                if (valido == recurso.disponible) {
                    "Stock insuficiente para esta fecha"
                } else {
                    null
                }

            cantidad.input.isEnabled = editable

            menos.isEnabled =
                editable &&
                        (texto.toIntOrNull() ?: 0) > 0

            mas.isEnabled =
                editable &&
                        valido != null &&
                        valido < recurso.disponible

            panel.tooltipText =
                if (!mas.isEnabled && editable) {
                    "Stock insuficiente para esta fecha"
                } else {
                    null
                }

            vinculando = false
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Fila =
        Fila(
            parent.context.columna().apply {
                layoutParams =
                    RecyclerView.LayoutParams(-1, -2)
            }
        )

    override fun onBindViewHolder(
        holder: Fila,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun onBindViewHolder(
        holder: Fila,
        position: Int,
        payloads: MutableList<Any>
    ) {
        holder.bind(items[position])
    }

    fun actualizar(
        stock: List<StockCotizacion>,
        numeros: Map<Long, String>,
        habilitado: Boolean
    ) {
        val anteriores = items
        val cantidadesAnteriores = cantidades
        val editableAnterior = editable

        items = stock
        cantidades = numeros
        editable = habilitado

        if (
            anteriores.map { it.id } !=
            items.map { it.id }
        ) {
            notifyDataSetChanged()
            return
        }

        items.forEachIndexed { index, recurso ->
            if (
                anteriores[index] != recurso ||
                cantidadesAnteriores[recurso.id] !=
                numeros[recurso.id] ||
                editableAnterior != habilitado
            ) {
                notifyItemChanged(index, "estado")
            }
        }
    }
}