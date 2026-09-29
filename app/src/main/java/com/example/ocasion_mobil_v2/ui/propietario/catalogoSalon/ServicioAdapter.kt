package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.google.android.material.chip.Chip

class ServicioAdapter(private val onClick: (ServicioSeleccionado) -> Unit) : RecyclerView.Adapter<ServicioAdapter.VH>() {
    private var items: List<ServicioSeleccionado> = emptyList()
    private var selectedIds: Set<Int> = emptySet()

    fun submitList(newItems: List<ServicioSeleccionado>, selected: List<ServicioSeleccionado>) {
        items = newItems
        selectedIds = selected.map { it.id }.toSet()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH = VH(LayoutInflater.from(parent.context).inflate(R.layout.item_servicio_chip, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], selectedIds.contains(items[position].id))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val chip: Chip = view.findViewById(R.id.chipServicio)
        fun bind(item: ServicioSeleccionado, selected: Boolean) {
            chip.text = if (item.personalizado) "★ ${item.nombre}" else item.nombre
            chip.isChecked = selected
            chip.setOnClickListener { onClick(item) }
        }
    }
}
