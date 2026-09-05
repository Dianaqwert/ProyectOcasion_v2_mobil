package com.example.ocasion_mobil_v2.ui.propietario

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Salon
import com.google.android.material.button.MaterialButton

class SalonAdapter(
    private val salones: MutableList<Salon>,

    private val onEditar: (Salon) -> Unit,
    private val onEliminar: (Salon) -> Unit,
    private val onDisponibilidad: (Salon) -> Unit

) : android.widget.BaseAdapter() {

    override fun getCount(): Int {
        return salones.size
    }

    override fun getItem(position: Int): Salon {
        return salones[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup
    ): View {

        val view = convertView
            ?: LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_salon,
                    parent,
                    false
                )

        val salon = getItem(position)

        val imgSalon =
            view.findViewById<ImageView>(
                R.id.imgSalon
            )

        val tvSalonName =
            view.findViewById<TextView>(
                R.id.tvSalonName
            )

        val tvSalonLocation =
            view.findViewById<TextView>(
                R.id.tvSalonLocation
            )

        val tvCapacity =
            view.findViewById<TextView>(
                R.id.tvCapacity
            )

        val tvPrice =
            view.findViewById<TextView>(
                R.id.tvPrice
            )

        val tvPriceBadge =
            view.findViewById<TextView>(
                R.id.tvPriceBadge
            )

        val tvDescription =
            view.findViewById<TextView>(
                R.id.tvDescription
            )

        val btnEdit =
            view.findViewById<MaterialButton>(
                R.id.btnEditSalon
            )

        val btnDelete =
            view.findViewById<MaterialButton>(
                R.id.btnDeleteSalon
            )

        val btnDisponibilidad =
            view.findViewById<MaterialButton>(
                R.id.btnDisponibilidadSalon
            )


        // ==========================================
        // INFORMACIÓN
        // ==========================================

        tvSalonName.text =
            salon.nombreSalon

        tvSalonLocation.text =
            "📍 ${salon.ciudad}"

        tvCapacity.text =
            "👥 ${salon.capacidadPersonas} personas"

        tvPrice.text =
            "$${salon.precioHora} / hora"

        tvPriceBadge.text =
            "$${salon.precioHora} / hora"

        tvDescription.text =
            salon.descripcion


        // ==========================================
        // IMAGEN
        // ==========================================

        if (salon.imagenes.isNotEmpty()) {

            try {

                imgSalon.setImageURI(
                    salon.imagenes.first().toUri()
                )

            } catch (e: Exception) {

                imgSalon.setImageResource(
                    R.drawable.ic_launcher_background
                )
            }

        } else {

            imgSalon.setImageResource(
                R.drawable.ic_launcher_background
            )
        }


        // ==========================================
        // EDITAR
        // ==========================================

        btnEdit.setOnClickListener {

            onEditar(salon)
        }


        // ==========================================
        // DAR DE BAJA
        // ==========================================

        btnDelete.setOnClickListener {

            onEliminar(salon)
        }


        // ==========================================
        // DISPONIBILIDAD
        // ==========================================

        btnDisponibilidad.setOnClickListener {

            onDisponibilidad(salon)
        }


        return view
    }
}