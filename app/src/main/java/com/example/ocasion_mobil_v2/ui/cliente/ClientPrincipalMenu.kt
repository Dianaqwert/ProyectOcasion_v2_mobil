package com.example.ocasion_mobil_v2.ui.cliente

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.SalonCardClienteDTO
import kotlinx.coroutines.launch

class ClientPrincipalMenu : FragmentActivity() {
    private val viewModel: ExplorarSalonesViewModel by viewModels()
    private lateinit var adapter: SalonAdapterClient
    private lateinit var emptyState: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var emptyMessage: TextView
    private lateinit var retryButton: com.google.android.material.button.MaterialButton
    private lateinit var recycler: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cliente_menu_principal)

        recycler = findViewById(R.id.rvSalonesHorizontales)
        emptyState = findViewById(R.id.emptyStateSalones)
        progress = findViewById(R.id.progressSalones)
        emptyMessage = findViewById(R.id.tvEmptySalones)
        retryButton = findViewById(R.id.btnReintentarSalones)

        adapter = SalonAdapterClient(::abrirDetalle)
        recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recycler.setHasFixedSize(false)
        recycler.itemAnimator = null
        recycler.adapter = adapter
        recycler.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val manager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                if (dx > 0 && viewModel.state.value.error == null && manager.findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.cargarSiguientePagina()
                }
            }
        })

        findViewById<SearchView>(R.id.searchView).setQuery(viewModel.state.value.consulta, false)
        findViewById<SearchView>(R.id.searchView).setOnQueryTextListener(
            object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?) = false
                override fun onQueryTextChange(newText: String?): Boolean {
                    viewModel.cambiarConsulta(newText.orEmpty())
                    return true
                }
            }
        )

        findViewById<ImageButton>(R.id.btnFiltros).setOnClickListener { abrirFiltros() }
        retryButton.setOnClickListener { viewModel.reintentar() }
        findViewById<View>(R.id.btnActualizarSalones).setOnClickListener {
            viewModel.aplicarFiltros(viewModel.state.value.filtros)
        }
        findViewById<View>(R.id.btnCerrarSesionCliente).setOnClickListener {
            com.example.ocasion_mobil_v2.data.session.SessionManager(this).cerrarSesion()
            startActivity(Intent(this, com.example.ocasion_mobil_v2.ui.login.LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        findViewById<View>(R.id.btnMasSalones).setOnClickListener { viewModel.reintentar() }
        findViewById<View>(R.id.btnLimpiarBusqueda).setOnClickListener {
            findViewById<SearchView>(R.id.searchView).setQuery("", false)
            viewModel.limpiarFiltros()
        }

        supportFragmentManager.setFragmentResultListener(
            FiltrosSalonesBottomSheet.RESULT_KEY, this
        ) { _, bundle ->
            if (bundle.getBoolean("limpiar")) {
                viewModel.limpiarFiltros()
            } else {
                viewModel.aplicarFiltros(
                    SalonFilters(
                        capacidadMin = bundle.getInt("capacidadMin", 50),
                        capacidadMax = bundle.getInt("capacidadMax", 1500),
                        precioMax = bundle.getFloat("precioMax", 10000f).toDouble(),
                        ciudad = bundle.getString("ciudad").orEmpty(),
                        serviciosIds = bundle.getIntegerArrayList("servicios")?.toSet().orEmpty()
                    )
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect(::renderState)
            }
        }
    }

    private fun abrirFiltros() {
        val filtros = viewModel.state.value.filtros
        FiltrosSalonesBottomSheet().apply {
            arguments = Bundle().apply {
                putInt("capacidadMin", filtros.capacidadMin)
                putInt("capacidadMax", filtros.capacidadMax)
                putFloat("precioMax", filtros.precioMax.toFloat())
                putString("ciudad", filtros.ciudad)
                putIntegerArrayList("servicios", ArrayList(filtros.serviciosIds))
            }
        }.show(supportFragmentManager, "filtros_salones")
    }

    private fun renderState(state: ExploreSalonesState) {
        val salones = state.salones
        adapter.actualizarLista(salones)
        recycler.visibility = if (salones.isEmpty()) View.INVISIBLE else View.VISIBLE
        progress.visibility = if (state.cargando) View.VISIBLE else View.GONE
        val mostrarVacio = !state.cargando && salones.isEmpty()
        emptyState.visibility = if (mostrarVacio) View.VISIBLE else View.GONE
        emptyMessage.text = state.error ?:
            "No encontramos salones con los criterios seleccionados. Prueba ajustando los filtros de precio o capacidad."
        retryButton.visibility = if (state.error != null) View.VISIBLE else View.GONE
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnMasSalones).apply {
            visibility = if (salones.isNotEmpty() && state.hayMas) View.VISIBLE else View.GONE
            isEnabled = !state.cargando
            text = if (state.error != null) "Reintentar más resultados" else "Cargar más salones"
        }
        findViewById<TextView>(R.id.tvEstadoBusqueda).text = if (state.cargando) "Buscando salones…"
            else if (salones.isNotEmpty() && state.error != null) state.error
            else "${state.total} salones encontrados"
        findViewById<View>(R.id.btnLimpiarBusqueda).visibility =
            if (!state.filtros.isDefault() || state.consulta.isNotBlank()) View.VISIBLE else View.GONE
    }

    private fun abrirDetalle(salon: SalonCardClienteDTO) {
        startActivity(Intent(this, DetalleSalonActivity::class.java).apply {
            putExtra(DetalleSalonActivity.EXTRA_ID_SALON, salon.idSalon)
            putExtra(DetalleSalonActivity.EXTRA_NOMBRE, salon.nombre)
            putExtra(DetalleSalonActivity.EXTRA_IMAGEN, salon.imagenPortada)
            putExtra(DetalleSalonActivity.EXTRA_PRECIO, salon.precioHora ?: -1.0)
            putExtra(DetalleSalonActivity.EXTRA_CAPACIDAD, salon.capacidad ?: -1)
            putExtra(DetalleSalonActivity.EXTRA_CIUDAD, salon.ciudad)
        })
    }
}
