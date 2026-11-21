package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class PanelPropietarioActivity : PantallaPropietario() {

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

        val filtro = raiz.boton(
            "Seleccionar mes y año"
        ) {
            seleccionarMes()
        }.apply {
            isVisible = !nuevas
        }

        val actualizar = raiz.boton(
            "Actualizar / Reintentar"
        ) {
            vm.cargar()
        }

        val error = raiz.texto("")
        val aviso = raiz.texto("")

        val lista = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(
                this@PanelPropietarioActivity
            )
        }

        val adaptador = EventosPropietarioAdapter(
            abrir = { reserva ->
                when (reserva.estado) {
                    "SOLICITUD_NUEVA" -> {
                        startActivity(
                            Intent(
                                this,
                                CotizarPropietarioActivity::class.java
                            ).putExtra(
                                "idReservacion",
                                reserva.id
                            )
                        )
                    }

                    "COTIZACION_PENDIENTE" -> {
                        startActivity(
                            Intent(
                                this,
                                ReservacionesActivity::class.java
                            )
                        )
                    }

                    else -> {
                        MaterialAlertDialogBuilder(this)
                            .setTitle("Evento #${reserva.id}")
                            .setMessage(desglose(reserva))
                            .setPositiveButton("Cerrar", null)
                            .show()
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

                    filtro.text = estado.mes.format(
                        DateTimeFormatter.ofPattern(
                            "MMMM yyyy",
                            Locale.forLanguageTag("es-MX")
                        )
                    ) + " · Cambiar"

                    actualizar.isEnabled = !estado.cargando

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

                        estado.error != null ->
                            "No se pudo completar la consulta."

                        eventos.isEmpty() ->
                            if (nuevas) {
                                "No hay solicitudes nuevas."
                            } else {
                                "No hay eventos en este mes."
                            }

                        else ->
                            "${eventos.size} eventos"
                    }

                    adaptador.actualizar(
                        eventos,
                        estado.descargando,
                        estado.cargando
                    )

                    estado.documento?.let { archivo ->
                        vm.documentoAbierto()

                        try {
                            val uri = FileProvider.getUriForFile(
                                this@PanelPropietarioActivity,
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
                                this@PanelPropietarioActivity,
                                "Instala un lector de PDF para abrir el documento.",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (_: Exception) {
                            Toast.makeText(
                                this@PanelPropietarioActivity,
                                "No pudimos abrir el PDF.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!isFinishing) {
            vm.cargar()
        }
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
            radius = context.dp(12).toFloat()
            strokeWidth = context.dp(1)
            strokeColor = Color.LTGRAY

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

        panel.texto(
            reserva.nombreCliente
                ?: "Cliente #${reserva.idCliente ?: "sin identificar"}",
            20f
        )

        panel.texto(
            "${reserva.numeroInvitados ?: "—"} invitados" +
                    " · ${reserva.fechaInicio ?: "Sin fecha"}" +
                    " a ${reserva.fechaFin ?: "—"}"
        )

        panel.texto(
            reserva.nombreSalon
                ?: "Salón #${reserva.idSalon ?: "—"}"
        )

        val etiqueta =
            ReglasPropietario.etiqueta(reserva.estado)

        panel.texto(etiqueta).apply {
            setBackgroundColor(
                when (etiqueta) {
                    "Pendiente" ->
                        Color.rgb(225, 225, 225)

                    "Esperando pago" ->
                        Color.rgb(255, 233, 160)

                    "Confirmada" ->
                        Color.rgb(207, 235, 197)

                    "Rechazada" ->
                        Color.rgb(255, 211, 211)

                    else -> Color.LTGRAY
                }
            )

            setPadding(
                context.dp(12),
                context.dp(8),
                context.dp(12),
                context.dp(8)
            )
        }

        if (ReglasPropietario.aceptada(reserva.estado)) {
            panel.texto("✓ Aceptado por el cliente")
        }

        reserva.costoTotal?.let {
            panel.texto("Total cotizado: $it")
        }

        val accion = when (reserva.estado) {
            "SOLICITUD_NUEVA" ->
                "Armar cotización"

            "COTIZACION_PENDIENTE" ->
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
                }
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

internal fun desglose(reserva: Reservacion): String =
    buildString {
        appendLine(
            reserva.nombreCliente
                ?: "Cliente #${reserva.idCliente ?: "sin identificar"}"
        )

        appendLine(
            "${reserva.numeroInvitados ?: "—"} invitados" +
                    " · ${reserva.fechaInicio ?: "Sin fecha"}" +
                    " a ${reserva.fechaFin ?: "—"}"
        )

        appendLine()
        appendLine("Servicios:")

        if (reserva.servicios.isEmpty()) {
            appendLine("Sin servicios adicionales registrados.")
        } else {
            reserva.servicios.forEach {
                appendLine(
                    "${it.nombre ?: "Servicio #${it.id}"}" +
                            " × ${it.cantidad ?: "—"}"
                )
            }
        }

        appendLine()
        appendLine("Inventario retenido:")

        if (reserva.inventario.isEmpty()) {
            appendLine("Sin inventario retenido registrado.")
        } else {
            reserva.inventario.forEach {
                appendLine(
                    "${it.nombre ?: "Recurso #${it.id}"}" +
                            " × ${it.cantidad ?: "—"}"
                )
            }
        }
    }