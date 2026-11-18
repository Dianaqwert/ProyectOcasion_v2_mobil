package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion

class ReservacionAdapter(private val abrir: (Reservacion) -> Unit) : ListAdapter<Reservacion, ReservacionAdapter.Holder>(Diff) {
    class Holder(val view: View) : RecyclerView.ViewHolder(view) {
        val salon: TextView = view.findViewById(R.id.tvReservaSalon)
        val cliente: TextView = view.findViewById(R.id.tvReservaCliente)
        val fecha: TextView = view.findViewById(R.id.tvReservaFecha)
        val referencia: TextView = view.findViewById(R.id.tvReservaReferencia)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_reservacion, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val r = getItem(position)
        holder.salon.text = r.nombreSalon ?: "Salón #${r.idSalon ?: "sin dato"}"
        holder.cliente.text = r.nombreCliente ?: "Cliente sin nombre disponible"
        holder.fecha.text = fechasReserva(r)
        holder.referencia.text = "Solicitud #${r.id} · Ver desglose"
        holder.view.setOnClickListener { abrir(r) }
    }
    private object Diff : DiffUtil.ItemCallback<Reservacion>() {
        override fun areItemsTheSame(oldItem: Reservacion, newItem: Reservacion) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Reservacion, newItem: Reservacion) = oldItem == newItem
    }
}

fun fechasReserva(r: Reservacion): String = listOfNotNull(r.fechaInicio, r.fechaFin).joinToString(" al ").ifEmpty { "Fecha no disponible" }
