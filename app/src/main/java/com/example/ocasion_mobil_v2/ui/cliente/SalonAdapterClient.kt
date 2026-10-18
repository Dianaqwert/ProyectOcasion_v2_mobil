package com.example.ocasion_mobil_v2.ui.cliente

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.SalonCardClienteDTO
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class SalonAdapterClient(
    private val onSalonClick: (SalonCardClienteDTO) -> Unit
) : ListAdapter<SalonCardClienteDTO, SalonAdapterClient.SalonViewHolder>(DIFF) {
    private val currency = DecimalFormat("$#,##0.00", DecimalFormatSymbols(Locale.US))

    class SalonViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.ivSalon)
        val capacity: TextView = view.findViewById(R.id.tvCapacidad)
        val name: TextView = view.findViewById(R.id.tvNombreRecinto)
        val city: TextView = view.findViewById(R.id.tvCiudad)
        val price: TextView = view.findViewById(R.id.tvPrecioHora)
        val service1: TextView = view.findViewById(R.id.tvServicio1)
        val service2: TextView = view.findViewById(R.id.tvServicio2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = SalonViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.itemsalon_cliente, parent, false)
    )

    override fun onBindViewHolder(holder: SalonViewHolder, position: Int) {
        val salon = getItem(position)
        holder.name.text = salon.nombre ?: "Salón para eventos"
        holder.capacity.text = "${salon.capacidad ?: "—"} personas"
        holder.city.text = "📍 ${salon.ciudad ?: "Ubicación por confirmar"}"
        holder.price.text = salon.precioHora?.let { "${currency.format(it)} / hr" } ?: "Precio por consultar"
        Glide.with(holder.image)
            .load(salon.imagenPortada)
            .placeholder(R.drawable.bg_map_placeholder)
            .error(R.drawable.bg_map_placeholder)
            .transform(CenterCrop(), RoundedCorners(16))
            .into(holder.image)
        bindService(holder.service1, salon.servicios.orEmpty().getOrNull(0))
        bindService(holder.service2, salon.servicios.orEmpty().getOrNull(1))
        holder.itemView.setOnClickListener { onSalonClick(salon) }
    }

    private fun bindService(view: TextView, service: String?) {
        view.text = service.orEmpty()
        view.visibility = if (service.isNullOrBlank()) View.GONE else View.VISIBLE
    }

    fun actualizarLista(nuevaLista: List<SalonCardClienteDTO>) {
        submitList(nuevaLista)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<SalonCardClienteDTO>() {
            override fun areItemsTheSame(oldItem: SalonCardClienteDTO, newItem: SalonCardClienteDTO) =
                oldItem.idSalon == newItem.idSalon

            override fun areContentsTheSame(oldItem: SalonCardClienteDTO, newItem: SalonCardClienteDTO) =
                oldItem == newItem
        }
    }
}
