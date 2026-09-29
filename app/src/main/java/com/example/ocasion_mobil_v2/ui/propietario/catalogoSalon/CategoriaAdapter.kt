package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.google.android.material.button.MaterialButton

class CategoriaAdapter(

    private val onToggle: (Long) -> Unit,

    private val onAddResource:
        (CategoriaInventario) -> Unit,

    private val onEditCategory:
        (CategoriaInventario) -> Unit,

    private val onDeleteCategory:
        (CategoriaInventario) -> Unit,

    private val onEditResource:
        (CategoriaInventario, RecursoInventario) -> Unit,

    private val onDeleteResource:
        (CategoriaInventario, RecursoInventario) -> Unit

) : RecyclerView.Adapter<CategoriaAdapter.VH>() {

    private var items =
        emptyList<CategoriaInventario>()


    fun submitList(
        value: List<CategoriaInventario>
    ) {

        items = value

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_categoria_inventario,
                    parent,
                    false
                )
        )
    }


    override fun getItemCount() =
        items.size


    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }


    inner class VH(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        private val tvNombre: TextView =
            view.findViewById(
                R.id.tvNombreCategoria
            )

        private val tvContador: TextView =
            view.findViewById(
                R.id.tvContadorRecursos
            )

        private val btnExpand:
                MaterialButton =
            view.findViewById(
                R.id.btnExpandCategoria
            )

        private val btnEditar:
                MaterialButton =
            view.findViewById(
                R.id.btnEditarCategoria
            )

        private val btnEliminar:
                MaterialButton =
            view.findViewById(
                R.id.btnEliminarCategoria
            )

        private val btnAdd:
                MaterialButton =
            view.findViewById(
                R.id.btnAgregarInsumo
            )

        private val rv:
                RecyclerView =
            view.findViewById(
                R.id.rvRecursos
            )


        fun bind(
            item: CategoriaInventario
        ) {

            tvNombre.text =
                item.nombre


            tvContador.text =
                "${item.recursos.size}/20 recursos"


            btnExpand.text =
                if (item.expandida)
                    "▲"
                else
                    "▼"


            btnExpand.setOnClickListener {

                onToggle(
                    item.localId
                )
            }


            btnEditar.setOnClickListener {

                onEditCategory(item)
            }


            btnEliminar.setOnClickListener {

                onDeleteCategory(item)
            }


            btnAdd.isEnabled =
                item.recursos.size < 20


            btnAdd.text =
                if (
                    item.recursos.size >= 20
                )
                    "Máximo de 20 alcanzado"
                else
                    "＋ Agregar insumo"


            btnAdd.setOnClickListener {

                onAddResource(item)
            }


            rv.layoutManager =
                LinearLayoutManager(
                    view.context
                )


            rv.isNestedScrollingEnabled =
                false


            rv.adapter =
                RecursoAdapter(
                    onEditResource,
                    onDeleteResource
                ).also {

                    it.setCategoria(item)

                    it.submitList(
                        item.recursos
                    )
                }


            rv.visibility =
                if (item.expandida)
                    View.VISIBLE
                else
                    View.GONE
        }
    }
}


class RecursoAdapter(

    private val onEdit:
        (CategoriaInventario, RecursoInventario) -> Unit,

    private val onDelete:
        (CategoriaInventario, RecursoInventario) -> Unit

) : RecyclerView.Adapter<RecursoAdapter.VH>() {

    private var items =
        emptyList<RecursoInventario>()

    private var categoria:
            CategoriaInventario? = null


    fun submitList(
        value: List<RecursoInventario>
    ) {

        items = value

        notifyDataSetChanged()
    }


    fun setCategoria(
        value: CategoriaInventario
    ) {

        categoria = value
    }


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): VH {

        return VH(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_recurso_inventario,
                    parent,
                    false
                )
        )
    }


    override fun getItemCount() =
        items.size


    override fun onBindViewHolder(
        holder: VH,
        position: Int
    ) {

        categoria?.let {

            holder.bind(
                it,
                items[position]
            )
        }
    }


    inner class VH(
        view: View
    ) : RecyclerView.ViewHolder(view) {

        private val name: TextView =
            view.findViewById(
                R.id.tvNombreRecurso
            )

        private val qty: TextView =
            view.findViewById(
                R.id.tvCantidadRecurso
            )

        private val edit: TextView =
            view.findViewById(
                R.id.btnEditarRecurso
            )

        private val del: TextView =
            view.findViewById(
                R.id.btnEliminarRecurso
            )


        fun bind(
            categoria: CategoriaInventario,
            recurso: RecursoInventario
        ) {

            name.text =
                recurso.nombre

            qty.text =
                "${recurso.cantidadTotal} unidades"


            edit.setOnClickListener {

                onEdit(
                    categoria,
                    recurso
                )
            }


            del.setOnClickListener {

                onDelete(
                    categoria,
                    recurso
                )
            }
        }
    }
}