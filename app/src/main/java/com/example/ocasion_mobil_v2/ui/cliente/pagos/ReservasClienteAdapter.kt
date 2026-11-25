package com.example.ocasion_mobil_v2.ui.cliente.pagos

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.repository.PaseQr
import com.example.ocasion_mobil_v2.data.repository.ReglasPagoCliente
import com.google.android.material.button.MaterialButton
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal data class FichaHistorial(
    val id: Long,
    val reserva: Reservacion?,
    val pase: PaseQr?,
    val cargando: Boolean,
    val descargandoId: Long?
)

internal object HistorialCliente {
    fun ordenar(reservas: List<Reservacion>): List<Reservacion> =
        reservas.sortedWith(compareByDescending<Reservacion> {
            it.fechaInicio?.let { fecha -> runCatching { LocalDate.parse(fecha) }.getOrNull() }
        }.thenByDescending { it.horaInicio }.thenByDescending { it.id })
}

internal enum class EstadoVisualReserva(val texto: String, val fondo: Int, val tinta: Int) {
    REVISION("En revisión", 0xFFE9E8E4.toInt(), 0xFF494741.toInt()),
    COTIZADA("Cotización lista", 0xFFFFE9A0.toInt(), 0xFF573A0C.toInt()),
    POR_PAGAR("Esperando pago", 0xFFFFE9A0.toInt(), 0xFF573A0C.toInt()),
    ABONO("Anticipo pagado", 0xFFFFE9A0.toInt(), 0xFF573A0C.toInt()),
    CONFIRMADA("Confirmada", 0xFFE1F1DA.toInt(), 0xFF265B1E.toInt()),
    RECHAZADA("Rechazada", 0xFFFCE2E1.toInt(), 0xFF8B211C.toInt()),
    CANCELADA("Cancelada", 0xFFFCE2E1.toInt(), 0xFF8B211C.toInt());

    companion object {
        fun de(estado: String, saldoPendiente: Boolean): EstadoVisualReserva? = when (estado) {
            "SOLICITUD_NUEVA", "COTIZACION_PENDIENTE", "ACEPTADA_PROPIETARIO", "PENDIENTE" -> REVISION
            "ESPERANDO_PAGO" -> COTIZADA
            "ACEPTADA_CLIENTE" -> POR_PAGAR
            "ANTICIPO_PAGADO" -> ABONO
            "CONFIRMADA" -> if (saldoPendiente) POR_PAGAR else CONFIRMADA
            "RECHAZADO", "RECHAZADA", "RECHAZADA_PROPIETARIO" -> RECHAZADA
            "CANCELADA", "CANCELADA_CLIENTE" -> CANCELADA
            else -> null
        }
    }
}

internal class ReservasClienteAdapter(
    private val abrirDetalle: (Long) -> Unit,
    private val descargarPdf: (Long) -> Unit,
    private val abrirQr: (PaseQr) -> Unit
) : ListAdapter<FichaHistorial, ReservasClienteAdapter.Fila>(object : DiffUtil.ItemCallback<FichaHistorial>() {
    override fun areItemsTheSame(old: FichaHistorial, new: FichaHistorial) = old.id == new.id
    override fun areContentsTheSame(old: FichaHistorial, new: FichaHistorial) = old == new
}) {
    private val locale = Locale.forLanguageTag("es-MX")
    private val moneda = NumberFormat.getCurrencyInstance(locale)
    private val fechaFormato = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", locale)

    init { stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Fila =
        Fila(LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_reserva, parent, false))

    override fun onBindViewHolder(holder: Fila, position: Int) {
        val ficha = getItem(position)
        val reserva = ficha.reserva
        val pase = ficha.pase
        val saldo = reserva?.let(ReglasPagoCliente::saldo)
        val visual = if (reserva != null) EstadoVisualReserva.de(reserva.estado, (saldo?.signum() ?: 0) > 0)
        else EstadoVisualReserva.CONFIRMADA
        holder.estado.text = if (reserva == null) "Pase sin conexión"
            else visual?.texto ?: reserva.estado.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }
        holder.estado.backgroundTintList = ColorStateList.valueOf(visual?.fondo ?: Color.rgb(233, 232, 228))
        holder.estado.setTextColor(visual?.tinta ?: Color.rgb(73, 71, 65))
        holder.salon.text = reserva?.nombreSalon ?: pase?.salon ?: reserva?.idSalon?.let { "Salón #$it" } ?: "Salón por confirmar"
        holder.folio.text = "Reserva #${ficha.id}"

        val fechas = listOfNotNull(reserva?.fechaInicio ?: pase?.fecha, reserva?.fechaFin)
            .distinct().joinToString(" al ") { runCatching { LocalDate.parse(it).format(fechaFormato) }.getOrDefault(it) }
        val horario = listOfNotNull(reserva?.horaInicio?.take(5), reserva?.horaFin?.take(5)).joinToString(" a ")
        holder.fecha.text = listOf(fechas.ifBlank { "Fecha por definir" }, horario).filter { it.isNotBlank() }.joinToString(" · ")
        holder.invitados.mostrar(reserva?.numeroInvitados?.let { "$it invitados" })
        val concepto = if (visual == EstadoVisualReserva.REVISION) "Importe estimado" else "Total"
        holder.total.mostrar(reserva?.costoTotal?.toBigDecimalOrNull()?.let { "$concepto · ${moneda.format(it)} MXN" })
        holder.saldo.mostrar(saldo?.takeIf { reserva?.estado?.let(ReglasPagoCliente::permitePdf) == true }?.let {
            if (it.signum() == 0) "Sin saldo pendiente" else "Saldo pendiente · ${moneda.format(it)} MXN"
        })
        holder.saldo.backgroundTintList = ColorStateList.valueOf(
            if ((saldo?.signum() ?: 0) > 0) Color.rgb(255, 236, 179) else Color.rgb(231, 243, 226)
        )

        holder.detalle.isVisible = reserva != null
        holder.detalle.isEnabled = !ficha.cargando
        holder.detalle.setOnClickListener { abrirDetalle(ficha.id) }
        holder.pdf.isVisible = reserva?.estado?.let(ReglasPagoCliente::permitePdf) == true
        holder.pdf.isEnabled = !ficha.cargando && ficha.descargandoId == null
        holder.pdf.text = when {
            ficha.descargandoId == ficha.id -> "Descargando PDF…"
            visual == EstadoVisualReserva.CONFIRMADA -> "Ver contrato (PDF)"
            else -> "Ver cotización (PDF)"
        }
        holder.pdf.setOnClickListener { descargarPdf(ficha.id) }
        holder.qr.isVisible = pase != null
        holder.qr.text = "Ver pase QR"
        holder.qr.setOnClickListener { pase?.let(abrirQr) }
    }

    private fun TextView.mostrar(valor: String?) {
        text = valor
        isVisible = valor != null
    }

    class Fila(view: View) : RecyclerView.ViewHolder(view) {
        val estado: TextView = view.findViewById(R.id.dashboardEstado)
        val salon: TextView = view.findViewById(R.id.dashboardSalon)
        val folio: TextView = view.findViewById(R.id.dashboardFolio)
        val fecha: TextView = view.findViewById(R.id.dashboardFecha)
        val invitados: TextView = view.findViewById(R.id.dashboardInvitados)
        val total: TextView = view.findViewById(R.id.dashboardTotal)
        val saldo: TextView = view.findViewById(R.id.dashboardSaldo)
        val detalle: MaterialButton = view.findViewById(R.id.dashboardDetalle)
        val pdf: MaterialButton = view.findViewById(R.id.dashboardPdf)
        val qr: MaterialButton = view.findViewById(R.id.dashboardQr)
    }
}
