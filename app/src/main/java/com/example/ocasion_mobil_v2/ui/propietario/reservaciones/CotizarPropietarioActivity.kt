package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.os.Bundle
import android.graphics.Typeface
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
import com.example.ocasion_mobil_v2.R
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

        val editar = intent.getBooleanExtra("editar", false)
        if (!preparar(if (editar) "Editar cotización" else "Armar cotización")) return

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

        val formulario = columna().apply { setPadding(dp(1), dp(6), dp(1), dp(12)) }
        scroll.addView(formulario)

        val evento = formulario.seccion("Solicitud #$id", destacada = true)
        val resumen = evento.texto("Cargando solicitud…", 22f).apply { setTypeface(typeface, Typeface.BOLD) }
        val datosEvento = evento.texto("", 14f).apply { setTextColor(context.getColor(R.color.texto_secundario)) }

        val documentos = ViewModelProvider(this)[PanelPropietarioViewModel::class.java]
        val pdf = evento.boton("Ver PDF guardado", secundario = true) { documentos.descargar(id) }
        evento.texto("El PDF muestra los importes y el mobiliario guardados en la reserva.", 14f)
        val errorPdf = evento.texto("")
        val error = formulario.texto("")

        val reintentar = formulario.boton(
            "Actualizar solicitud", secundario = true
        ) {
            vm.cargar()
        }

        val cotizacion = formulario.seccion("Renta base")
        val tipoImporte = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val renta = cotizacion.entrada(
            "Renta del salón (MXN)",
            tipoImporte
        )

        renta.input.filters =
            arrayOf(InputFilter.LengthFilter(12))

        renta.input.doAfterTextChanged {
            vm.renta(it.toString())
        }

        val servicios = formulario.seccion("Servicios solicitados")
        servicios.texto("Captura el precio unitario. Usa 0 si el servicio está incluido.", 14f)
        val filasServicios = columna().apply { setPadding(0, 0, 0, 0) }
        servicios.addView(filasServicios)
        var serviciosMostrados: List<ServicioReservado>? = null
        val entradas = mutableMapOf<Long, Pair<ServicioReservado, Entrada>>()
        var aplicandoPrecios = false
        val mobiliario = formulario.seccion("Mobiliario disponible")
        mobiliario.texto("Asignar mobiliario es opcional.", 14f)

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

        mobiliario.addView(
            inventario,
            LinearLayout.LayoutParams(-1, -2)
        )

        val stockVacio = mobiliario.texto("")

        val total = raiz.texto("", 18f).apply { setTypeface(typeface, Typeface.BOLD) }

        val enviar = raiz.boton(
            "Enviar cotización"
        ) {
            vm.enviar()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    documentos.state.collect { documento ->
                        pdf.isEnabled = documento.descargando == null
                        errorPdf.mostrarError(documento.error)
                        documento.documento?.let {
                            documentos.documentoAbierto()
                            abrirPdfPropietario(it)
                        }
                    }
                }

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

                    resumen.text = estado.reserva?.nombreSalon ?: if (estado.cargando) "Cargando solicitud…" else "Datos del evento"
                    datosEvento.text = estado.reserva?.let {
                        listOfNotNull(it.nombreCliente, it.correoCliente, it.fechaYHorario()).joinToString("\n")
                    }

                    error.mostrarError(estado.error)

                    reintentar.isEnabled =
                        !estado.cargando && !estado.enviando

                    reintentar.text =
                        if (estado.cargando) {
                            "Consultando disponibilidad…"
                        } else {
                            "Actualizar solicitud"
                        }

                    val editable =
                        estado.verificado &&
                                !estado.cargando &&
                                !estado.enviando

                    renta.input.isEnabled = editable

                    if (
                        renta.input.text.toString() !=
                        estado.renta
                    ) {
                        renta.input.setText(estado.renta)
                    }

                    renta.campo.error =
                        if (
                            estado.renta.isNotEmpty() &&
                            ReglasPropietario.dinero(
                                estado.renta
                            )?.signum() != 1
                        ) {
                            "Ingresa un precio válido, con hasta dos decimales."
                        } else {
                            null
                        }

                    if (serviciosMostrados != estado.servicios) {
                        serviciosMostrados = estado.servicios
                        filasServicios.removeAllViews()
                        entradas.clear()
                        if (estado.servicios.isEmpty()) filasServicios.texto("Sin servicios adicionales solicitados.", 14f)
                        for (servicio in estado.servicios) {
                            val idServicio = servicio.id?.takeIf { it > 0 }
                            val nombre = servicio.nombre ?: "Servicio"
                            if (idServicio == null) {
                                filasServicios.texto("$nombre · Cantidad ${servicio.cantidad ?: 1}", 14f)
                                continue
                            }
                            if (idServicio in entradas) continue
                            val cantidad = estado.servicios.filter { it.id == idServicio }.sumOf { it.cantidad ?: 1 }
                            filasServicios.texto("$nombre · Cantidad $cantidad", 15f)
                            val entrada = filasServicios.entrada("Precio unitario (MXN)", tipoImporte)
                            entrada.input.filters = arrayOf(InputFilter.LengthFilter(12))
                            entrada.input.doAfterTextChanged {
                                if (!aplicandoPrecios) vm.precioServicio(idServicio, it.toString())
                            }
                            entradas[idServicio] = servicio to entrada
                        }
                    }
                    aplicandoPrecios = true
                    entradas.values.forEach { (servicio, entrada) ->
                        val precio = estado.precio(servicio)
                        if (entrada.input.text.toString() != precio) entrada.input.setText(precio)
                        entrada.input.isEnabled = editable
                        entrada.campo.error = if (ReglasPropietario.dinero(precio) == null)
                            "Ingresa un importe desde 0 con hasta dos decimales." else null
                    }
                    aplicandoPrecios = false
                    total.text = importeReserva(estado.totalCotizado?.toPlainString())
                        ?.let { "Total cotizado · $it" } ?: "Total por calcular"

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
                            if (editar) "Guardar cotización" else "Enviar cotización"
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

        private val controles = LinearLayout(panel.context).apply {
            orientation = LinearLayout.HORIZONTAL
            panel.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = panel.context.dp(4) })
        }
        private val menos = controles.boton(
            "− Quitar", secundario = true
        ) {
            ajustar(-1)
        }

        private val mas = controles.boton(
            "+ Agregar", secundario = true
        ) {
            ajustar(1)
        }

        private var recursoId: Long? = null
        private var vinculando = false

        init {
            menos.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = panel.context.dp(4) }
            mas.layoutParams = LinearLayout.LayoutParams(0, -2, 1f).apply { marginStart = panel.context.dp(4) }
            nombre.setTypeface(nombre.typeface, Typeface.BOLD)
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

            stock.text = "${recurso.disponible} disponibles · ${recurso.ocupado} ocupadas de ${recurso.total}"

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
