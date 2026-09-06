package com.example.ocasion_mobil_v2.ui.cliente

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.salon.SalonData
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SalonAdapterClient (
    private var listaSalones: List<SalonData>,
    private val onSalonClick: (SalonData) -> Unit
) : RecyclerView.Adapter<SalonAdapterClient.SalonViewHolder>() {

    class SalonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivSalon: ImageView = view.findViewById(R.id.ivSalon)
        val tvCapacidad: TextView = view.findViewById(R.id.tvCapacidad)
        val tvNombre: TextView = view.findViewById(R.id.tvNombreRecinto)
        val tvCiudad: TextView = view.findViewById(R.id.tvCiudad)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecioHora)
        val tvServicio1: TextView = view.findViewById(R.id.tvServicio1)
        val tvServicio2: TextView = view.findViewById(R.id.tvServicio2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SalonViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.itemsalon_cliente, parent, false)
        return SalonViewHolder(view)
    }

    override fun onBindViewHolder(holder: SalonViewHolder, position: Int) {
        val salon = listaSalones[position]

        holder.tvNombre.text = salon.nombre
        holder.tvCapacidad.text = salon.capacidad.toString()
        holder.tvCiudad.text = "📍 ${salon.ciudad}"

        // Formato $#,##0.00 / hr
        val simbolos = DecimalFormatSymbols(Locale.US)
        val formatoMoneda = DecimalFormat("$#,##0.00", simbolos)
        holder.tvPrecio.text = "${formatoMoneda.format(salon.precioPorHora)} / hr"

        // Imagen con bordes redondeados usando Glide
        Glide.with(holder.itemView.context)
            .load(salon.imagenUrl)
            .transform(CenterCrop(), RoundedCorners(16))
            .into(holder.ivSalon)

        // Renderizar solo los 2 primeros servicios
        if (salon.servicios.isNotEmpty()) {
            holder.tvServicio1.text = salon.servicios[0]
            holder.tvServicio1.visibility = View.VISIBLE
        } else {
            holder.tvServicio1.visibility = View.GONE
        }

        if (salon.servicios.size >= 2) {
            holder.tvServicio2.text = salon.servicios[1]
            holder.tvServicio2.visibility = View.VISIBLE
        } else {
            holder.tvServicio2.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onSalonClick(salon) }
    }

    override fun getItemCount(): Int = listaSalones.size

    fun actualizarLista(nuevaLista: List<SalonData>) {
        listaSalones = nuevaLista
        notifyDataSetChanged()
    }
}