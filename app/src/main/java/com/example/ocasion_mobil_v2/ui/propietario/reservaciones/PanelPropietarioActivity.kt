package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.ui.cliente.pagos.EstadoVisualReserva
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.format.DateTimeFormatter
import java.util.Locale

class PanelPropietarioActivity : PantallaPropietario() {

    private var reservaPorAbrir: Long = -1L

    private val vm by lazy {
        ViewModelProvider(this)[
            PanelPropietarioViewModel::class.java
        ]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val nuevas =
            intent.getBooleanExtra("soloNuevas", false)

        if (
            !preparar(
                if (nuevas) {
                    "Solicitudes nuevas"
                } else {
                    "Panel mensual"
                }
            )
        ) return

        reservaPorAbrir = if (savedInstanceState == null) {
            intent.getLongExtra(EXTRA_RESERVA_CON_LLEGADA, -1L)
        } else {
            savedInstanceState.getLong(EXTRA_RESERVA_CON_LLEGADA, -1L)
        }

        val controles = raiz.seccion(destacada = true)
        val aviso = controles.texto("", 16f).apply {
            setTypeface(typeface, Typeface.BOLD)
            setAccessibilityHeading(true)
        }
        val filtro = controles.boton(
            "Seleccionar mes y año", secundario = true
        ) {
            seleccionarMes()
        }.apply {
            isVisible = !nuevas
        }

        val actualizar = controles.boton(
            "Actualizar", secundario = true
        ) {
            vm.cargar()
        }

        val error = raiz.texto("")

        val lista = RecyclerView(this).apply {
            clipToPadding = false
            setPadding(dp(1), dp(2), dp(1), dp(10))
            layoutManager = LinearLayoutManager(
                this@PanelPropietarioActivity
            )
        }

        val adaptador = EventosPropietarioAdapter(
            abrir = { reserva ->
                when {
                    ReglasPropietario.puedeCotizar(reserva) -> {
                        startActivity(
                            Intent(
                                this,
                                CotizarPropietarioActivity::class.java
                            ).putExtra("editar", reserva.estado == "ESPERANDO_PAGO").putExtra(
                                "idReservacion",
                                reserva.id
                            )
                        )
                    }

                    reserva.estado == "COTIZACION_PENDIENTE" -> {
                        startActivity(
                            Intent(
                                this,
                                ReservacionesActivity::class.java
                            )
                        )
                    }

                    else -> {
                        mostrarEvento(reserva)
                    }
                }
            },

            pdf = {
                vm.descargar(it.id)
            }
        )

        lista.adapter = adaptador

        raiz.addView(
            lista,
            LinearLayout.LayoutParams(-1, 0, 1f)
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { estado ->

                    if (!nuevas && !estado.cargando && estado.error == null && reservaPorAbrir > 0) {
                        estado.reservas.firstOrNull { it.id == reservaPorAbrir }?.let { reserva ->
                            reservaPorAbrir = -1L
                            ReglasPropietario.fecha(reserva.fechaInicio)?.let { vm.mes(YearMonth.from(it)) }
                            mostrarEvento(reserva)
                        }
                    }

                    filtro.text = estado.mes.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.forLanguageTag("es-MX")
                        )
                    ) + " · Cambiar"

                    actualizar.isEnabled = !estado.cargando
                    actualizar.text = if (estado.error == null) "Actualizar" else "Reintentar"

                    error.mostrarError(estado.error)

                    val eventos = estado.reservas
                        .filter {
                            if (nuevas) {
                                it.estado == "SOLICITUD_NUEVA"
                            } else {
                                ReglasPropietario.delMes(
                                    it,
                                    estado.mes
                                )
                            }
                        }
                        .sortedWith(
                            compareBy<Reservacion> {
                                ReglasPropietario.fecha(
                                    it.fechaInicio
                                ) ?: LocalDate.MAX
                            }.thenBy {
                                it.id
                            }
                        )

                    aviso.text = when {
                        estado.cargando ->
                            "Cargando solicitudes…"

                        estado.error != null -> "Consulta interrumpida"

                        eventos.isEmpty() ->
                            if (nuevas) {
                                "No hay solicitudes nuevas."
                            } else {
                                "No hay eventos en este mes."
                            }

                        else ->
                            if (nuevas) {
                                if (eventos.size == 1) "1 solicitud nueva" else "${eventos.size} solicitudes nuevas"
                            } else {
                                if (eventos.size == 1) "1 evento" else "${eventos.size} eventos"
                            }
                    }

                    adaptador.actualizar(
                        eventos,
                        estado.descargando,
                        estado.cargando
                    )

                    estado.documento?.let { archivo ->
                        vm.documentoAbierto()

                        abrirPdfPropietario(archivo)
                    }
                }
            }
        }
    }

    private fun mostrarEvento(reserva: Reservacion) {
        val contenido = columna()
        val ficha = contenido.seccion(reserva.nombreSalon ?: "Datos del evento", destacada = true)
        reserva.nombreCliente?.let { ficha.texto(it, 16f) }
        reserva.correoCliente?.let { ficha.texto(it, 14f) }
        ficha.texto(reserva.fechaYHorario(), 14f)
        importeReserva(reserva.costoTotal)?.let { ficha.texto("Total cotizado · $it") }
        importeReserva(reserva.saldoPendiente)?.let { ficha.texto("Saldo pendiente · $it") }

        reserva.textoLlegadaPropietario()?.let { texto ->
            contenido.seccion("Acceso al evento").mostrarLlegadaRegistrada(texto)
        }

        val servicios = contenido.seccion("Servicios")
        if (reserva.servicios.isEmpty()) servicios.texto("Sin servicios registrados.", 14f)
        reserva.servicios.forEach {
            val resumen = listOfNotNull(
                "${it.nombre ?: "Servicio #${it.id}"} · ${it.cantidad ?: 1}",
                importeReserva(it.precio)?.let { precio -> "$precio por unidad" },
                importeReserva(it.subtotal)
            ).joinToString(" · ")
            servicios.texto(resumen, 14f)
        }
        val inventario = contenido.seccion("Mobiliario asignado")
        if (reserva.inventario.isEmpty()) inventario.texto("Sin mobiliario asignado.", 14f)
        reserva.inventario.forEach {
            inventario.texto("${it.nombre ?: "Recurso #${it.id}"} · ${it.cantidad ?: "—"}", 14f)
        }
        val vista = NestedScrollView(this).apply {
            setBackgroundColor(getColor(R.color.reserva_fondo))
            addView(contenido)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Evento #${reserva.id}")
            .setView(vista)
            .setPositiveButton("Cerrar", null)
            .show()
    }

    override fun onResume() {
        super.onResume()

        if (!isFinishing) {
            vm.cargar()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putLong(EXTRA_RESERVA_CON_LLEGADA, reservaPorAbrir)
        super.onSaveInstanceState(outState)
    }

    private fun seleccionarMes() {
        val actual = vm.state.value.mes

        val panel = columna()

        panel.texto("Mes")

        val mes = NumberPicker(this).apply {
            minValue = 1
            maxValue = 12
            value = actual.monthValue
        }

        panel.addView(mes)
        panel.texto("Año")

        val years = vm.state.value.reservas.mapNotNull {
            ReglasPropietario.fecha(it.fechaInicio)?.year
        }

        val anio = NumberPicker(this).apply {
            minValue = minOf(
                actual.year - 10,
                years.minOrNull() ?: actual.year
            )

            maxValue = maxOf(
                actual.year + 10,
                years.maxOrNull() ?: actual.year
            )

            value = actual.year
        }

        panel.addView(anio)

        MaterialAlertDialogBuilder(this)
            .setTitle("Filtrar eventos")
            .setView(panel)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Aplicar") { _, _ ->
                mes.clearFocus()
                anio.clearFocus()

                vm.mes(
                    YearMonth.of(
                        anio.value,
                        mes.value
                    )
                )
            }
            .show()
    }

    companion object {
        internal const val EXTRA_RESERVA_CON_LLEGADA = "reservaConLlegada"
    }
}

internal val formatoLlegadaPropietario =
    DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-MX"))

internal fun Reservacion.textoLlegadaPropietario(): String? {
    if (checkInRealizado != true) return null
    val fecha = try {
        fechaCheckIn?.let { LocalDateTime.parse(it).format(formatoLlegadaPropietario) }
    } catch (_: DateTimeParseException) {
        null
    }
    return if (fecha == null) "Llegada registrada" else "Llegada registrada · $fecha"
}

private fun LinearLayout.mostrarLlegadaRegistrada(llegada: String) {
    texto(llegada, 14f).apply {
        setTypeface(typeface, Typeface.BOLD)
        setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = context.dp(6)
            bottomMargin = context.dp(6)
        }
        background = GradientDrawable().apply {
            cornerRadius = context.dp(12).toFloat()
            setColor(context.getColor(R.color.check_in_concedido))
        }
    }
}

internal fun PantallaPropietario.abrirPdfPropietario(archivo: java.io.File) {
    try {
        val uri = FileProvider.getUriForFile(
            this,
            "$packageName.reservas.pdf",
            archivo
        )

        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    uri,
                    "application/pdf"
                )

                clipData = ClipData.newRawUri(
                    "Cotización",
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            this,
            "Instala un lector de PDF para abrir el documento.",
            Toast.LENGTH_LONG
        ).show()
    } catch (_: Exception) {
        Toast.makeText(
            this,
            "No pudimos abrir el PDF.",
            Toast.LENGTH_LONG
        ).show()
    }
}

private class EventosPropietarioAdapter(
    private val abrir: (Reservacion) -> Unit,
    private val pdf: (Reservacion) -> Unit
) : RecyclerView.Adapter<EventosPropietarioAdapter.Fila>() {

    private var items = emptyList<Reservacion>()
    private var descargando: Long? = null
    private var cargando = false

    class Fila(
        card: MaterialCardView,
        val panel: LinearLayout
    ) : RecyclerView.ViewHolder(card)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Fila {

        val card = MaterialCardView(parent.context).apply {
            setCardBackgroundColor(Color.WHITE)
            radius = context.dp(18).toFloat()
            strokeWidth = context.dp(1)
            strokeColor = context.getColor(com.example.ocasion_mobil_v2.R.color.borde_input)

            layoutParams =
                RecyclerView.LayoutParams(-1, -2).apply {
                    bottomMargin = parent.context.dp(12)
                }
        }

        val panel = parent.context.columna()
        card.addView(panel)

        return Fila(card, panel)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: Fila,
        position: Int
    ) {
        val reserva = items[position]
        val panel = holder.panel

        panel.removeAllViews()

        val visual = EstadoVisualReserva.de(reserva.estado, reserva.saldoPendiente?.toBigDecimalOrNull()?.signum() == 1)
        panel.texto(visual?.texto ?: ReglasPropietario.etiqueta(reserva.estado), 13f).apply {
            setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = context.dp(6) }
            setPadding(context.dp(10), context.dp(5), context.dp(10), context.dp(5))
            setTextColor(visual?.tinta ?: context.getColor(R.color.texto_secundario))
            background = GradientDrawable().apply {
                cornerRadius = context.dp(12).toFloat()
                setColor(visual?.fondo ?: context.getColor(R.color.gris_claro))
            }
        }

        panel.texto(reserva.nombreSalon ?: "Salón #${reserva.idSalon ?: "—"}", 22f).apply {
            setTypeface(typeface, Typeface.BOLD)
        }
        panel.texto("Reserva #${reserva.id} · ${reserva.nombreCliente ?: "Cliente"}", 14f).apply {
            setTextColor(context.getColor(R.color.texto_secundario))
        }
        panel.texto(reserva.fechaYHorario(), 14f)
        reserva.textoLlegadaPropietario()?.let(panel::mostrarLlegadaRegistrada)

        importeReserva(reserva.costoTotal)?.let {
            val concepto = if (reserva.estado == "SOLICITUD_NUEVA") "Importe estimado" else "Total cotizado"
            panel.texto("$concepto · $it").apply { setTypeface(typeface, Typeface.BOLD) }
        }
        if (reserva.estado in setOf("ACEPTADA_CLIENTE", "ANTICIPO_PAGADO", "CONFIRMADA")) {
            importeReserva(reserva.saldoPendiente)?.let { panel.texto("Saldo pendiente · $it", 14f) }
        }

        val accion = when {
            reserva.estado == "ESPERANDO_PAGO" && ReglasPropietario.puedeCotizar(reserva) -> "Editar cotización"
            reserva.estado == "SOLICITUD_NUEVA" && ReglasPropietario.puedeCotizar(reserva) ->
                "Armar cotización"

            reserva.estado == "COTIZACION_PENDIENTE" ->
                "Abrir bandeja de aprobación"

            else ->
                "Ver detalle"
        }

        panel.boton(accion) {
            abrir(reserva)
        }.isEnabled = !cargando

        if (ReglasPropietario.puedePdf(reserva.estado)) {
            panel.boton(
                if (descargando == reserva.id) {
                    "Descargando…"
                } else {
                    "Ver cotización / contrato (PDF)"
                },
                secundario = true
            ) {
                pdf(reserva)
            }.isEnabled = !cargando && descargando == null
        }
    }

    fun actualizar(
        nuevos: List<Reservacion>,
        pdfId: Long?,
        cargando: Boolean
    ) {
        if (
            items == nuevos &&
            descargando == pdfId &&
            this.cargando == cargando
        ) return

        items = nuevos
        descargando = pdfId
        this.cargando = cargando

        notifyDataSetChanged()
    }
}
