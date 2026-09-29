package com.example.ocasion_mobil_v2.ui.propietario.catalogoSalon

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.ui.propietario.ConfirmacionSalonActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class CatalogoSalonActivity :
    ComponentActivity() {

    private val viewModel:
            CatalogoSalonViewModel by viewModels()


    private lateinit var chipGroup:
            ChipGroup

    private lateinit var rvCategorias:
            androidx.recyclerview.widget.RecyclerView

    private lateinit var tvServiciosCount:
            TextView

    private lateinit var tvCategoriasCount:
            TextView

    private lateinit var btnAgregarCategoria:
            MaterialButton

    private lateinit var btnServicioPersonalizado:
            MaterialButton

    private lateinit var btnGuardar:
            MaterialButton

    private lateinit var adapter:
            CategoriaAdapter


    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )


        setContentView(
            R.layout.activity_catalogo_salon
        )


        val idSalon =
            intent.getIntExtra(
                "idSalon",
                -1
            )


        if (idSalon <= 0) {

            Toast.makeText(
                this,
                "No se encontró el ID del salón.",
                Toast.LENGTH_LONG
            ).show()

            finish()

            return
        }


        viewModel.iniciar(
            idSalon
        )


        initViews()

        setupRecycler()

        setupButtons()

        observeState()
    }


    private fun initViews() {

        chipGroup =
            findViewById(
                R.id.chipGroupServicios
            )


        rvCategorias =
            findViewById(
                R.id.rvCategorias
            )


        tvServiciosCount =
            findViewById(
                R.id.tvServiciosCount
            )


        tvCategoriasCount =
            findViewById(
                R.id.tvCategoriasCount
            )


        btnAgregarCategoria =
            findViewById(
                R.id.btnAgregarCategoria
            )


        btnServicioPersonalizado =
            findViewById(
                R.id.btnServicioPersonalizado
            )


        btnGuardar =
            findViewById(
                R.id.btnGuardarCatalogo
            )


        findViewById<TextView>(
            R.id.btnBackCatalogo
        ).setOnClickListener {

            finish()
        }
    }


    private fun setupRecycler() {

        adapter =
            CategoriaAdapter(

                onToggle = {
                    viewModel
                        .cambiarExpansionCategoria(it)
                },

                onAddResource = {
                    mostrarBottomSheetRecurso(it)
                },

                onEditCategory = {
                    mostrarDialogEditarCategoria(it)
                },

                onDeleteCategory = {
                    confirmarEliminarCategoria(it)
                },

                onEditResource = { categoria, recurso ->
                    mostrarBottomSheetEditar(
                        categoria,
                        recurso
                    )
                },

                onDeleteResource = { categoria, recurso ->
                    confirmarEliminarRecurso(
                        categoria,
                        recurso
                    )
                }
            )


        rvCategorias.layoutManager =
            LinearLayoutManager(this)


        rvCategorias.isNestedScrollingEnabled =
            false


        rvCategorias.adapter =
            adapter
    }


    private fun setupButtons() {

        btnAgregarCategoria.setOnClickListener {

            mostrarDialogCategoria()
        }


        btnServicioPersonalizado.setOnClickListener {

            mostrarDialogServicioPersonalizado()
        }


        btnGuardar.setOnClickListener {

            viewModel.guardarCatalogo {

                val idSalon =
                    viewModel.state
                        .value
                        .idSalon
                        ?: return@guardarCatalogo


                startActivity(

                    Intent(
                        this,
                        ConfirmacionSalonActivity::class.java
                    ).apply {

                        putExtra(
                            "idSalon",
                            idSalon
                        )

                        putExtra(
                            "nombreSalon",
                            intent.getStringExtra(
                                "nombreSalon"
                            ) ?: "Tu salón"
                        )

                        putExtra(
                            "capacidadPersonas",
                            intent.getStringExtra(
                                "capacidadPersonas"
                            ) ?: ""
                        )

                        putExtra(
                            "precioHora",
                            intent.getStringExtra(
                                "precioHora"
                            ) ?: ""
                        )

                        putExtra(
                            "descripcion",
                            intent.getStringExtra(
                                "descripcion"
                            ) ?: ""
                        )

                        putExtra(
                            "ciudad",
                            intent.getStringExtra(
                                "ciudad"
                            ) ?: ""
                        )

                        putExtra(
                            "codigoPostal",
                            intent.getStringExtra(
                                "codigoPostal"
                            ) ?: ""
                        )

                        putExtra(
                            "calle",
                            intent.getStringExtra(
                                "calle"
                            ) ?: ""
                        )

                        putExtra(
                            "numero",
                            intent.getStringExtra(
                                "numero"
                            ) ?: ""
                        )

                        putExtra(
                            "fraccionamiento",
                            intent.getStringExtra(
                                "fraccionamiento"
                            ) ?: ""
                        )

                        putExtra(
                            "servicios",
                            viewModel.state.value
                                .serviciosSeleccionados
                                .size
                        )

                        putExtra(
                            "categorias",
                            viewModel.state.value
                                .categoriasInventario
                                .size
                        )

                        putExtra(
                            "recursos",
                            viewModel.state.value
                                .categoriasInventario
                                .sumOf {
                                    it.recursos.size
                                }
                        )
                    }
                )


                finish()
            }
        }
    }


    private fun observeState() {

        lifecycleScope.launch {

            viewModel.state.collect { state ->

                tvServiciosCount.text =
                    "${state.serviciosSeleccionados.size}/10 seleccionados"


                tvCategoriasCount.text =
                    "${state.categoriasInventario.size}/8 categorías"


                btnAgregarCategoria.isEnabled =
                    state.puedeAgregarCategoria &&
                            !state.guardando


                btnServicioPersonalizado.isEnabled =
                    state.puedeAgregarServicio &&
                            !state.guardando


                btnGuardar.isEnabled =
                    !state.guardando


                btnGuardar.text =
                    if (state.guardando)
                        "Guardando catálogo..."
                    else
                        "Guardar y continuar"


                adapter.submitList(
                    state.categoriasInventario
                )


                renderServicios(
                    state
                )


                state.mensajeError?.let {

                    Snackbar.make(
                        findViewById(
                            R.id.rootCatalogo
                        ),
                        it,
                        Snackbar.LENGTH_LONG
                    ).show()

                    viewModel.limpiarMensajes()
                }


                state.mensajeExito?.let {

                    Snackbar.make(
                        findViewById(
                            R.id.rootCatalogo
                        ),
                        it,
                        Snackbar.LENGTH_SHORT
                    ).show()

                    viewModel.limpiarMensajes()
                }
            }
        }
    }


    private fun renderServicios(
        state: CatalogoSalonState
    ) {

        chipGroup.removeAllViews()


        state.serviciosDisponibles.forEach {

                servicio ->


            val selected =
                state.serviciosSeleccionados
                    .any {
                        it.id ==
                                servicio.idServicio
                    }


            val chip =
                Chip(this).apply {

                    text =
                        if (selected)
                            "✓ ${servicio.nombreServicio}"
                        else
                            servicio.nombreServicio


                    isCheckable = true

                    isChecked =
                        selected


                    setOnClickListener {

                        viewModel.toggleServicio(

                            ServicioSeleccionado(

                                id =
                                    servicio.idServicio,

                                nombre =
                                    servicio.nombreServicio,

                                descripcion =
                                    servicio.descripcion
                                        .orEmpty(),

                                personalizado =
                                    servicio.idServicio > 6
                            )
                        )
                    }
                }


            chipGroup.addView(
                chip
            )
        }
    }


    private fun mostrarDialogCategoria() {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.dialog_categoria,
                    null
                )


        val input =
            view.findViewById<EditText>(
                R.id.etNombreCategoria
            )


        val dialog =
            MaterialAlertDialogBuilder(this)

                .setTitle(
                    "Nueva categoría"
                )

                .setMessage(
                    "Ejemplo: Mobiliario, Cristalería, Mantelería..."
                )

                .setView(view)

                .setNegativeButton(
                    "Cancelar",
                    null
                )

                .setPositiveButton(
                    "Crear",
                    null
                )

                .create()


        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog
                    .BUTTON_POSITIVE
            ).setOnClickListener {

                if (
                    viewModel.agregarCategoria(
                        input.text.toString()
                    )
                ) {

                    dialog.dismiss()
                }
            }
        }


        dialog.show()
    }


    private fun mostrarDialogEditarCategoria(
        categoria: CategoriaInventario
    ) {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.dialog_categoria,
                    null
                )


        val input =
            view.findViewById<EditText>(
                R.id.etNombreCategoria
            )


        input.setText(
            categoria.nombre
        )


        val dialog =
            MaterialAlertDialogBuilder(this)

                .setTitle(
                    "Editar categoría"
                )

                .setView(view)

                .setNegativeButton(
                    "Cancelar",
                    null
                )

                .setPositiveButton(
                    "Guardar",
                    null
                )

                .create()


        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog
                    .BUTTON_POSITIVE
            ).setOnClickListener {

                if (
                    viewModel.editarCategoriaLocal(
                        categoria.localId,
                        input.text.toString()
                    )
                ) {

                    dialog.dismiss()
                }
            }
        }


        dialog.show()
    }


    private fun confirmarEliminarCategoria(
        categoria: CategoriaInventario
    ) {

        if (
            categoria.recursos.isNotEmpty()
        ) {

            MaterialAlertDialogBuilder(this)

                .setTitle(
                    "No se puede eliminar todavía"
                )

                .setMessage(
                    "La categoría \"${categoria.nombre}\" " +
                            "contiene ${categoria.recursos.size} insumo(s). " +
                            "Vacía la categoría antes de eliminarla."
                )

                .setPositiveButton(
                    "Entendido",
                    null
                )

                .show()

            return
        }


        MaterialAlertDialogBuilder(this)

            .setTitle(
                "Eliminar categoría"
            )

            .setMessage(
                "¿Quieres eliminar \"${categoria.nombre}\"?"
            )

            .setNegativeButton(
                "Cancelar",
                null
            )

            .setPositiveButton(
                "Eliminar"
            ) { _, _ ->

                viewModel.eliminarCategoriaLocal(
                    categoria.localId
                )
            }

            .show()
    }


    private fun mostrarDialogServicioPersonalizado() {

        val view =
            LayoutInflater.from(this)
                .inflate(
                    R.layout.dialog_servicio_personalizado,
                    null
                )


        val nombre =
            view.findViewById<EditText>(
                R.id.etNombreServicio
            )


        val descripcion =
            view.findViewById<EditText>(
                R.id.etDescripcionServicio
            )


        val dialog =
            MaterialAlertDialogBuilder(this)

                .setTitle(
                    "Servicio personalizado"
                )

                .setMessage(
                    "Se creará en el catálogo y quedará seleccionado para este salón."
                )

                .setView(view)

                .setNegativeButton(
                    "Cancelar",
                    null
                )

                .setPositiveButton(
                    "Crear",
                    null
                )

                .create()


        dialog.setOnShowListener {

            dialog.getButton(
                android.app.AlertDialog
                    .BUTTON_POSITIVE
            ).setOnClickListener {

                if (
                    nombre.text
                        .toString()
                        .trim()
                        .isEmpty()
                ) {

                    nombre.error =
                        "Ingresa el nombre"

                    return@setOnClickListener
                }


                viewModel.agregarServicioPersonalizado(

                    nombre.text.toString(),

                    descripcion.text.toString()
                )


                dialog.dismiss()
            }
        }


        dialog.show()
    }


    private fun mostrarBottomSheetRecurso(
        categoria: CategoriaInventario
    ) {

        val dialog =
            BottomSheetDialog(this)


        val view =
            layoutInflater.inflate(
                R.layout.bottom_sheet_recurso,
                null
            )


        dialog.setContentView(
            view
        )


        val nombre =
            view.findViewById<EditText>(
                R.id.etNombreRecurso
            )


        val cantidad =
            view.findViewById<EditText>(
                R.id.etCantidadRecurso
            )


        view.findViewById<MaterialButton>(
            R.id.btnGuardarRecurso
        ).setOnClickListener {

            val n =
                nombre.text
                    .toString()
                    .trim()


            val q =
                cantidad.text
                    .toString()
                    .trim()
                    .toIntOrNull()


            if (n.isBlank()) {

                nombre.error =
                    "Ingresa el nombre"

                return@setOnClickListener
            }


            if (
                q == null ||
                q !in 1..10_000
            ) {

                cantidad.error =
                    "Debe estar entre 1 y 10,000"

                return@setOnClickListener
            }


            if (
                viewModel.agregarRecurso(
                    categoria.localId,
                    n,
                    q
                )
            ) {

                dialog.dismiss()
            }
        }


        dialog.show()
    }


    private fun mostrarBottomSheetEditar(
        categoria: CategoriaInventario,
        recurso: RecursoInventario
    ) {

        val dialog =
            BottomSheetDialog(this)


        val view =
            layoutInflater.inflate(
                R.layout.bottom_sheet_recurso,
                null
            )


        dialog.setContentView(
            view
        )


        view.findViewById<TextView>(
            R.id.tvTituloBottomSheet
        ).text =
            "Editar insumo"


        val nombre =
            view.findViewById<EditText>(
                R.id.etNombreRecurso
            )


        val cantidad =
            view.findViewById<EditText>(
                R.id.etCantidadRecurso
            )


        nombre.setText(
            recurso.nombre
        )


        cantidad.setText(
            recurso.cantidadTotal.toString()
        )


        view.findViewById<MaterialButton>(
            R.id.btnGuardarRecurso
        ).apply {

            text =
                "Guardar cambios"


            setOnClickListener {

                val n =
                    nombre.text
                        .toString()
                        .trim()


                val q =
                    cantidad.text
                        .toString()
                        .trim()
                        .toIntOrNull()


                if (n.isBlank()) {

                    nombre.error =
                        "Ingresa el nombre"

                    return@setOnClickListener
                }


                if (
                    q == null ||
                    q !in 1..10_000
                ) {

                    cantidad.error =
                        "Debe estar entre 1 y 10,000"

                    return@setOnClickListener
                }


                if (
                    viewModel.editarRecursoLocal(
                        categoria.localId,
                        recurso.localId,
                        n,
                        q
                    )
                ) {

                    dialog.dismiss()
                }
            }
        }


        dialog.show()
    }


    private fun confirmarEliminarRecurso(
        categoria: CategoriaInventario,
        recurso: RecursoInventario
    ) {

        MaterialAlertDialogBuilder(this)

            .setTitle(
                "Eliminar insumo"
            )

            .setMessage(
                "¿Eliminar \"${recurso.nombre}\" del inventario?"
            )

            .setNegativeButton(
                "Cancelar",
                null
            )

            .setPositiveButton(
                "Eliminar"
            ) { _, _ ->

                viewModel.eliminarRecursoLocal(
                    categoria.localId,
                    recurso.localId
                )
            }

            .show()
    }
}